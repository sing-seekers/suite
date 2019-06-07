package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.object.Object_;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.primitive.IntChr_Obj;
import suite.primitive.IntFunUtil;
import suite.primitive.Int_Int;
import suite.streamlet.FunUtil.Iterate;

public class IntChrPair {

	private static IntChrPair none_ = IntChrPair.of(IntFunUtil.EMPTYVALUE, ChrFunUtil.EMPTYVALUE);

	public int t0;
	public char t1;

	public static Iterate<IntChrPair> mapFst(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<IntChrPair> mapSnd(Chr_Chr fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntChrPair none() {
		return none_;
	}

	public static IntChrPair of(int t0, char t1) {
		return new IntChrPair(t0, t1);
	}

	protected IntChrPair(int t0, char t1) {
		update(t0, t1);
	}

	public static Comparator<IntChrPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntChrPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int fst(IntChrPair pair) {
		return pair.t0;
	}

	public static char snd(IntChrPair pair) {
		return pair.t1;
	}

	public <O> O map(IntChr_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(int t0_, char t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntChrPair.class) {
			var other = (IntChrPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Character.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
