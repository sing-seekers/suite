package suite.funp.p4;

import static primal.statics.Fail.fail;

import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
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

public class P4Alloc extends FunpCfg {

	private Amd64 amd64 = Amd64.me;
	private int is = integerSize;
	private int ps = pointerSize;
	private OpReg[] regsPs = amd64.regs(ps);
	private OpReg _ax = regsPs[amd64.axReg];
	private OpReg _bx = regsPs[amd64.bxReg];
	private OpReg _cx = regsPs[amd64.cxReg];
	private OpReg _dx = regsPs[amd64.dxReg];

	private OpImm countPointer;
	private OpImm labelPointer;
	private OpImm xorPointer;
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
		countPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, is))).in; // how many used blocks out there
		labelPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, ps))).in; // start point of remaining free area
		xorPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, ps))).in; // allocation checker

		freeChainTablePointer = em.spawn(em1 -> em1.emit( //
				Insn.DS, //
				amd64.imm32(allocSizes.length * ps), //
				amd64.imm8(0l))).in;

		em.mov(amd64.mem(labelPointer, ps), bufferStart);

		var regPointer = _ax; // allocated pointer; return value
		var opOffset = _bx; // offset to the table of free chains
		var opSize = _cx; // size we want to allocate

		var allocVsAdjust = em.spawn(em1 -> {
			em1.emit(Insn.INC, amd64.mem(countPointer, is));
			em1.emit(Insn.XOR, amd64.mem(xorPointer, ps), regPointer);
			em1.mov(amd64.mem(regPointer, 0, ps), opOffset);
			em1.addImm(regPointer, ps);
			em1.emit(Insn.RET);
		}).in;

		var allocNew = em.spawn(em.label(), em1 -> {
			var pointer = amd64.mem(labelPointer, ps);
			em1.mov(regPointer, pointer);
			em1.emit(Insn.ADD, pointer, opSize);
		}, allocVsAdjust).in;

		var allocSize = em.spawn(em.label(), em1 -> {
			var rf = _dx;
			em1.mov(rf, freeChainTablePointer);
			em1.emit(Insn.ADD, rf, opOffset);
			var fcp = amd64.mem(rf, 0, ps);

			em1.mov(regPointer, fcp);
			em1.emit(Insn.OR, regPointer, regPointer);
			em1.emit(Insn.JZ, allocNew);

			// reuse from buffer pool
			var rt = _dx;
			em1.mov(rt, amd64.mem(regPointer, 0, ps));
			em1.mov(fcp, rt);
		}, allocVsAdjust).in;

		allocVsRoutine = em.spawn(em1 -> {
			var size = _ax;
			em1.addImm(size, ps);

			for (var i = 0; i < allocSizes.length; i++) {
				em1.mov(opOffset, amd64.imm(i * ps, ps));
				em1.mov(opSize, amd64.imm(allocSizes[i], ps));
				em1.emit(Insn.CMP, size, opSize);
				em1.emit(Insn.JLE, allocSize);
			}

			em1.emit(Insn.HLT, amd64.remark("ALLOC TOO LARGE"));
		}).in;
	}

	public void deinit(Emit em) {
		em.emit(Insn.CMP, amd64.mem(countPointer, is), amd64.imm(0l, is));
		em.emit(Insn.JNZ, em.spawn(em1 -> em1.emit(Insn.HLT, amd64.remark("ALLOC MISMATCH"))).in);
		em.emit(Insn.CMP, amd64.mem(xorPointer, ps), amd64.imm(0l, ps));
		em.emit(Insn.JNZ, em.spawn(em1 -> em1.emit(Insn.HLT, amd64.remark("ALLOC POINTER MISMATCH"))).in);
	}

	// allocate with a fixed size, but allow de-allocation without specifying size
	// i.e. save the size information (free chain table index) into the allocated
	// block
	public CompileOut allocVs(Compile0 c0, int size) {
		return allocVs_(alloc_(c0, size + ps));
	}

	public CompileOut allocVs(Compile0 c0, OpReg size) {
		var ra = c0.rs.get(ps);
		if (ra != _ax)
			c0.em.emit(Insn.PUSH, _ax);
		c0.em.mov(_ax, size);
		c0.em.emit(Insn.PUSH, _bx);
		c0.em.emit(Insn.PUSH, _cx);
		c0.em.emit(Insn.PUSH, _dx);
		c0.em.emit(Insn.CALL, allocVsRoutine);
		c0.em.emit(Insn.POP, _dx);
		c0.em.emit(Insn.POP, _cx);
		c0.em.emit(Insn.POP, _bx);
		c0.em.mov(ra, _ax);
		if (ra != _ax)
			c0.em.emit(Insn.POP, _ax);
		return c0.returnOp(ra);
	}

	public void deallocVs(Compile0 c0, Funp reference) {
		var regPointer = c0.compilePsReg(reference);
		c0.em.addImm(regPointer, -ps);
		dealloc_(c0, regPointer, amd64.mem(regPointer, 0, ps));
	}

	public CompileOut alloc(Compile0 c0, int size) {
		return alloc_(c0, size).map((c1, r, index) -> c1.returnOp(r));
	}

	public void dealloc(Compile0 c0, int size, Funp reference) {
		var pair = getAllocSize(size);
		var regPointer = c0.compilePsReg(reference);
		dealloc_(c0, regPointer, amd64.imm(pair.t0 * ps, ps));
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

	private Fixie3<Compile0, OpReg, Operand> alloc_(Compile0 c0, Operand opOffset, Operand opSize) {
		var rf = c0.em.mov(c0.rs.get(ps), freeChainTablePointer);
		c0.em.emit(Insn.ADD, rf, opOffset);
		var fcp = amd64.mem(rf, 0, ps);

		var c1 = c0.mask(fcp);
		var regPointer = c0.isOutSpec ? c0.pop0 : c1.rs.get(ps);
		var labelEnd = c1.em.label();

		c1.em.mov(regPointer, fcp);
		c1.em.emit(Insn.OR, regPointer, regPointer);
		c1.em.emit(Insn.JZ, c1.spawn(c2 -> {
			var pointer = amd64.mem(labelPointer, ps);
			c2.em.mov(regPointer, pointer);
			c2.em.emit(Insn.ADD, pointer, opSize);
		}, labelEnd));

		c1.mask(regPointer).mov(fcp, amd64.mem(regPointer, 0, ps));
		c1.em.label(labelEnd);
		c1.em.emit(Insn.INC, amd64.mem(countPointer, is));
		c1.em.emit(Insn.XOR, amd64.mem(xorPointer, ps), regPointer);

		return Fixie.of(c1, regPointer, opOffset);
	}

	private void dealloc_(Compile0 c0, OpReg regPointer, Operand opOffset) {
		c0.em.emit(Insn.XOR, amd64.mem(xorPointer, ps), regPointer);
		c0.em.emit(Insn.DEC, amd64.mem(countPointer, is));

		var c1 = c0.mask(regPointer);
		var rf = c1.em.mov(c1.rs.get(ps), freeChainTablePointer);
		c1.em.emit(Insn.ADD, rf, opOffset);
		var fcp = amd64.mem(rf, 0, ps);

		var c2 = c1.mask(fcp);
		c2.mov(amd64.mem(regPointer, 0, ps), fcp);
		c2.mov(fcp, regPointer);
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
