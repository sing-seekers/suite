package org.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.parser.Operator;
import org.suite.doer.Comparer;
import org.suite.doer.Formatter;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.node.Tree;

public class Interpreter {

	private Map<Atom, Node> functions = new TreeMap<Atom, Node>();

	private static final Atom ELSE = Atom.create("else");
	private static final Atom FALSE = Atom.create("false");
	private static final Atom IF = Atom.create("if");
	private static final Atom LEFT = Atom.create("left");
	private static final Atom NOT = Atom.create("not");
	private static final Atom OPER = Atom.create("oper");
	private static final Atom PLAIN = Atom.create("p");
	private static final Atom RIGHT = Atom.create("right");
	private static final Atom SWITCH = Atom.create("switch");
	private static final Atom THEN = Atom.create("then");
	private static final Atom TREE = Atom.create("tree");
	private static final Atom TRUE = Atom.create("true");

	public void addFunctions(Node node) {
		for (Node f : flatten(node, TermOp.NEXT__)) {
			Tree tree = Tree.decompose(f, TermOp.IS____);
			if (tree != null)
				addFunction((Atom) tree.getLeft(), tree.getRight());
		}
	}

	public void addFunction(Atom head, Node body) {
		functions.put(head, body);
	}

	// Evaluation method:
	// - Substitute as much as possible;
	// - Handle "if"s and switches by evaluating their determinants;
	// - Expand and repeat above, until the node is not changed anymore.
	// - Simplify the final mess we have got.
	public Node evaluate(Node node) {
		Node previous = null;

		do {
			previous = node;
			node = performSubst(node);
			node = performDetermination(node);
			node = performExpand(node);
		} while (Comparer.comparer.compare(previous, node) != 0);

		return simplify(node);
	}

	private Node performSubst(Node node) {
		node = node.finalNode();

		if (node instanceof Tree) {
			Tree t = (Tree) node;
			Node l = t.getLeft(), r = t.getRight();
			Node gl = performSubst(l), gr = performSubst(r);

			if (t.getOperator() == TermOp.DIVIDE) {
				Tree lambda = Tree.decompose(gl, TermOp.INDUCE);

				if (lambda != null) {
					Node body = lambda.getRight();
					Node variable = lambda.getLeft();
					EvaluatableReference value = new EvaluatableReference(gr);
					node = replace(body, variable, value);
					return node;
				}
			}

			if (gl != l || gr != r)
				node = new Tree(t.getOperator(), gl, gr);
		}

		return node;
	}

	private Node performDetermination(Node node) {
		node = node.finalNode();

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Operator operator = tree.getOperator();
			Node l = tree.getLeft(), r = tree.getRight();

			if (operator == TermOp.SEP___) {
				List<Node> list = flatten(tree, TermOp.SEP___);
				Node name = list.get(0);

				if (name == IF && list.get(2) == THEN && list.get(4) == ELSE)
					node = ifThenElse(list.get(1), list.get(3), list.get(5));
				else if (name == SWITCH)
					node = doSwitch(list);
			} else {
				Node gl = performDetermination(l), gr = performDetermination(r);
				if (gl != l || gr != r)
					node = new Tree(operator, gl, gr);
			}
		}

