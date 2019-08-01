package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.FltPrim;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.FltPuller;

public interface Flt_Chr {

	public char apply(float c);

	public static Fun<FltPuller, ChrStreamlet> lift(Flt_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			float c;
			while ((c = ts.pull()) != FltPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<FltPuller> sum(Flt_Chr fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			float c;
			var result = (char) 0;
			while ((c = source.g()) != FltPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
