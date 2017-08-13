package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.FltFunUtil;
import suite.primitive.FltLngSink;
import suite.primitive.FltLngSource;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Lng;
import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.FltLngPair;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive float key and primitive long value. Long.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class FltLngMap {

	private int size;
	private float[] ks;
	private long[] vs;

	public static <T> Fun<Outlet<T>, FltLngMap> collect(Obj_Flt<T> kf0, Obj_Lng<T> vf0) {
		Obj_Flt<T> kf1 = kf0.rethrow();
		Obj_Lng<T> vf1 = vf0.rethrow();
		return outlet -> {
			FltLngMap map = new FltLngMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public FltLngMap() {
		this(8);
	}

	public FltLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(float key, Flt_Lng fun) {
		long v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(FltLngSink sink) {
		FltLngPair pair = FltLngPair.of((float) 0, (long) 0);
		FltLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(float key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			float[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(float key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public int size() {
		return size;
	}

	public FltLngSource source() {
		return source_();
	}

	public FltObjStreamlet<Long> streamlet() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(new FltObjSource<Long>() {
			private FltLngSource source0 = source_();
			private FltLngPair pair0 = FltLngPair.of((float) 0, (long) 0);

			public boolean source2(FltObjPair<Long> pair) {
				boolean b = source0.source2(pair0);
				pair.t0 = pair0.t0;
				pair.t1 = pair0.t1;
				return b;
			}
		}));
	}

	private long put_(float key, long v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltLngSource source_() {
		return new FltLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltLngPair pair) {
				long v;
				while ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new float[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
