package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Dbl_Dbl;
import suite.primitive.Flt_Flt;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class FltDblPair {

	public float t0;
	public double t1;

	public static Fun<FltDblPair, FltDblPair> map0(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<FltDblPair, FltDblPair> map1(Dbl_Dbl fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltDblPair of(float t0, double t1) {
		return new FltDblPair(t0, t1);
	}

	private FltDblPair(float t0, double t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<FltDblPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Double.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltDblPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static float first_(FltDblPair pair) {
		return pair.t0;
	}

	public static double second(FltDblPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltDblPair.class) {
			FltDblPair other = (FltDblPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Double.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
