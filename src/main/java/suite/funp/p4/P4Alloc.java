package suite.funp.p4;

import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
import primal.adt.Mutable;
import primal.primitive.adt.pair.IntIntPair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.FunpCfg;
import suite.funp.Funp_.Funp;
import suite.funp.p4.P4Emit.Emit;
import suite.funp.p4.P4GenerateCode.Compile0;
import suite.funp.p4.P4GenerateCode.CompileOut;

import static primal.statics.Fail.fail;

public class P4Alloc extends FunpCfg {

	private Amd64 amd64 = Amd64.me;
	private int is = integerSize;
	private int ps = pointerSize;

	private OpImm countPointer;
	private OpImm labelPointer;
	private OpImm freeChainTablePointer;
	private OpImm allocVsRoutine;

	private int[] allocSizes = { //
			4, //
			8, 12, //
			16, 20, 24, //
			32, 40, 48, 56, //
			64, 80, 96, 112, //
			128, 160, 192, 224, //
			256, 320, 384, 448, //
			512, 640, 768, 896, //
			1024, 1280, 1536, 1792, //
			16777216, };

	public P4Alloc(FunpCfg f) {
		super(f);
	}

	public void init(Emit em, OpReg bufferStart) {
		countPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, is))).in;
		labelPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, is))).in;

		freeChainTablePointer = em.spawn(em1 -> em1.emit( //
				Insn.DS, //
				amd64.imm32(allocSizes.length * ps), //
				amd64.imm8(0l))).in;

		em.mov(amd64.mem(labelPointer, ps), bufferStart);

		var regsPs = amd64.regs(ps);
		var ra = regsPs[amd64.axReg];
		var regOffset = regsPs[amd64.bxReg];
		var regSize = regsPs[amd64.cxReg];

		var allocVsAdjust = em.spawn(em1 -> {
			em1.emit(Insn.INC, amd64.mem(countPointer, is));
			em1.mov(amd64.mem(ra, 0, ps), regOffset);
			em1.addImm(ra, ps);
			em1.emit(Insn.RET);
		}).in;

		var allocNew = em.spawn(em.label(), em1 -> {
			var pointer = amd64.mem(labelPointer, ps);
			em1.mov(ra, pointer);
			em1.emit(Insn.ADD, pointer, regSize);
		}, allocVsAdjust).in;

		var allocSize = em.spawn(em.label(), em1 -> {
			var rf = regsPs[amd64.dxReg];
			em1.mov(rf, freeChainTablePointer);
			em1.emit(Insn.ADD, rf, regOffset);
			var fcp = amd64.mem(rf, 0, ps);

			em1.mov(ra, fcp);
			em1.emit(Insn.OR, ra, ra);
			em1.emit(Insn.JZ, allocNew);

			// reuse from buffer pool
			var rt = regsPs[amd64.dxReg];
			em1.mov(rt, amd64.mem(ra, 0, ps));
			em1.mov(fcp, rt);
		}, allocVsAdjust).in;

		allocVsRoutine = em.spawn(em1 -> {
			var size = regsPs[amd64.axReg];
			em1.addImm(size, ps);

			for (var i = 0; i < allocSizes.length; i++) {
				em1.mov(regOffset, amd64.imm(i * ps, ps));
				em1.mov(regSize, amd64.imm(allocSizes[i], ps));
				em1.emit(Insn.CMP, size, regSize);
				em1.emit(Insn.JBE, allocSize);
			}

			em1.emit(Insn.HLT, amd64.remark("ALLOC TOO LARGE"));
		}).in;
	}

	public void deinit(Emit em) {
		em.emit(Insn.CMP, amd64.mem(countPointer, is), amd64.imm(0l, is));
		em.emit(Insn.JNZ, em.spawn(em1 -> em1.emit(Insn.HLT, amd64.remark("ALLOC MISMATCH"))).in);
	}

	// allocate with a fixed size, but allow de-allocation without specifying size
	// i.e. save the size information (free chain table index) into the allocated
	// block
	public CompileOut allocVs(Compile0 c0, int size) {
		return allocVs_(alloc_(c0, size + ps));
	}

	public CompileOut allocVs(Compile0 c0, OpReg size) {
		var regsPs = amd64.regs(ps);
		var ra = c0.rs.get(ps);
		var _ax = regsPs[amd64.axReg];
		if (ra != _ax)
			c0.em.emit(Insn.PUSH, _ax);
		c0.em.mov(_ax, size);
		c0.em.emit(Insn.PUSH, regsPs[amd64.bxReg]);
		c0.em.emit(Insn.PUSH, regsPs[amd64.cxReg]);
		c0.em.emit(Insn.PUSH, regsPs[amd64.dxReg]);
		c0.em.emit(Insn.CALL, allocVsRoutine);
		c0.em.emit(Insn.POP, regsPs[amd64.dxReg]);
		c0.em.emit(Insn.POP, regsPs[amd64.cxReg]);
		c0.em.emit(Insn.POP, regsPs[amd64.bxReg]);
		c0.em.mov(ra, _ax);
		if (ra != _ax)
			c0.em.emit(Insn.POP, _ax);
		return c0.returnOp(ra);
	}

	public CompileOut allocVs0(Compile0 c0, OpReg size) {
		var result = new Mutable<CompileOut>();
		var regOffset = c0.rs.get(ps);
		var regSize = c0.mask(regOffset).rs.get(ps);
		var labelEnd = c0.em.label();

		var alloc = c0 //
				.mask(regOffset, regSize) //
				.spawn(c1 -> result.set(allocVs_(alloc_(c1, regOffset, regSize))), labelEnd);

		c0.em.addImm(size, ps);

		for (var i = 0; i < allocSizes.length; i++) {
			c0.em.mov(regOffset, amd64.imm(i * ps, ps));
			c0.em.mov(regSize, amd64.imm(allocSizes[i], ps));
			c0.em.emit(Insn.CMP, size, regSize);
			c0.em.emit(Insn.JBE, alloc);
		}

		c0.em.emit(Insn.HLT, amd64.remark("ALLOC TOO LARGE"));
		c0.em.label(labelEnd);
		return result.value();
	}

	public void deallocVs(Compile0 c0, Funp reference) {
		var ref = c0.compilePsReg(reference);
		c0.em.addImm(ref, -ps);
		dealloc_(c0, ref, amd64.mem(ref, 0, ps));
	}

	public CompileOut alloc(Compile0 c0, int size) {
		return alloc_(c0, size).map((c1, r, index) -> c1.returnOp(r));
	}

	public void dealloc(Compile0 c0, int size, Funp reference) {
		var pair = getAllocSize(size);
		var ref = c0.compilePsReg(reference);
		dealloc_(c0, ref, amd64.imm(pair.t0 * ps, ps));
	}

	private CompileOut allocVs_(Fixie3<Compile0, OpReg, Operand> f) {
		return f.map((c1, r, index) -> {
			c1.em.mov(amd64.mem(r, 0, ps), index);
			c1.em.addImm(r, ps);
			return c1.returnOp(r);
		});
	}

	private Fixie3<Compile0, OpReg, Operand> alloc_(Compile0 c0, int size) {
		var pair = getAllocSize(size);
		return alloc_(c0, amd64.imm(pair.t0 * ps, ps), amd64.imm(pair.t1, ps));
	}

	private Fixie3<Compile0, OpReg, Operand> alloc_(Compile0 c0, Operand allocIndex, Operand allocSize) {
		var rf = c0.em.mov(c0.rs.get(ps), freeChainTablePointer);
		c0.em.emit(Insn.ADD, rf, allocIndex);
		var fcp = amd64.mem(rf, 0, ps);

		var c1 = c0.mask(fcp);
		var ra = c0.isOutSpec ? c0.pop0 : c1.rs.get(ps);
		var labelEnd = c1.em.label();

		c1.em.mov(ra, fcp);
		c1.em.emit(Insn.OR, ra, ra);
		c1.em.emit(Insn.JZ, c1.spawn(c2 -> {
			var pointer = amd64.mem(labelPointer, ps);
			c2.em.mov(ra, pointer);
			c2.em.emit(Insn.ADD, pointer, allocSize);
		}, labelEnd));

		c1.mask(ra).mov(fcp, amd64.mem(ra, 0, ps));
		c1.em.label(labelEnd);
		c1.em.emit(Insn.INC, amd64.mem(countPointer, is));

		return Fixie.of(c1, ra, allocIndex);
	}

	private void dealloc_(Compile0 c0, OpReg ref, Operand opOffset) {
		c0.em.emit(Insn.DEC, amd64.mem(countPointer, is));

		var c1 = c0.mask(ref);
		var rf = c1.em.mov(c1.rs.get(ps), freeChainTablePointer);
		c1.em.emit(Insn.ADD, rf, opOffset);
		var fcp = amd64.mem(rf, 0, ps);

		var c2 = c1.mask(fcp);
		c2.mov(amd64.mem(ref, 0, ps), fcp);
		c2.mov(fcp, ref);
	}

	private IntIntPair getAllocSize(int size0) {
		var size1 = Math.max(ps, size0);

		for (var i = 0; i < allocSizes.length; i++) {
			var allocSize = allocSizes[i];
			if (size1 <= allocSize)
				return IntIntPair.of(i, allocSize);
		}
		return fail();
	}

}
