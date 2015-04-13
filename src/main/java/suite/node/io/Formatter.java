package suite.node.io;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.adt.IdentityKey;
import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.parser.CommentPreprocessor;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Formats a node for human-readable purpose (display), or for
 * computer-parseable purpose (dump).
 *
 * Data, Dict and Tuple, even if they are dumped, cannot be parsed back into
 * their corresponding nodes.
 *
 * @author ywsing
 */
public class Formatter {

	private boolean isDump;
	private Set<Integer> set = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	/**
	 * Converts a node into graph representation. The nodes link to other nodes
	 * via an integer key.
	 */
	private static class Graphizer {
		private int count;
		private Map<IdentityKey<Node>, Integer> ids = new HashMap<>();
		private StringBuilder sb = new StringBuilder();

		private int graphize(Node node) {
			IdentityKey<Node> key = IdentityKey.of(node.finalNode());
			Integer id = ids.get(key);
			Tree tree;

			if (id == null) {
				ids.put(key, id = count++);
				String content;

				if (node instanceof Dict)
					content = Read.from(((Dict) node).map.entrySet()) //
							.map(e -> graphize(e.getKey()) + ":" + graphize(e.getValue()) + ", ") //
							.collect(As.joined("dict(", ", ", ")"));
				else if ((tree = Tree.decompose(node)) != null) {
					int id0 = graphize(tree.getLeft());
					int id1 = graphize(tree.getRight());
					content = "tree(" + id0 + tree.getOperator().getName() + id1 + ")";
				} else if (node instanceof Tuple)
					content = Read.from(((Tuple) node).nodes) //
							.map(n -> graphize(n) + ", ") //
							.collect(As.joined("tuple(", ", ", ")"));
				else
					content = dump(node);

				sb.append(id + " = " + content + "\n");
			}

			return id;
		}
	}

	/**
	 * Converts a node into an ugly tree representation. Children are dumped
	 * line-by-line with indentation.
	 */
	private static class Treeizer {
		private StringBuilder sb = new StringBuilder();

		private void treeize(Node node, String indent) {
			Tree tree = Tree.decompose(node);
			String indent1 = indent + "  ";

			if (tree != null) {
				String op = tree.getOperator().getName();
				op = Util.stringEquals(op, " ") ? "<>" : op.trim();

				treeize(tree.getLeft(), indent1);
				sb.append(indent + op + "\n");
				treeize(tree.getRight(), indent1);
			} else if (node instanceof Dict)
				for (Entry<Node, Reference> entry : ((Dict) node).map.entrySet()) {
					sb.append(indent + "d:" + dump(entry.getKey()) + "\n");
					treeize(entry.getValue().finalNode(), indent1);
				}
			else if (node instanceof Tuple)
				for (Node child : ((Tuple) node).nodes)
					sb.append(indent + "t:" + dump(child) + "\n");
			else
				sb.append(indent + dump(node) + "\n");
		}
	}

	public Formatter(boolean isDump) {
		this.isDump = isDump;
	}

	public static String display(Node node) {
		return new Formatter(false).format(node);
	}

	public static String dump(Node node) {
		return new Formatter(true).format(node);
	}

	public static String graphize(Node node) {
		Graphizer graphizer = new Graphizer();
		int fn = graphizer.graphize(node);
		return graphizer.sb.toString() + "return(" + fn + ")\n";
	}

	public static String treeize(Node node) {
		Treeizer treeize = new Treeizer();
		treeize.treeize(node, "");
		return treeize.sb.toString();
	}

	private String format(Node node) {
		format(node, 0);
		return sb.toString();
	}

	/**
	 * Converts a node to its string representation.
	 *
	 * @param node
	 *            Node to be converted.
	 * @param parentPrec
	 *            Minimum operator precedence without adding parentheses.
	 */
	private void format(Node node, int parentPrec) {
		node = node.finalNode();
		Integer objectId = System.identityHashCode(node);

		// Avoids infinite recursion if object is recursive
		if (set.add(objectId)) {
			format0(node, parentPrec);
			set.remove(objectId);
		} else
			sb.append("<<recurse>>");
	}

