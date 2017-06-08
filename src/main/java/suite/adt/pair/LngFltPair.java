package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Flt_Flt;
import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class LngFltPair {

	public long t0;
	public float t1;

	public static Fun<LngFltPair, LngFltPair> map0(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<LngFltPair, LngFltPair> map1(Flt_Flt fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static LngFltPair of(long t0, float t1) {
		return new LngFltPair(t0, t1);
	}

	private LngFltPair(long t0, float t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<LngFltPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<LngFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static long first_(LngFltPair pair) {
		return pair.t0;
	}

	public static float second(LngFltPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngFltPair.class) {
			LngFltPair other = (LngFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
