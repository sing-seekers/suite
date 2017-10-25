package suite.fp.intrinsic;

import java.util.List;

import suite.Suite;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.util.To;

public class SuiteIntrinsics {

	public Intrinsic match = (callback, inputs) -> {
		String s = Data.get(inputs.get(0)).toString();
		Node n = Data.get(inputs.get(1));
		Node[] m = Suite.match(s.toString()).apply(n);
		if (m != null)
			return Intrinsics.drain(callback, p -> new Data<>(m[p]), m.length);
		else
			return Intrinsics.id_.invoke(callback, List.of(Atom.NIL));
	};

	public Intrinsic parse = (callback, inputs) -> {
		String s = Data.get(inputs.get(0)).toString();
		return new Data<>(Suite.parse(s));
	};

	public Intrinsic substitute = (callback, inputs) -> {
		String s = Data.get(inputs.get(0)).toString();
		Node[] array = ThunkUtil.yawnList(callback::yawn, inputs.get(1), true) //
				.map(Data::<Node> get) //
				.toArray(Node.class);
		return new Data<>(Suite.substitute(s, array));
	};

	public Intrinsic toChars = (callback, inputs) -> {
		Node n = Data.get(inputs.get(0));
		return new Data<>(To.chars(Formatter.dump(n)));
	};

}
