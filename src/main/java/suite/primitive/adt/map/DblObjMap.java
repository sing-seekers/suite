package suite.primitive.adt.map;

import java.util.Objects;

import suite.primitive.DblPrimitives.DblObjSink;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblObjOutlet;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class DblObjMap<V> {

	private int size;
	private double[] ks;
	private Object[] vs;

	public static <V> DblObjMap<V> collect(DblObjOutlet<V> outlet) {
		DblObjMap<V> map = new DblObjMap<>();
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public DblObjMap() {
		this(8);
	}

	public DblObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(double key, Dbl_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblObjMap) {
			DblObjMap<?> other = (DblObjMap<?>) object;
			boolean b = size == other.size;
			for (DblObjPair<V> pair : streamlet())
				b &= other.get(pair.t0).equals(pair.t1);
			return b;
		} else
			return false;
	}

	public void forEach(DblObjSink<V> sink) {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		DblObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(double key) {
		var index = index(key);
		return ks[index] == key ? cast(vs[index]) : null;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (DblObjPair<V> pair : streamlet()) {
			h = h * 31 + Double.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public void put(double key, V v1) {
		size++;
		store(key, v1);
		rehash();
	}

	public void update(double key, Iterate<V> fun) {
		var mask = vs.length - 1;
		var index = index(key);
		V v0 = cast(vs[index]);
		V v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != null ? 1 : 0) - (v0 != null ? 1 : 0);
		if (v1 == null)
			new Object() {
				public void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v = vs[index1];
					if (v != null) {
						var k = ks[index1];
						vs[index1] = null;
						rehash(index1);
						store(k, v);
					}
				}
			}.rehash(index);
		rehash();
	}

	public int size() {
		return size;
	}

	public DblObjSource<V> source() {
		return source_();
	}

	public DblObjStreamlet<V> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			double[] ks0 = ks;
			var vs0 = vs;
			Object o;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((o = vs0[i]) != null)
					store(ks0[i], o);
		}
	}

	private void store(double key, Object v1) {
		var index = index(key);
		if (vs[index] == null) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(double key) {
		var mask = vs.length - 1;
		var index = Double.hashCode(key) & mask;
		while (vs[index] != null && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private DblObjSource<V> source_() {
		return new DblObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblObjPair<V> pair) {
				while (index < capacity) {
					var k = ks[index];
					var v = vs[index++];
					if (v != null) {
						pair.update(k, cast(v));
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
