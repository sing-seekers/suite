package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.Objects;

import primal.Ob;
import primal.adt.Opt;
import suite.primitive.IntPrimitives.IntTest;
import suite.primitive.IntPrimitives.Int_Obj;

public class IntOpt {

	private static int empty = IntFunUtil.EMPTYVALUE;
	private static IntOpt none_ = IntOpt.of(empty);

	private int value;

	public static IntOpt none() {
		return none_;
	}

	public static IntOpt of(int t) {
		var p = new IntOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == empty;
	}

	public IntOpt filter(IntTest pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Int_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public int get() {
		return !isEmpty() ? value : fail("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Ob.clazz(object) == IntOpt.class && Ob.equals(value, ((IntOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != empty ? Integer.toString(value) : "null";
	}

}
