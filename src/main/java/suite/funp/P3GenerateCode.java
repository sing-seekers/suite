package suite.funp;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assembler;
import suite.assembler.Amd64Parser;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpAsm;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpAssign;
import suite.funp.P1.FunpData;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpInvokeInt;
import suite.funp.P1.FunpInvokeInt2;
import suite.funp.P1.FunpInvokeIo;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpRoutine;
import suite.funp.P1.FunpRoutine2;
import suite.funp.P1.FunpRoutineIo;
import suite.funp.P1.FunpSaveRegisters;
import suite.funp.P1.FunpWhile;
import suite.funp.P3JumpIf.JumpIf;
import suite.node.Atom;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.Bytes;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.FunUtil2.Sink2;
import suite.util.Switch;

public class P3GenerateCode {

	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;

	private Amd64 amd64 = Amd64.me;
	private Amd64Assembler asm = new Amd64Assembler();

	private OpReg cl = amd64.cl;
	private OpReg eax = amd64.eax;
	private OpReg ecx = amd64.ecx;
	private OpReg edx = amd64.edx;
	private OpReg ebp = amd64.ebp;
	private OpReg esp = amd64.esp;
	private OpReg esi = amd64.esi;
	private OpReg edi = amd64.edi;
	private OpReg[] integerRegs = is == 4 ? amd64.reg32 : is == 8 ? amd64.reg64 : null;
	private RegisterSet registerSet = new RegisterSet().mask(ebp, esp);

	private Map<Object, Insn> insnByOp = Map.ofEntries( //
			entry(TermOp.BIGOR_, Insn.OR), //
			entry(TermOp.BIGAND, Insn.AND), //
			entry(TermOp.PLUS__, Insn.ADD), //
			entry(TermOp.MINUS_, Insn.SUB), //
			entry(TermOp.MULT__, Insn.IMUL), //
			entry(TreeUtil.AND, Insn.AND), //
			entry(TreeUtil.OR_, Insn.OR), //
			entry(TreeUtil.XOR, Insn.XOR));

