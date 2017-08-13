package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.DblFunUtil;
import suite.primitive.DblLngSink;
import suite.primitive.DblLngSource;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Lng;
import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.DblLngPair;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblObjOutlet;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive double key and primitive long value. Long.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblLngMap {

	private int size;
	private double[] ks;
	private long[] vs;

	public static <T> Fun<Outlet<T>, DblLngMap> collect(Obj_Dbl<T> kf0, Obj_Lng<T> vf0) {
		Obj_Dbl<T> kf1 = kf0.rethrow();
		Obj_Lng<T> vf1 = vf0.rethrow();
		return outlet -> {
			DblLngMap map = new DblLngMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public DblLngMap() {
		this(8);
	}

	public DblLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(double key, Dbl_Lng fun) {
		long v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(DblLngSink sink) {
		DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
		DblLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(double key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(double key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public int size() {
		return size;
	}

	public DblLngSource source() {
		return source_();
	}

	public DblObjStreamlet<Long> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(new DblObjSource<Long>() {
			private DblLngSource source0 = source_();
			private DblLngPair pair0 = DblLngPair.of((double) 0, (long) 0);

			public boolean source2(DblObjPair<Long> pair) {
				boolean b = source0.source2(pair0);
				pair.t0 = pair0.t0;
				pair.t1 = pair0.t1;
				return b;
			}
		}));
	}

	private long put_(double key, long v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblLngSource source_() {
		return new DblLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblLngPair pair) {
				long v;
				while ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