		return node;
	}

	private Node performExpand(Node node) {
		node = node.finalNode();

		if (node instanceof Atom) {
			Atom atom = (Atom) node;
			Node definition = functions.get(atom);
			if (definition != null)
				node = definition;
		} else if (node instanceof Tree) {
			Tree t = (Tree) node;
			Node l = t.getLeft(), r = t.getRight();
			Node gl = performExpand(l), gr = performExpand(r);
			if (gl != l || gr != r)
				node = new Tree(t.getOperator(), gl, gr);
		}

		return node;
	}

	private Node simplify(Node node) {

		// Late and lazy evaluation; evaluates a reference once and for all
		if (node instanceof EvaluatableReference) {
			EvaluatableReference reference = (EvaluatableReference) node;
			if (!reference.evaluated) {
				Node evaluated = evaluate(reference.finalNode());
				reference.bound(evaluated);
				return evaluated;
			}
		}

		Tree tree = Tree.decompose(node);
		if (tree != null)
			node = performSimplify(tree);
		return node;
	}

	private Node performSimplify(Tree tree) {
		TermOp operator = (TermOp) tree.getOperator();
		Node l = tree.getLeft(), r = tree.getRight();

		if (operator == TermOp.SEP___) {
			if (l == PLAIN)
				return r;

			Node param = simplify(r);

			if (l == NOT)
				return param == TRUE ? FALSE : TRUE;
			else if (l == TREE)
				return param.finalNode() instanceof Tree ? TRUE : FALSE;
			else if (l == LEFT || l == RIGHT || l == OPER) {
				Tree t = Tree.decompose(param);
				return l == OPER ? new Str(t.getOperator().getName())
						: l == LEFT ? t.getLeft() : t.getRight();
			}
		} else if (operator == TermOp.EQUAL_)
			return eq(simplify(l), simplify(r));
		else if (operator == TermOp.LT____ || operator == TermOp.LE____
				|| operator == TermOp.GT____ || operator == TermOp.LE____
				|| operator == TermOp.PLUS__ || operator == TermOp.MINUS_
				|| operator == TermOp.MULT__ || operator == TermOp.DIVIDE) {
			int n1 = getNumber(simplify(l)), n2 = getNumber(simplify(r));

			switch (operator) {
			case LT____:
				return n1 < n2 ? TRUE : FALSE;
			case LE____:
				return n1 <= n2 ? TRUE : FALSE;
			case GT____:
				return n1 > n2 ? TRUE : FALSE;
			case GE____:
				return n1 >= n2 ? TRUE : FALSE;
			case PLUS__:
				return Int.create(n1 + n2);
			case MINUS_:
				return Int.create(n1 - n2);
			case MULT__:
				return Int.create(n1 * n2);
			case DIVIDE:
				return Int.create(n1 / n2);
			}
		}

		Node gl = simplify(l), gr = simplify(r);
		if (gl != l || gr != r)
			return new Tree(operator, gl, gr);

		// throw new RuntimeException("Cannot simplify " +
		// Formatter.dump(tree));

		return tree;
	}

	private Node ifThenElse(Node if_, Node then_, Node else_) {
		return evaluate(if_) == TRUE ? then_ : else_;
	}

	private Node doSwitch(List<Node> list) {
		int last = list.size() - 1;
		for (int i = 1; i < last; i++) {
			Tree t = Tree.decompose(list.get(i), TermOp.INDUCE);
			if (t != null) {
				if (evaluate(t.getLeft()) == TRUE)
					return t.getRight();
			} else
				throw new RuntimeException("Bad switch definition");
		}
		return evaluate(list.get(last));
	}

	private static Node replace(Node node, Node from, Node to) {
		node = node.finalNode();

		if (node == from)
			return to;
		else if (node instanceof Tree) {
			Tree t = (Tree) node;
			Node l = t.getLeft(), r = t.getRight();
			Node gl = replace(l, from, to), gr = replace(r, from, to);
			if (gl != l || gr != r)
				return new Tree(t.getOperator(), gl, gr);
		}

		return node;
	}

	private static List<Node> flatten(Node node, Operator operator) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		Tree tree;

		while ((tree = Tree.decompose(node, operator)) != null) {
			nodes.add(tree.getLeft());
			node = tree.getRight();
		}

		nodes.add(node);
		return nodes;
	}

	private Atom eq(Node left, Node right) {
		return Comparer.comparer.compare(left, right) == 0 ? TRUE : FALSE;
	}

	private int getNumber(Node node) {
		if (node instanceof Int)
			return ((Int) node).getNumber();
		else
			throw new RuntimeException(Formatter.dump(node)
					+ " is not a number");
	}

}