	private Map<TermOp, Insn> setInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.SETE), //
			entry(TermOp.LE____, Insn.SETLE), //
			entry(TermOp.LT____, Insn.SETL), //
			entry(TermOp.NOTEQ_, Insn.SETNE));

	private Map<TermOp, Insn> setnInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.SETNE), //
			entry(TermOp.LE____, Insn.SETG), //
			entry(TermOp.LT____, Insn.SETGE), //
			entry(TermOp.NOTEQ_, Insn.SETE));

	private Map<Atom, Insn> shInsnByOp = Map.ofEntries( //
			entry(TreeUtil.SHL, Insn.SHL), //
			entry(TreeUtil.SHR, Insn.SHR));

	private P3DecomposeOperand deOp = new P3DecomposeOperand();

	public List<Instruction> compile0(Funp funp) {
		List<Instruction> instructions = new ArrayList<>();
		P3Emit emit = new P3Emit(instructions::add);
		new Compile0(CompileOut_.OPREG, emit).new Compile1(registerSet, 0).compile(funp);
		return instructions;
	}

	public Bytes compile1(int offset, List<Instruction> instructions, boolean dump) {
		return asm.assemble(offset, instructions, dump);
	}

	private class Compile0 {
		private P3Emit em;
		private CompileOut_ type;
		private boolean isOutSpec;
		private FunpMemory target; // only for CompileOutType.ASSIGN
		private OpReg pop0, pop1; // only for CompileOutType.OPSPEC, TWOOPSPEC

		private Compile0(CompileOut_ type, P3Emit emit) {
			this(type, emit, null, null, null);
		}

		private Compile0(CompileOut_ type, P3Emit emit, FunpMemory target, OpReg pop0, OpReg pop1) {
			this.em = emit;
			this.type = type;
			this.isOutSpec = type == CompileOut_.OPSPEC || type == CompileOut_.TWOOPSPEC;
			this.target = target;
			this.pop0 = pop0;
			this.pop1 = pop1;
		}

		private class Compile1 {
			private RegisterSet rs;
			private int fd;

			private Compile1(RegisterSet rs, int fd) {
				this.rs = rs;
				this.fd = fd;
			}

			// invariant: fd = ESP - EBP
			private CompileOut compile(Funp n) {
				Fun<Operand, CompileOut> postOp = op -> {
					Operand old = op;
					if (type == CompileOut_.ASSIGN)
						em.mov(amd64.mem(mask(op).compileOpReg(target.pointer), target.start, is), old);
					else if (type == CompileOut_.OP || type == CompileOut_.OPREG) {
						if (type == CompileOut_.OPREG && !(op instanceof OpReg))
							em.mov(op = rs.get(old.size), old);
						return new CompileOut(op);
					} else if (type == CompileOut_.OPSPEC)
						em.mov(pop0, old);
					else
						throw new RuntimeException();
					return new CompileOut();
				};

				Fun2<Operand, Operand, CompileOut> postTwoOp = (op0, op1) -> {
					Operand old0 = op0;
					Operand old1 = op1;
					if (type == CompileOut_.ASSIGN) {
						OpReg r = mask(op0, op1).compileOpReg(target.pointer);
						em.mov(amd64.mem(r, target.start, ps), old0);
						em.mov(amd64.mem(r, target.start + ps, ps), old1);
					} else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG) {
						if (type == CompileOut_.TWOOPREG && !(op0 instanceof OpReg))
							em.mov(op0 = rs.mask(op1).get(old0.size), old0);
						if (type == CompileOut_.TWOOPREG && !(op1 instanceof OpReg))
							em.mov(op1 = rs.mask(op0).get(old1.size), old1);
						return new CompileOut(op0, op1);
					} else if (type == CompileOut_.TWOOPSPEC) {
						OpReg r = rs.mask(old1, pop1).get(pop0);
						em.mov(r, old0);
						em.mov(pop1, old1);
						em.mov(pop0, r);
					} else
						throw new RuntimeException();
					return new CompileOut();
				};

				Fun<Sink2<Compile1, FunpMemory>, CompileOut> postAssign = assign -> {
					if (type == CompileOut_.ASSIGN) {
						assign.sink2(this, target);
						return new CompileOut();
					} else if (type == CompileOut_.OP || type == CompileOut_.OPREG || type == CompileOut_.OPSPEC) {
						OpReg op0 = isOutSpec ? pop0 : rs.get(is);
						em.emit(amd64.instruction(Insn.PUSH, eax));
						int fd1 = fd - is;
						assign.sink2(new Compile1(rs, fd1), frame(fd1, fd));
						em.mov(op0, amd64.mem(ebp, fd1, is));
						em.emit(amd64.instruction(Insn.POP, rs.mask(op0).get(is)));
						return postOp.apply(op0);
					} else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG || type == CompileOut_.TWOOPSPEC) {
						OpReg op0 = isOutSpec ? pop0 : rs.get(is);
						OpReg op1 = isOutSpec ? pop1 : rs.mask(op0).get(is);
						int size = ps * 2;
						int fd1 = fd - size;
						Operand imm = amd64.imm(size);
						em.emit(amd64.instruction(Insn.SUB, esp, imm));
						assign.sink2(new Compile1(rs, fd1), frame(fd1, fd));
						em.mov(op0, amd64.mem(ebp, fd1, ps));
						em.mov(op1, amd64.mem(ebp, fd1 + ps, ps));
						em.emit(amd64.instruction(Insn.ADD, esp, imm));
						return postTwoOp.apply(op0, op1);
					} else
						throw new RuntimeException();
				};

				Fun<Sink<Compile1>, CompileOut> postRoutine = routine -> compileRoutine(routine).map(postTwoOp);

				Source<CompileOut> postDontCare = () -> {
					if (type == CompileOut_.OP || type == CompileOut_.OPREG)
						return new CompileOut(eax);
					else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG)
						return new CompileOut(eax, edx);
					else
						return new CompileOut();
				};

				Fun<Operand, FixieFun4<Insn, Insn, Funp, Funp, Boolean>> cmpJmp = label -> (insn, nInsn, lhs, rhs) -> {
					Pair<Funp, OpReg> pair = compileCommutativeTree(Insn.CMP, Assoc.RIGHT, lhs, rhs);
					em.emit(amd64.instruction(pair.t0 == lhs ? insn : nInsn, label));
					return true;
				};

				return new Switch<CompileOut>(n //
				).applyIf(FunpAllocStack.class, f -> f.apply((size, value, expr, stack) -> {
					Operand imm = amd64.imm(size), op;
					int fd1 = fd - size;
					Compile1 c1 = new Compile1(rs, fd1);

					stack.update(fd1);

					if ((op = deOp.decomposeOperand(value)) != null && op.size == is)
						em.emit(amd64.instruction(Insn.PUSH, value != null ? op : eax));
					else {
						em.emit(amd64.instruction(Insn.SUB, esp, imm));
						c1.compileAssign(value, frame(fd1, fd));
					}
					CompileOut out = c1.compile(expr);
					if (size == is)
						em.emit(amd64.instruction(Insn.POP, rs.mask(out.op0, out.op1).get(size)));
					else
						em.emit(amd64.instruction(Insn.ADD, esp, imm));
					return out;
				})).applyIf(FunpAsm.class, f -> f.apply((assigns, asm) -> {
					Amd64Parser p = new Amd64Parser();

					new Object() {
						private void assign(Compile1 c1, int i) {
							if (i < assigns.size()) {
								Pair<OpReg, Funp> assign = assigns.get(i);
								OpReg op = assign.t0;
								c1.saveRegs(c2 -> {
									c2.compileOpSpec(assign.t1, op);
									assign(c2.mask(op), i + 1);
								}, op);
							}
						}
					}.assign(this, 0);

					Read.from(asm).map(p::parse).sink(em::emit);
					return postOp.apply(eax);
				})).applyIf(FunpAssign.class, f -> f.apply((memory, value, expr) -> {
					compileAssign(value, memory);
					return compile(expr);
				})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
					return postOp.apply(amd64.imm(b ? 1 : 0, Funp_.booleanSize));
				})).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
					OpReg r1 = pop1.reg < 4 ? pop1 : rs.get(1);
					OpReg r0 = integerRegs[r1.reg];
					compileOpSpec(expr, r0);
					return postOp.apply(r1);
				})).applyIf(FunpData.class, f -> f.apply(pairs -> {
					return postAssign.apply((c1, target) -> {
						for (Pair<Funp, IntIntPair> pair : pairs) {
							IntIntPair offset = pair.t1;
							FunpMemory target_ = FunpMemory.of(target.pointer, target.start + offset.t0, target.end + offset.t1);
							c1.compileAssign(pair.t0, target_);
						}
					});
				})).applyIf(FunpDontCare.class, f -> {
					return postDontCare.source();
				}).applyIf(FunpError.class, f -> {
					em.emit(amd64.instruction(Insn.HLT));
					return postDontCare.source();
				}).applyIf(FunpFixed.class, f -> f.apply((var, expr) -> {
					throw new RuntimeException();
				})).applyIf(FunpFramePointer.class, t -> {
					return postOp.apply(ebp);
				}).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
					Sink<Funp> compile0, compile1;
					Source<CompileOut> out;

					if (type == CompileOut_.ASSIGN || isOutSpec) {
						compile0 = compile1 = this::compile;
						out = CompileOut::new;
					} else if (type == CompileOut_.OP || type == CompileOut_.OPREG) {
						OpReg[] ops = new OpReg[1];
						compile0 = node_ -> {
							Operand op0 = compileOp(node_);
							em.mov(ops[0] = rs.get(op0), op0);
						};
						compile1 = node_ -> compileOpSpec(node_, ops[0]);
						out = () -> postOp.apply(ops[0]);
					} else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG) {
						OpReg[] ops = new OpReg[2];
						compile0 = node_ -> {
							CompileOut co1 = compileTwoOp(node_);
							em.mov(ops[0] = rs.mask(co1.op1).get(co1.op0), co1.op0);
							em.mov(ops[1] = rs.mask(ops[0]).get(co1.op1), co1.op1);
						};
						compile1 = node_ -> compileTwoOpSpec(node_, ops[0], ops[1]);
						out = () -> postTwoOp.apply(ops[0], ops[1]);
					} else
						throw new RuntimeException();

					Operand condLabel = em.label();
					Operand endLabel = em.label();

					Sink2<Funp, Funp> thenElse = (condt, condf) -> {
						compile0.sink(condt);
						em.emit(amd64.instruction(Insn.JMP, endLabel));
						em.emit(amd64.instruction(Insn.LABEL, condLabel));
						compile1.sink(condf);
						em.emit(amd64.instruction(Insn.LABEL, endLabel));
					};

					JumpIf jumpIf = new P3JumpIf(cmpJmp.apply(condLabel)).new JumpIf(if_);
					Source<Boolean> r;

					if ((r = jumpIf.jnxIf()) != null && r.source())
						thenElse.sink2(then, else_);
					else if ((r = jumpIf.jxxIf()) != null && r.source())
						thenElse.sink2(else_, then);
					else {
						OpReg r0 = compileOpReg(if_);
						em.emit(amd64.instruction(Insn.OR, r0, r0));
						em.emit(amd64.instruction(Insn.JZ, condLabel));
						thenElse.sink2(then, else_);
					}

					return out.source();
				})).applyIf(FunpInvokeInt.class, f -> f.apply(routine -> {
					if (!rs.contains(eax)) {
						compileInvoke(routine);
						return postOp.apply(eax);
					} else
						throw new RuntimeException();
				})).applyIf(FunpInvokeInt2.class, f -> f.apply(routine -> {
					if (!rs.contains(eax, edx)) {
						compileInvoke(routine);
						return postTwoOp.apply(eax, edx);
					} else
						throw new RuntimeException();
				})).applyIf(FunpInvokeIo.class, f -> f.apply(routine -> {
					return postAssign.apply((c1, target) -> {
						OpReg r0 = c1.compileOpReg(target.pointer);
						Compile1 c2 = c1.mask(r0);
						c2.compileInvoke(routine);
						c2.compileMove(r0, target.start, ebp, c2.fd, target.size());
					});
				})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
					int size = end - start;
					Operand op0, op1;
					if (type == CompileOut_.ASSIGN)
						return postAssign.apply((c1, target) -> {
							Operand op_ = deOp.decomposeOperand(target);
							if (size != target.size())
								throw new RuntimeException();
							else if (op_ != null)
								c1.compileInstruction(Insn.MOV, op_, n);
							else {
								OpReg r0 = c1.compileOpReg(target.pointer);
								OpReg r1 = c1.mask(r0).compileOpReg(pointer);
								c1.mask(r0, r1).compileMove(r0, target.start, r1, start, size);
							}
						});
					else if (type == CompileOut_.OP || type == CompileOut_.OPREG || type == CompileOut_.OPSPEC)
						if ((op0 = deOp.decomposeOpMem(pointer, start, size)) != null)
							return postOp.apply(op0);
						else
							return postOp.apply(amd64.mem(compileOpReg(pointer), start, size));
					else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG || type == CompileOut_.TWOOPSPEC)
						if ((op0 = deOp.decomposeOpMem(pointer, start, ps)) != null
								&& (op1 = deOp.decomposeOpMem(pointer, start + ps, ps)) != null)
							return postTwoOp.apply(op0, op1);
						else {
							OpReg r = compileOpReg(pointer);
							return postTwoOp.apply(amd64.mem(r, start, ps), amd64.mem(r, start + is, ps));
						}
					else
						throw new RuntimeException();
				})).applyIf(FunpNumber.class, f -> f.apply(i -> {
					return postOp.apply(amd64.imm(i.get(), is));
				})).applyIf(FunpRoutine.class, f -> f.apply(expr -> {
					return postRoutine.apply(c1 -> c1.compileOpSpec(expr, eax));
				})).applyIf(FunpRoutine2.class, f -> f.apply(expr -> {
					return postRoutine.apply(c1 -> c1.compileTwoOpSpec(expr, eax, edx));
				})).applyIf(FunpRoutineIo.class, f -> f.apply((expr, is, os) -> {
					FunpMemory out = frame(ps + is, os);
					return postRoutine.apply(c1 -> c1.compileAssign(expr, out));
				})).applyIf(FunpSaveRegisters.class, f -> f.apply(expr -> {
					OpReg[] opRegs = rs.list(r -> r != esp.reg);

					for (int i = 0; i <= opRegs.length - 1; i++)
						em.emit(amd64.instruction(Insn.PUSH, opRegs[i]));

					CompileOut out0 = new Compile1(registerSet, fd - opRegs.length * is).compile(expr);
					Operand op0 = isOutSpec ? pop0 : out0.op0;
					Operand op1 = isOutSpec ? pop1 : out0.op1;

					if (op0 != null)
						em.mov(op0 = rs.contains(op0) ? rs.mask(op1).get(op0.size) : op0, out0.op0);
					if (op1 != null)
						em.mov(op1 = rs.contains(op1) ? rs.mask(op0).get(op1.size) : op1, out0.op1);

					CompileOut out1 = new CompileOut(op0, op1);

					for (int i = opRegs.length - 1; 0 <= i; i--)
						em.emit(amd64.instruction(Insn.POP, opRegs[i]));

					return out1;
				})).applyIf(FunpTree.class, f -> f.apply((operator, lhs, rhs) -> {
					return postOp.apply(compileTree(n, operator, operator.getAssoc(), lhs, rhs));
				})).applyIf(FunpTree2.class, f -> f.apply((operator, lhs, rhs) -> {
					return postOp.apply(compileTree(n, operator, Assoc.RIGHT, lhs, rhs));
				})).applyIf(FunpWhile.class, f -> f.apply((while_, do_, expr) -> {
					Operand loopLabel = em.label();
					Operand contLabel = em.label();
					Operand exitLabel = em.label();

					em.emit(amd64.instruction(Insn.LABEL, loopLabel));
					Source<Boolean> r;

					if ((r = new P3JumpIf(cmpJmp.apply(exitLabel)).new JumpIf(while_).jnxIf()) != null && r.source())
						;
					else if ((r = new P3JumpIf(cmpJmp.apply(contLabel)).new JumpIf(while_).jxxIf()) != null && r.source()) {
						em.emit(amd64.instruction(Insn.JMP, exitLabel));
						em.emit(amd64.instruction(Insn.LABEL, contLabel));
					} else {
						OpReg r0 = compileOpReg(while_);
						em.emit(amd64.instruction(Insn.OR, r0, r0));
						em.emit(amd64.instruction(Insn.JZ, exitLabel));
					}

					compileOp(do_);
					em.emit(amd64.instruction(Insn.JMP, loopLabel));
					em.emit(amd64.instruction(Insn.LABEL, exitLabel));
					return compile(expr);
				})).nonNullResult();
			}

			private Operand compileTree(Funp n, Object operator, Assoc assoc, Funp lhs, Funp rhs) {
				Integer numRhs = rhs instanceof FunpNumber ? ((FunpNumber) rhs).i.get() : null;
				Insn insn = insnByOp.get(operator);
				Insn setInsn = setInsnByOp.get(operator);
				Insn setnInsn = setnInsnByOp.get(operator);
				Insn shInsn = shInsnByOp.get(operator);
				OpMem op = deOp.decomposeOpMem(n, 0, is);
				OpReg opResult = null;

				if (opResult == null && op != null)
					em.lea(opResult = isOutSpec ? pop0 : rs.get(ps), op);

				if (opResult == null && operator == TermOp.DIVIDE && numRhs != null && Integer.bitCount(numRhs) == 1)
					em.shiftImm(Insn.SHR, opResult = compileLoad(rhs), Integer.numberOfTrailingZeros(numRhs));

				if (opResult == null)
					if (operator == TermOp.DIVIDE) {
						OpReg opResult_ = isOutSpec ? pop0 : rs.get(eax);
						Sink<Compile1> sink0 = c1 -> {
							c1.compileOpSpec(lhs, eax);
							Operand opRhs0 = c1.mask(eax).compileOp(rhs);
							Operand opRhs1 = !(opRhs0 instanceof OpImm) ? opRhs0 : c1.rs.mask(eax, edx).get(is);
							em.mov(opRhs1, opRhs0);
							em.emit(amd64.instruction(Insn.XOR, edx, edx));
							em.emit(amd64.instruction(Insn.IDIV, opRhs1));
							em.mov(opResult_, eax);
						};
						Sink<Compile1> sink1 = rs.contains(eax) ? c1 -> c1.saveRegs(sink0, eax) : sink0;
						saveRegs(sink1, edx);
						opResult = opResult_;
					} else if (operator == TermOp.MINUS_) {
						Pair<Funp, OpReg> pair = compileCommutativeTree(Insn.SUB, assoc, lhs, rhs);
						if (pair.t1 == rhs)
							em.emit(amd64.instruction(Insn.NEG, pair.t1));
					} else if (setInsn != null) {
						Pair<Funp, OpReg> pair = compileCommutativeTree(Insn.CMP, assoc, lhs, rhs);
						em.emit(amd64.instruction(pair.t1 == lhs ? setInsn : setnInsn, opResult = isOutSpec ? pop0 : rs.get(1)));
					} else if (shInsn != null) {
						OpReg op0 = compileLoad(lhs);
						if (numRhs != null)
							em.emit(amd64.instruction(shInsn, op0, amd64.imm(numRhs, 1)));
						else
							saveRegs(c1 -> {
								Operand opRhs = c1.mask(op0).compileOpSpec(rhs, cl);
								em.emit(amd64.instruction(shInsn, op0, opRhs));
							}, ecx);
						opResult = op0;
					} else
						opResult = compileCommutativeTree(insn, assoc, lhs, rhs).t1;

				return opResult;
			}

			private Pair<Funp, OpReg> compileCommutativeTree(Insn insn, Assoc assoc, Funp lhs, Funp rhs) {
				Operand opLhs = deOp.decomposeOperand(lhs);
				Operand opRhs = deOp.decomposeOperand(rhs);
				OpReg opLhsReg = opLhs instanceof OpReg ? (OpReg) opLhs : null;
				OpReg opRhsReg = opRhs instanceof OpReg ? (OpReg) opRhs : null;
				boolean isLhsModifiable = opLhs != null && insn == Insn.CMP || opLhsReg != null && !rs.contains(opLhsReg);
				boolean isRhsModifiable = opRhs != null && insn == Insn.CMP || opRhsReg != null && !rs.contains(opRhsReg);

				if (isLhsModifiable)
					return Pair.of(lhs, compileRegInstruction(insn, opLhsReg, opRhs, lhs));
				else if (isRhsModifiable)
					return Pair.of(rhs, compileRegInstruction(insn, opRhsReg, opLhs, rhs));
				else if (opLhs != null && opRhs instanceof OpImm)
					return Pair.of(lhs, em.emitRegInsn(insn, compileLoad(lhs), opRhs));
				else if (opRhs != null && opLhs instanceof OpImm)
					return Pair.of(rhs, em.emitRegInsn(insn, compileLoad(rhs), opLhs));
				else if (opLhs != null)
					return Pair.of(rhs, em.emitRegInsn(insn, compileLoad(rhs), opLhs));
				else if (opRhs != null)
					return Pair.of(lhs, em.emitRegInsn(insn, compileLoad(lhs), opRhs));
				else {
					boolean isRightAssoc = assoc == Assoc.RIGHT;
					Funp first = isRightAssoc ? rhs : lhs;
					Funp second = isRightAssoc ? lhs : rhs;
					OpReg op0 = compileLoad(first);
					Operand op1 = mask(op0).compileOp(second);
					return Pair.of(first, em.emitRegInsn(insn, op0, op1));
				}
			}

			private Pair<Operand, Operand> compileRoutine(Sink<Compile1> sink) {
				Operand routineLabel = em.label();
				Operand endLabel = em.label();
				em.emit(amd64.instruction(Insn.JMP, endLabel));
				em.emit(amd64.instruction(Insn.LABEL, routineLabel));
				em.emit(amd64.instruction(Insn.PUSH, ebp));
				em.mov(ebp, esp);
				sink.sink(new Compile1(registerSet, 0));
				em.emit(amd64.instruction(Insn.POP, ebp));
				em.emit(amd64.instruction(Insn.RET));
				em.emit(amd64.instruction(Insn.LABEL, endLabel));
				return Pair.of(ebp, routineLabel);
			}

			private void compileInstruction(Insn insn, Operand op0, Funp f1) {
				Operand op1 = deOp.decomposeOperand(f1);
				compileInstruction(insn, op0, op1 != null ? op1 : mask(op0).compileOp(f1));
			}

			private void compileInstruction(Insn insn, Operand op0, Operand op1) {
				if (op0 instanceof OpMem && op1 instanceof OpMem || op0 instanceof OpImm) {
					Operand oldOp1 = op1;
					em.emit(amd64.instruction(Insn.MOV, op1 = rs.mask(op0).get(op1.size), oldOp1));
				}
				em.emit(amd64.instruction(insn, op0, op1));
			}

			private OpReg compileRegInstruction(Insn insn, OpReg op0, Operand op1, Funp f1) {
				return em.emitRegInsn(insn, op0, op1 != null ? op1 : mask(op0).compileOp(f1));
			}

			private void compileInvoke(Funp n) {
				CompileOut out = compileTwoOp(n);
				Operand op;
				if (!new RegisterSet().mask(out.op1).contains(ebp))
					op = out.op1;
				else
					em.mov(op = rs.mask(out.op0).get(ps), out.op1);
				em.mov(ebp, out.op0);
				em.emit(amd64.instruction(Insn.CALL, op));
			}

			private OpReg compileLoad(Funp node) {
				return isOutSpec ? compileOpSpec(node, pop0) : compileOpReg(node);
			}

			private void compileAssign(Funp n, FunpMemory target) {
				new Compile0(CompileOut_.ASSIGN, em, target, null, null).new Compile1(rs, fd).compile(n);
			}

			private Operand compileOp(Funp n) {
				return new Compile0(CompileOut_.OP, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpReg(Funp n) {
				return (OpReg) new Compile0(CompileOut_.OPREG, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpSpec(Funp n, OpReg op) {
				new Compile0(CompileOut_.OPSPEC, em, null, op, null).new Compile1(rs, fd).compile(n);
				return op;
			}

			private CompileOut compileTwoOp(Funp n) {
				return new Compile0(CompileOut_.TWOOP, em).new Compile1(rs, fd).compile(n);
			}

			private CompileOut compileTwoOpSpec(Funp n, OpReg op0, OpReg op1) {
				new Compile0(CompileOut_.TWOOPSPEC, em, null, op0, op1).new Compile1(rs, fd).compile(n);
				return new CompileOut(pop0, pop1);
			}

			private void compileMove(OpReg r0, int start0, OpReg r1, int start1, int size) {
				Sink2<Compile1, OpReg> sink = (c1, r) -> {
					int s = r.size;
					em.mov(r, amd64.mem(r1, start1, s));
					em.mov(amd64.mem(r0, start0, s), r);
					c1.compileMove(r0, start0 + s, r1, start1 + s, size - s);
				};

				if (r0 != r1 || start0 != start1)
					if (16 < size)
						saveRegs(c1 -> {
							OpReg r = rs.mask(r0, edi).get(esi);
							em.lea(r, amd64.mem(r1, start1, is));
							em.lea(edi, amd64.mem(r0, start0, is));
							em.mov(esi, r);
							em.mov(ecx, amd64.imm(size / 4, is));
							em.emit(amd64.instruction(Insn.CLD));
							em.emit(amd64.instruction(Insn.REP));
							em.emit(amd64.instruction(Insn.MOVSD));
							for (int i = 0; i < size % 4; i++)
								em.emit(amd64.instruction(Insn.MOVSB));
						}, ecx, esi, edi);
					else if (is <= size)
						sink.sink2(this, rs.get(is));
					else if (0 < size)
						saveRegs(c1 -> sink.sink2(c1, cl), ecx);
			}

			private FunpMemory frame(int start, int end) {
				return FunpMemory.of(Funp_.framePointer, start, end);
			}

			private void saveRegs(Sink<Compile1> sink, OpReg... opRegs) {
				saveRegs(sink, rs, fd, 0, opRegs);
			}

			private void saveRegs(Sink<Compile1> sink, RegisterSet rs_, int fd_, int index, OpReg... opRegs) {
				OpReg op;
				if (index < opRegs.length && rs_.contains(op = opRegs[index])) {
					em.emit(amd64.instruction(Insn.PUSH, op));
					saveRegs(sink, rs_.unmask(op.reg), fd_ - op.size, index + 1, opRegs);
					em.emit(amd64.instruction(Insn.POP, op));
				} else
					sink.sink(new Compile1(rs_, fd_));
			}

			private Compile1 mask(Operand... ops) {
				return new Compile1(rs.mask(ops), fd);
			}
		}
	}

	private class CompileOut {
		private Operand op0, op1;

		private CompileOut() {
			this(null);
		}

		private CompileOut(Operand op0) {
			this(op0, null);
		}

		private CompileOut(Operand op0, Operand op1) {
			this.op0 = op0;
			this.op1 = op1;
		}
	}

	private enum CompileOut_ {
		ASSIGN, // assign value to certain memory region
		OP, // put value to an operand (r/m or immediate)
		OPREG, // put value to a register operand
		OPSPEC, // put value to a specified operand
		TWOOP, // put value to an operand pair
		TWOOPREG, // put value to a register operand pair
		TWOOPSPEC, // put value to a specified operand pair
	};

}
