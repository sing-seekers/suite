package suite.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Memoize {

	private enum State {
		EMPTY__, INUSE_, FLAGGED
	}

	public static <I, O> Fun<I, O> byInput(Fun<I, O> fun) {
		Map<I, O> results = new ConcurrentHashMap<>();
		return in -> results.computeIfAbsent(in, in_ -> fun.apply(in_));
	}

	public static <I, O> Fun<I, O> byInput(Class<O> clazz, Fun<I, O> fun, int size) {
		return new Fun<I, O>() {
			class R {
				State state = State.EMPTY__;
				I input;
				O output;
			}

			private Map<I, R> map = new HashMap<>();
			private R array[] = new R[size];
			private int p = 0;

			{
				for (int i = 0; i < size; i++)
					array[i] = new R();
			}

			public synchronized O apply(I in) {
				O result;
				R r = map.get(in);

				if (r == null) {
					while ((r = array[p]).state != State.FLAGGED) {
						r.state = State.INUSE_;
						p = ++p > size ? p - size : p;
					}

					if (r.state == State.INUSE_)
						map.remove(r.input);
					else
						r.state = State.INUSE_;

					r.input = in;
					r.output = result = fun.apply(in);
					map.put(in, r);
				} else {
					r.state = State.FLAGGED;
					result = r.output;
				}

				return result;
			}
		};
	}

	public static <T> Source<T> timed(Source<T> source) {
		return timed(source, 30 * 1000l);
	}

	public static <T> Source<T> timed(Source<T> source, long duration) {
		return new Source<T>() {
			private long timestamp = 0;
			private T result;

			public synchronized T source() {
				long current = System.currentTimeMillis();
				if (result == null || current > timestamp + duration) {
					timestamp = current;
					result = source.source();
				}
				return result;
			}
		};
	}

}
