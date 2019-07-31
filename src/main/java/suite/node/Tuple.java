package suite.node;

import primal.Ob;

public class Tuple extends Node {

	public final Node[] nodes;

	public static Node[] t(Node node) {
		return ((Tuple) node).nodes;
	}

	public static Tuple of(Node[] nodes) {
		return new Tuple(nodes);
	}

	private Tuple(Node[] nodes) {
		this.nodes = nodes;
	}

	@Override
	public boolean equals(Object object) {
		return Ob.clazz(object) == Tuple.class && Ob.equals(nodes, ((Tuple) object).nodes);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var node : nodes)
			h = h * 31 + node.hashCode();
		return h;
	}

}
