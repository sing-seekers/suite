package suite.instructionexecutor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.instructionexecutor.io.IndexedIo;
import suite.lp.invocable.Invocables.InvocableNode;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermParser.TermOp;
import suite.node.util.Comparer;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionUtil {

	private static final BiMap<Insn, String> insnNames = HashBiMap.create();
	private static final Map<Operator, Insn> evalInsns = new HashMap<>();

	static {
		for (Insn insn : Insn.values())
			insnNames.put(insn, insn.name);

		evalInsns.put(TermOp.PLUS__, Insn.EVALADD_______);
		evalInsns.put(TermOp.DIVIDE, Insn.EVALDIV_______);
		evalInsns.put(TermOp.EQUAL_, Insn.EVALEQ________);
		evalInsns.put(TermOp.GE____, Insn.EVALGE________);
		evalInsns.put(TermOp.GT____, Insn.EVALGT________);
		evalInsns.put(TermOp.LE____, Insn.EVALLE________);
		evalInsns.put(TermOp.LT____, Insn.EVALLT________);
		evalInsns.put(TermOp.MULT__, Insn.EVALMUL_______);
		evalInsns.put(TermOp.MINUS_, Insn.EVALSUB_______);
		evalInsns.put(TermOp.MODULO, Insn.EVALMOD_______);
		evalInsns.put(TermOp.NOTEQ_, Insn.EVALNE________);
	}

	public enum Insn {
		ASSIGNCLOSURE_("ASSIGN-CLOSURE"), //
		ASSIGNCONST___("ASSIGN-CONSTANT"), //
		ASSIGNFRAMEREG("ASSIGN-FRAME-REG"), //
		ASSIGNGLOBAL__("ASSIGN-GLOBAL"), //
		ASSIGNINT_____("ASSIGN-INT"), //
		BACKUPCSP_____("BACKUP-CSP"), //
		BACKUPDSP_____("BACKUP-DSP"), //
		BIND__________("BIND"), //
		BINDMARK______("BIND-MARK"), //
		BINDUNDO______("BIND-UNDO"), //
		CALL__________("CALL"), //
		CALLCLOSURE___("CALL-CLOSURE"), //
		CALLREG_______("CALL-REG"), //
		COMPARE_______("COMPARE"), //
		CONSLIST______("CONS-LIST"), //
		CONSPAIR______("CONS-PAIR"), //
		DECOMPOSETREE0("DECOMPOSE-TREE0"), //
		DECOMPOSETREE1("DECOMPOSE-TREE1"), //
		ENTER_________("ENTER"), //
		ERROR_________("ERROR"), //
		EVALADD_______("EVAL-ADD"), //
		EVALDIV_______("EVAL-DIV"), //
		EVALEQ________("EVAL-EQ"), //
		EVALGE________("EVAL-GE"), //
		EVALGT________("EVAL-GT"), //
		EVALLE________("EVAL-LE"), //
		EVALLT________("EVAL-LT"), //
		EVALMOD_______("EVAL-MOD"), //
		EVALMUL_______("EVAL-MUL"), //
		EVALNE________("EVAL-NE"), //
		EVALSUB_______("EVAL-SUB"), //
		EXIT__________("EXIT"), //
		FGETC_________("FGETC"), //
		FORMTREE0_____("FORM-TREE0"), //
		FORMTREE1_____("FORM-TREE1"), //
		HEAD__________("HEAD"), //
		IFFALSE_______("IF-FALSE"), //
		IFGE__________("IF-GE"), //
		IFGT__________("IF-GT"), //
		IFLE__________("IF-LE"), //
		IFLT__________("IF-LT"), //
		IFNOTEQUALS___("IF-NOT-EQ"), //
		INVOKEJAVACLS_("INVOKE-JAVA-CLASS"), //
		INVOKEJAVAOBJ0("INVOKE-JAVA-OBJ0"), //
		INVOKEJAVAOBJ1("INVOKE-JAVA-OBJ1"), //
		INVOKEJAVAOBJ2("INVOKE-JAVA-OBJ2"), //
		ISCONS________("IS-CONS"), //
		ISVECTOR______("IS-VECTOR"), //
		JUMP__________("JUMP"), //
		JUMPREG_______("JUMP-REG"), //
		LABEL_________("LABEL"), //
		LOG___________("LOG"), //
		LOG1__________("LOG1"), //
		LEAVE_________("LEAVE"), //
		NEWNODE_______("NEW-NODE"), //
		POP___________("POP"), //
		POPANY________("POP-ANY"), //
		POPEN_________("POPEN"), //
		PROVEINTERPRET("PROVE-INTERPRET"), //
		PROVESYS______("PROVE-SYS"), //
		PUSH__________("PUSH"), //
		PUSHCONST_____("PUSH-CONSTANT"), //
		REMARK________("REMARK"), //
		RESTORECSP____("RESTORE-CSP"), //
		RESTOREDSP____("RESTORE-DSP"), //
		RETURN________("RETURN"), //
		RETURNVALUE___("RETURN-VALUE"), //
		SETCLOSURERES_("SET-CLOSURE-RESULT"), //
		SETRESULT_____("SET-RESULT"), //
		STOREGLOBAL___("STORE-GLOBAL"), //
		TAIL__________("TAIL"), //
		TOP___________("TOP"), //
		VCONCAT_______("VCONCAT"), //
		VCONS_________("VCONS"), //
		VELEM_________("VELEM"), //
		VEMPTY________("VEMPTY"), //
		VHEAD_________("VHEAD"), //
		VRANGE________("VRANGE"), //
		VTAIL_________("VTAIL"), //
		;

		String name;

		private Insn(String name) {
			this.name = name;
		}
	}

	protected static class Instruction {
		protected Insn insn;
		protected int op0, op1, op2;

		protected Instruction(Insn insn, int op0, int op1, int op2) {
			this.insn = insn;
			this.op0 = op0;
			this.op1 = op1;
			this.op2 = op2;
		}

		public String toString() {
			return insn.name + " " + op0 + ", " + op1 + ", " + op2;
		}
	}

	protected static class Activation extends Closure {
		protected Activation previous;

		protected Activation(Closure closure, Activation previous) {
			this(closure.frame, closure.ip, previous);
		}

		protected Activation(Frame frame, int ip, Activation previous) {
			super(frame, ip);
			this.previous = previous;
		}
	}

	// Indicates a function call with a specified set of framed environment.
	// Closure must extend Node in order to be put in a list (being cons-ed).
	protected static class Closure extends Node {
		protected Frame frame;
		protected int ip;
		protected Node result;

		protected Closure(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}

		public String toString() {
			return "frameSize = " + frame.registers.length + ", IP = " + ip;
		}
	}

	protected static class Frame {
		protected Frame previous;
		protected Node registers[];

		protected Frame(Frame previous, int frameSize) {
			this.previous = previous;
			registers = new Node[frameSize];
		}
	}

	public static class FunComparer extends Comparer {
		private Fun<Node, Node> unwrapper;

		public FunComparer(Fun<Node, Node> unwrapper) {
			this.unwrapper = unwrapper;
		}

		public int compare(Node n0, Node n1) {
			return super.compare(unwrapper.apply(n0), unwrapper.apply(n1));
		}
	}

	public static Insn getEvalInsn(TermOp operator) {
		return InstructionUtil.evalInsns.get(operator);
	}

	public static Insn getInsn(String insnName) {
		return InstructionUtil.insnNames.inverse().get(insnName);
	}

	public static List<Node> extractTuple(Node node) {
		List<Node> rs = new ArrayList<>(5);
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			rs.add(tree.getLeft());
			node = tree.getRight();
		}

		rs.add(node);
		return rs;
	}

	public static InvocableNode execInvokeJavaClass(String clazzName) {
		Class<? extends InvocableNode> clazz;

		try {
			@SuppressWarnings("unchecked")
			Class<? extends InvocableNode> clazz0 = (Class<? extends InvocableNode>) Class.forName(clazzName);
			clazz = clazz0;
		} catch (ClassNotFoundException ex1) {
			throw new RuntimeException(ex1);
		}

		try {
			return clazz.newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Node execPopen(Node n0, final Node n1, IndexedIo indexedIo, final Fun<Node, Node> unwrapper) {
		try {
			Node result = Atom.unique();
			final Process process = Runtime.getRuntime().exec(ExpandUtil.expandString(unwrapper, n0));

			indexedIo.put(result, new InputStreamReader(process.getInputStream()));

			// Use a separate thread to write to the process, so that read and
			// write occur at the same time and would not block up.
			// Have to make sure the executors are thread-safe!
			new Thread() {
				public void run() {
					try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos)) {
						ExpandUtil.expand(unwrapper, n1, writer);
					} catch (Exception ex) {
						LogUtil.error(ex);
					}
				}
			}.start();

			return result;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
