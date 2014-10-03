package suite.lp.kb;

import java.util.Objects;

import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.util.Util;

/**
 * Index rules by the first atom in their heads.
 *
 * @author ywsing
 */
public class Prototype implements Comparable<Prototype> {

	public final Node head;

	public static Prototype of(Rule rule) {
		return of(rule, 0);
	}

	public static Prototype of(Rule rule, int n) {
		return of(rule.head, n);
	}

	public static Prototype of(Node node) {
		return of(node, 0);
	}

	public static Prototype of(Node node, int n) {
		for (int i = 0; i < n; i++) {
			Tree tree = Tree.decompose(node);
			node = tree != null ? tree.getRight() : Atom.NIL;
		}

		if (node != null) {
			Tree t0, t1;

			while ((t1 = Tree.decompose(node)) != null) {
				t0 = t1;
				node = t0.getLeft();
			}
		}

		boolean indexable = node != null && !SewingGeneralizer.isVariant(node) && !(node instanceof Reference);
		return indexable ? new Prototype(node) : null;
	}

	private Prototype(Node head) {
		this.head = head;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(head);
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Prototype.class) {
			Prototype p = (Prototype) object;
			return Objects.equals(head, p.head);
		} else
			return false;
	}

	@Override
	public String toString() {
		return head.toString();
	}

	@Override
	public int compareTo(Prototype other) {
		return head.compareTo(other.head);
	}

}
