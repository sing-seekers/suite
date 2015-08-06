package suite.node;

import suite.node.io.Formatter;
import suite.node.util.Comparer;

public class Node implements Comparable<Node> {

	public Node finalNode() {
		return this;
	}

	@Override
	public int compareTo(Node other) {
		return Comparer.comparer.compare(this, other);
	}

	@Override
	public String toString() {
		return Formatter.dump(finalNode());
	}

}
