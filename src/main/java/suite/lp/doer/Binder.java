package suite.lp.doer;

import java.util.Map;
import java.util.Objects;

import suite.lp.Trail;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.util.To;

public class Binder {

	public static boolean bind(Node n0, Node n1, Trail trail) {
		n0 = n0.finalNode();
		n1 = n1.finalNode();

		if (n0 == n1)
			return true;

		Class<? extends Node> clazz0 = n0.getClass();
		Class<? extends Node> clazz1 = n1.getClass();

		if (clazz0 == Reference.class) {
			trail.addBind((Reference) n0, n1);
			return true;
		} else if (clazz1 == Reference.class) {
			trail.addBind((Reference) n1, n0);
			return true;
		}

		if (clazz0 == Dict.class && clazz1 == Dict.class) {
			Map<Node, Reference> map0 = ((Dict) n0).map;
			Map<Node, Reference> map1 = ((Dict) n1).map;
			boolean result = true;
			for (Node key : To.list(map0.keySet(), map1.keySet())) {
				Node v0 = map0.computeIfAbsent(key, k -> new Reference());
				Node v1 = map1.computeIfAbsent(key, k -> new Reference());
				result &= bind(v0, v1, trail);
			}
			return result;
		} else if (clazz0 == Int.class && clazz1 == Int.class)
			return ((Int) n0).number == ((Int) n1).number;
		else if (clazz0 == Str.class && clazz1 == Str.class)
			return Objects.equals(((Str) n0).value, ((Str) n1).value);
		else if (Tree.class.isAssignableFrom(clazz0) && Tree.class.isAssignableFrom(clazz1)) {
			Tree t0 = (Tree) n0;
			Tree t1 = (Tree) n1;
			return t0.getOperator() == t1.getOperator() //
					&& bind(t0.getLeft(), t1.getLeft(), trail) //
					&& bind(t0.getRight(), t1.getRight(), trail);
		} else if (clazz0 == Tuple.class && clazz1 == Tuple.class) {
			Node[] nodes0 = ((Tuple) n0).nodes;
			Node[] nodes1 = ((Tuple) n1).nodes;
			boolean result = nodes0.length == nodes1.length;
			if (result) {
				for (int i = 0; i < nodes0.length; i++)
					result &= bind(nodes0[i], nodes1[i], trail);
			}
			return result;
		} else
			return false;
	}

}
