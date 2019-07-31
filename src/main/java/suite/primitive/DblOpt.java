package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.Objects;

import primal.Ob;
import primal.adt.Opt;
import suite.primitive.DblPrimitives.DblTest;
import suite.primitive.DblPrimitives.Dbl_Obj;

public class DblOpt {

	private static double empty = DblFunUtil.EMPTYVALUE;
	private static DblOpt none_ = DblOpt.of(empty);

	private double value;

	public static DblOpt none() {
		return none_;
	}

	public static DblOpt of(double t) {
		var p = new DblOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == empty;
	}

	public DblOpt filter(DblTest pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Dbl_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public double get() {
		return !isEmpty() ? value : fail("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Ob.clazz(object) == DblOpt.class && Ob.equals(value, ((DblOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != empty ? Double.toString(value) : "null";
	}

}
