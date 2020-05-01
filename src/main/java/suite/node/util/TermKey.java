package suite.node.util;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Pair;
import primal.fp.Funs.Sink;
import primal.primitive.IntPrim.IntSink;
import primal.primitive.adt.map.IntObjMap;
import primal.primitive.adt.pair.IntObjPair;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewrite_.NodeHead;
import suite.node.io.Rewrite_.NodeRead;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Node.hashCode() method would not permit taking hash code of terms with
 * free references.
 *
 * This method allows such thing by giving aliases, thus "a + .123" and "a +
 * .456" will have same hash keys.
 *
 * @author ywsing
 */
public class TermKey implements Comparable<TermKey> {

	public class TermVisitor {
		private int nAliases = 0;
		private IntObjMap<Integer> aliases = new IntObjMap<>();
		private IntSink referenceSink;
		private Sink<NodeRead> nrSink;

		public TermVisitor(IntSink referenceSink, Sink<NodeRead> nrSink) {
			this.referenceSink = referenceSink;
			this.nrSink = nrSink;
		}

		public void visit(Node node) {
			if (node instanceof Reference) {
				var id = ((Reference) node).getId();
				referenceSink.f(aliases.computeIfAbsent(id, any -> nAliases++));
			} else {
				var nr = NodeRead.of(node);
				for (var p : nr.children) {
					visit(p.k);
					visit(p.v);
				}
				nrSink.f(nr);
			}
		}
	}

	private class TermHasher {
		private int h = 7;

		public TermHasher(Node node) {
			new TermVisitor(
					i -> h = h * 31 + i
					, nr -> h = h * 31 + Objects.hash(nr.type, nr.terminal, nr.op)
			).visit(node);
		}
	}

	private class TermLister {
		private List<IntObjPair<NodeHead>> list = new ArrayList<>();

		public TermLister(Node node) {
			new TermVisitor(i -> list.add(IntObjPair.of(i, null)), nr -> Pair.of(null, nr)).visit(node);
		}

		public boolean equals(Object object) {
			var b = Get.clazz(object) == TermLister.class;

			if (b) {
				var list1 = ((TermLister) object).list;
				var size0 = list.size();
				var size1 = list1.size();
				b &= size0 == size1;

				if (b)
					for (var i = 0; b && i < size0; i++) {
						var p0 = list.get(i);
						var p1 = list1.get(i);
						b &= Equals.ab(p0.k, p1.k);

						var nh0 = p0.v;
						var nh1 = p1.v;
						var b0 = nh0 != null;
						var b1 = nh1 != null;
						b &= b0 == b1;
						if (b0 && b1)
							b &= Equals.ab(nh0.type, nh1.type)
									&& Equals.ab(nh0.terminal, nh1.terminal)
									&& Equals.ab(nh0.op, nh1.op);
					}
			}

			return b;
		}

		public int hashCode() {
			var h = 7;
			for (var pair : list) {
				h = h * 31 + Objects.hash(pair.k);
				if (pair.v != null) {
					h = h * 31 + Objects.hash(pair.v.type);
					h = h * 31 + Objects.hash(pair.v.terminal);
					h = h * 31 + Objects.hash(pair.v.op);
				}
			}
			return h;
		}
	}

	public final Node node;

	public TermKey(Node node) {
		this.node = node;
	}

	@Override
	public int compareTo(TermKey other) {
		return Integer.compare(hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == TermKey.class) {
			var node1 = ((TermKey) object).node;
			var tl0 = new TermLister(node);
			var tl1 = new TermLister(node1);
			return Equals.ab(tl0, tl1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return new TermHasher(node).h;
	}

}
