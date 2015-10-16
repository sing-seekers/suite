package suite.adt;

import java.util.Comparator;
import java.util.Objects;

import suite.util.FunUtil.Fun;
import suite.util.Util;

public class Pair<T0, T1> {

	public T0 t0;
	public T1 t1;

	public static <K0, K1, V> Fun<Pair<K0, V>, Pair<K1, V>> map0(Fun<K0, K1> fun) {
		return pair -> Pair.of(fun.apply(pair.t0), pair.t1);
	}

	public static <K, V0, V1> Fun<Pair<K, V0>, Pair<K, V1>> map1(Fun<V0, V1> fun) {
		return pair -> Pair.of(pair.t0, fun.apply(pair.t1));
	}

	public static <T0, T1> Pair<T0, T1> of(T0 t0, T1 t1) {
		return new Pair<>(t0, t1);
	}

	private Pair(T0 t0, T1 t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T0 extends Comparable<? super T0>, T1 extends Comparable<? super T1>> Comparator<Pair<T0, T1>> comparator() {
		return (pair0, pair1) -> {
			int c = 0;
			c = c == 0 ? Util.compare(first_(pair0), first_(pair1)) : c;
			c = c == 0 ? Util.compare(second(pair0), second(pair1)) : c;
			return c;
		};
	}

	public static <T0 extends Comparable<? super T0>, T1> Comparator<Pair<T0, T1>> comparatorByFirst() {
		return (pair0, pair1) -> Util.compare(first_(pair0), first_(pair1));
	}

	public static <T0> T0 first_(Pair<T0, ?> pair) {
		return pair != null ? pair.t0 : null;
	}

	public static <T1> T1 second(Pair<?, T1> pair) {
		return pair != null ? pair.t1 : null;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Pair.class) {
			Pair<?, ?> other = (Pair<?, ?>) object;
			return Objects.equals(t0, other.t0) && Objects.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(t0) ^ Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0.toString() + ":" + t1.toString();
	}

}
