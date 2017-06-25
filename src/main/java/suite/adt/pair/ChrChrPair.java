package suite.adt.pair;

import java.util.Comparator;

import suite.adt.Opt;
import suite.primitive.ChrChr_Obj;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ChrChrPair {

	private static ChrChrPair none_ = ChrChrPair.of(ChrFunUtil.EMPTYVALUE, ChrFunUtil.EMPTYVALUE);

	public char t0;
	public char t1;

	public static Fun<ChrChrPair, ChrChrPair> map0(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<ChrChrPair, ChrChrPair> map1(Chr_Chr fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrChrPair none() {
		return none_;
	}

	public static ChrChrPair of(char t0, char t1) {
		return new ChrChrPair(t0, t1);
	}

	private ChrChrPair(char t0, char t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ChrChrPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ChrChrPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public <O> Opt<O> map(ChrChr_Obj<O> fun) {
		return t0 != ChrFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public static char first_(ChrChrPair pair) {
		return pair.t0;
	}

	public static char second(ChrChrPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrChrPair.class) {
			ChrChrPair other = (ChrChrPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Character.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
