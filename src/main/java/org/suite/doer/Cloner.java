package org.suite.doer;

import java.util.HashMap;
import java.util.Map;

import org.suite.kb.Rule;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;

public class Cloner {

	private Map<Reference, Reference> references = new HashMap<>();

	public Rule clone(Rule rule) {
		return new Rule(clone(rule.getHead()), clone(rule.getTail()));
	}

	public Node clone(Node node) {
		Tree tree = Tree.create(null, null, node);
		cloneRight(tree);
		return tree.getRight();
	}

	private void cloneRight(Tree tree) {
		while (true) {
			Node right = tree.getRight().finalNode();

			if (right instanceof Reference)
				right = getNewReference((Reference) right);

			if (right instanceof Tree) {
				Tree rightTree = (Tree) right;
				rightTree = Tree.create(rightTree.getOperator(), clone(rightTree.getLeft()), rightTree.getRight());
				Tree.forceSetRight(tree, rightTree);
				tree = rightTree;
				continue;
			}

			Tree.forceSetRight(tree, right);
			break;
		}
	}

	public Node cloneOld(Node node) {
		node = node.finalNode();

		if (node instanceof Reference)
			node = getNewReference((Reference) node);

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = tree.getLeft(), right = tree.getRight();
			Node left1 = clone(left), right1 = clone(right);
			if (left != left1 || right != right1)
				node = Tree.create(tree.getOperator(), left1, right1);
		}

		return node;
	}

	private Node getNewReference(Reference oldRef) {
		Node node = references.get(oldRef);

		if (node == null) {
			Reference newRef = new Reference();
			node = newRef;
			references.put(oldRef, newRef);
		}

		return node;
	}

}
