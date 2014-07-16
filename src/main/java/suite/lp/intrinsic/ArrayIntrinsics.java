package suite.lp.intrinsic;

import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.lp.intrinsic.Intrinsics.Intrinsic;
import suite.lp.intrinsic.Intrinsics.IntrinsicBridge;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.TermOp;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class ArrayIntrinsics {

	public static Intrinsic append = (bridge, inputs) -> {
		List<Node> array0 = ((Tuple) inputs.get(0)).getNodes();
		List<Node> array1 = ((Tuple) inputs.get(1)).getNodes();
		return new Tuple(Util.add(array0, array1));
	};

	public static Intrinsic arrayList = new Intrinsic() {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			List<Node> array = ((Tuple) inputs.get(0)).getNodes();

			if (!array.isEmpty()) {
				Node left = bridge.wrap(BasicIntrinsics.id, array.get(0));
				Node right = bridge.wrap(this, new Tuple(array.subList(1, array.size())));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public static Intrinsic left = (bridge, inputs) -> {
		int position = ((Int) inputs.get(0)).getNumber();
		List<Node> array = ((Tuple) inputs.get(1)).getNodes();
		return new Tuple(Util.left(array, position));
	};

	public static Intrinsic listArray = (bridge, inputs) -> {
		Source<Node> value = ExpandUtil.expandList(bridge::unwrap, inputs.get(0));
		return new Tuple(To.list(value));
	};

	public static Intrinsic right = (bridge, inputs) -> {
		int position = ((Int) inputs.get(0)).getNumber();
		List<Node> array = ((Tuple) inputs.get(1)).getNodes();
		return new Tuple(Util.right(array, position));
	};

}
