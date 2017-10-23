package suite.funp;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Pair;
import suite.funp.P1.FunpFramePointer;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.String_;

public class Funp_ {

	public static int booleanSize = 1;
	public static int integerSize = 4;
	public static int pointerSize = 4;

	public static FunpFramePointer framePointer = new FunpFramePointer();

	public interface Funp {
	}

	public static void dump(Funp node) {
		StringBuilder sb = new StringBuilder();
		dump(sb, node);
		LogUtil.info(sb.toString());
	}

	private static void dump(StringBuilder sb, Object object) {
		if (object instanceof Funp)
			dump_(sb, (Funp) object);
		else if (object instanceof Collection<?>) {
			sb.append("[");
			for (Object object1 : (Collection<?>) object) {
				dump(sb, object1);
				sb.append(",");
			}
			sb.append("]");
		} else if (object instanceof Pair<?, ?>) {
			Pair<?, ?> pair = (Pair<?, ?>) object;
			sb.append("<");
			dump(sb, pair.t0);
			sb.append(", ");
			dump(sb, pair.t1);
			sb.append(">");
		} else
			sb.append(object != null ? object.toString() : "null");
	}

	private static void dump_(StringBuilder sb, Funp node) {
		Class<? extends Funp> clazz = node.getClass();

		Method m = Read.from(clazz.getMethods()) //
				.filter(method -> String_.equals(method.getName(), "apply")) //
				.uniqueResult();

		Class<?> type = m.getParameters()[0].getType();
		Mutable<Object> mut = Mutable.nil();

		Fun<Object, Object> set = o -> {
			mut.set(o);
			return true;
		};

		Object p;
		if (type == FixieFun0.class)
			p = (FixieFun0<?>) () -> set.apply(List.of());
		else if (type == FixieFun1.class)
			p = (FixieFun1<?, ?>) (o0) -> set.apply(List.of(o0));
		else if (type == FixieFun2.class)
			p = (FixieFun2<?, ?, ?>) (o0, o1) -> set.apply(List.of(o0, o1));
		else if (type == FixieFun3.class)
			p = (FixieFun3<?, ?, ?, ?>) (o0, o1, o2) -> set.apply(List.of(o0, o1, o2));
		else
			throw new RuntimeException();

		Rethrow.ex(() -> m.invoke(node, p));

		sb.append(clazz.getSimpleName());
		sb.append("{");
		for (Object object : (Collection<?>) mut.get()) {
			dump(sb, object);
			sb.append(",");
		}
		sb.append("}");
	}

}