	private void format0(Node node, int parentPrec) {
		if (node instanceof Atom)
			sb.append(quoteAtomIfRequired(((Atom) node).name));
		else if (node instanceof Data) {
			Object data = Data.get(node);
			if (data instanceof Chars)
				sb.append("Chars<" + quoteStringIfRequired(((Chars) data).toString()) + ">");
			else
				sb.append("Data<" + data.getClass().getSimpleName() + ">");
		} else if (node instanceof Dict) {
			sb.append("dict<");
			for (Entry<Node, Reference> entry : ((Dict) node).map.entrySet()) {
				format(entry.getKey(), 0);
				sb.append(":");
				format(entry.getValue(), 0);
				sb.append(",");
			}
			sb.append(">");
		} else if (node instanceof Int)
			sb.append(((Int) node).number);
		else if (node instanceof Reference)
			sb.append(SewingGeneralizer.variablePrefix + ((Reference) node).getId());
		else if (node instanceof Str)
			sb.append(quoteStringIfRequired(((Str) node).value));
		else if (node instanceof Tree)
			formatTree(parentPrec, (Tree) node);
		else if (node instanceof Tuple) {
			sb.append("tuple<");
			for (Node n : ((Tuple) node).nodes) {
				format(n, 0);
				sb.append(", ");
			}
			sb.append(">");
		} else
			sb.append(node.getClass().getSimpleName() + '@' + Integer.toHexString(node.hashCode()));
	}

	private void formatTree(int parentPrec, Tree tree) {
		if (tree.getOperator() == TermOp.TUPLE_ && tree.getLeft().finalNode() == Atom.of("[")) {
			sb.append("[");
			format(tree.getRight(), 0);
			sb.append("]");
		} else
			formatTree0(tree, parentPrec);
	}

	private void formatTree0(Tree tree, int parentPrec) {
		if (tree != null && tree.getOperator().getPrecedence() <= parentPrec) {
			sb.append("(");
			formatTree(tree);
			sb.append(")");
		} else
			formatTree(tree);
	}

	private void formatTree(Tree tree) {
		Node[] m;
		if ((m = CustomStyles.braceMatcher.apply(tree)) != null) {
			format(m[0], TermOp.getLeftPrec(TermOp.BRACES));
			sb.append(" {");
			format(m[1], 0);
			sb.append("}");
		} else
			formatTree0(tree);
	}

	private void formatTree0(Tree tree) {
		Operator operator = tree.getOperator();
		Node left = tree.getLeft();
		Node right = tree.getRight();
		boolean isSpaceBefore = TermOp.isSpaceBefore(operator);
		boolean isSpaceAfter = TermOp.isSpaceAfter(operator);

		format(left, TermOp.getLeftPrec(operator));
		if (isSpaceBefore)
			sb.append(' ');
		sb.append(operator.getName());
		if (isSpaceAfter && right != Atom.NIL)
			sb.append(' ');
		if (!isSpaceAfter || right != Atom.NIL)
			format(right, TermOp.getRightPrec(operator));
		// a, () suppressed as a,
	}

	private String quoteAtomIfRequired(String s0) {
		String s1;
		if (isDump)
			if (!s0.isEmpty()) {
				boolean quote = false;

				for (char c : Util.chars(s0))
					quote |= !('0' <= c && c <= '9') //
							&& !('a' <= c && c <= 'z') //
							&& !('A' <= c && c <= 'Z') //
							&& c != '.' && c != '-' && c != '_' && c != '$' && c != '!';

				quote |= s0.contains(CommentPreprocessor.openGroupComment) //
						|| s0.contains(CommentPreprocessor.closeGroupComment) //
						|| s0.contains(CommentPreprocessor.openLineComment) //
						|| s0.contains(CommentPreprocessor.closeLineComment);

				quote |= ParseUtil.isInteger(s0);

				s1 = quote ? Escaper.escape(s0, '\'') : s0;
			} else
				s1 = "()";
		else
			s1 = s0;
		return s1;
	}

	private String quoteStringIfRequired(String s) {
		return isDump ? Escaper.escape(s, '"') : s;
	}

}
