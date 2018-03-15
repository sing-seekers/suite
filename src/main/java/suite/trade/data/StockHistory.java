package suite.trade.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.LngFltPair;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.DataSource.Datum;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.BinOp;
import suite.util.Object_;
import suite.util.Set_;
import suite.util.String_;

public class StockHistory {

	private static Cleanse cleanse = new Cleanse();

	public final String exchange;
	public final Time time;
	public final boolean isActive;
	public final Map<String, LngFltPair[]> data; // un-adjusted
	public final LngFltPair[] dividends;
	public final LngFltPair[] splits;

	public static StockHistory of(Outlet<String> outlet) {
		Map<String, String> properties = new HashMap<>();
		Map<String, LngFltPair[]> data = new HashMap<>();
		String line;

		while ('9' < (line = outlet.next()).charAt(0)) {
			String[] array = line.split("=");
			properties.put(array[0].trim(), array[1].trim());
		}

		String exchange = properties.get("exchange");
		String timeZoneStr = properties.get("timeZone");
		String isActive = properties.get("isActive");

		int timeZone = timeZoneStr != null ? Integer.parseInt(timeZoneStr) : 0;
		LngFltPair[] dividends = readPairs(timeZone, outlet);
		LngFltPair[] splits = readPairs(timeZone, outlet);
		String tag;

		while ((tag = outlet.next()) != null)
			data.put(tag, readPairs(timeZone, outlet));

		return of( //
				exchange, //
				Time.ofYmdHms(line), //
				String_.equals(isActive, "Y"), //
				data, //
				dividends, //
				splits);
	}

	private static LngFltPair[] readPairs(int timeZone, Outlet<String> outlet) {
		List<LngFltPair> pairs = new ArrayList<>();
		String line;

		if (String_.equals(line = outlet.next(), "{"))
			while (!String_.equals(line = outlet.next(), "}")) {
				int p = line.lastIndexOf(":");
				Time time = Time.of(line.substring(0, p));
				float price = Float.parseFloat(line.substring(p + 1));
				pairs.add(LngFltPair.of(time.epochSec(timeZone), price));
			}
		else
			Fail.t();

		return pairs.toArray(new LngFltPair[0]);
	}

	public static StockHistory new_() {
		return of(null, TimeRange.min, true, new HashMap<>(), new LngFltPair[0], new LngFltPair[0]);
	}

	public static StockHistory of(//
			String exchange, //
			Time time, //
			boolean isActive, //
			Map<String, LngFltPair[]> data, //
			LngFltPair[] dividends, //
			LngFltPair[] splits) {
		return new StockHistory(exchange, time, isActive, data, dividends, splits);
	}

	private StockHistory( //
			String exchange, //
			Time time, //
			boolean isActive, //
			Map<String, LngFltPair[]> data, //
			LngFltPair[] dividends, //
			LngFltPair[] splits) {
		this.exchange = exchange;
		this.time = time;
		this.isActive = isActive;
		this.data = data;
		this.dividends = dividends;
		this.splits = splits;
	}

	public LngFltPair[] get(String tag) {
		return data.getOrDefault(tag, new LngFltPair[0]);
	}

	public StockHistory cleanse() {
		Map<String, LngFltPair[]> data_ = Read //
				.from2(data) //
				.map2((name, pairs) -> {
					if (!String_.equals(name, "volume"))
						return cleanse.cleanse(pairs);
					else
						return pairs;
				}) //
				.toMap();

		return create(data_, dividends, splits);
	}

	public StockHistory filter(TimeRange period) {
		long t0 = period.from.epochSec();
		long tx = period.to.epochSec();
		Iterate<LngFltPair[]> filter_ = pairs0 -> {
			List<LngFltPair> pairs1 = new ArrayList<>();
			for (LngFltPair pair : pairs0)
				if (t0 <= pair.t0 && pair.t0 < tx)
					pairs1.add(pair);
			return pairs1.toArray(new LngFltPair[0]);
		};

		Map<String, LngFltPair[]> data1 = Read.from2(data) //
				.mapValue(filter_) //
				.toMap();

		return create(data1, filter_.apply(dividends), filter_.apply(splits));
	}

	public StockHistory merge(StockHistory other) {
		boolean isActive_ = isActive && other.isActive;
		Set<String> keys = Set_.union(data.keySet(), other.data.keySet());

		BinOp<LngFltPair[]> merge_ = (pairs0, pairs1) -> {
			List<LngFltPair> pairs = new ArrayList<>();
			int length1 = pairs1.length;
			int i1 = 0;
			for (LngFltPair pair0 : pairs0) {
				long l0 = pair0.t0;
				while (i1 < length1) {
					LngFltPair pair1 = pairs1[i1];
					long l1 = pair1.t0;
					if (l1 < l0)
						pairs.add(pair1);
					else if (l0 < l1)
						break;
					i1++;
				}
				pairs.add(pair0);
			}
			while (i1 < length1)
				pairs.add(pairs1[i1++]);
			return pairs.toArray(new LngFltPair[0]);
		};

		Map<String, LngFltPair[]> data1 = Read.from(keys) //
				.map2(key -> merge_.apply(get(key), other.get(key))) //
				.toMap();

		return create(isActive_, data1, merge_.apply(dividends, other.dividends), merge_.apply(splits, other.splits));
	}

	public StockHistory alignToDate() {
		Iterate<LngFltPair[]> align_ = pairs0 -> {
			List<LngFltPair> pairs1 = new ArrayList<>();
			Time date = TimeRange.min;
			for (LngFltPair pair : pairs0) {
				Time date1 = Time.ofEpochSec(pair.t0).startOfDay();
				if (Object_.compare(date, date1) < 0)
					pairs1.add(pair);
				date = date1;
			}
			return pairs1.toArray(new LngFltPair[0]);
		};

		Map<String, LngFltPair[]> data1 = Read.from2(data) //
				.mapValue(align_) //
				.toMap();

		return create(data1, align_.apply(dividends), align_.apply(splits));
	}

	public DataSource toDataSource() {
		LngFltPair[] opPairs = adjustPrices("open");
		LngFltPair[] clPairs = adjustPrices("close");
		LngFltPair[] loPairs = adjustPrices("low");
		LngFltPair[] hiPairs = adjustPrices("high");
		LngFltPair[] vlPairs = data.get("volume");
		LngFltPair[] ps = clPairs;
		int length = ps.length;

		Datum[] data = new Datum[length];
		int io = 0, ic = 0, il = 0, ih = 0, iv = 0;

		for (int i = 0; i < length; i++) {
			long t = ps[i].t0;
			int io_ = io, il_ = il, ih_ = ih, iv_ = iv;

			io = scan(opPairs, io, t);
			ic = scan(clPairs, ic, t);

			data[i] = new Datum( //
					t, //
					t + DataSource.tickDuration, //
					opPairs[io_].t1, //
					clPairs[ic - 1].t1, //
					Ints_.range(il_, il = scan(loPairs, il_, t)).collect(Int_Flt.lift(i_ -> loPairs[i_].t1)).min(), //
					Ints_.range(ih_, ih = scan(hiPairs, ih_, t)).collect(Int_Flt.lift(i_ -> hiPairs[i_].t1)).max(), //
					Ints_.range(iv_, iv = scan(vlPairs, iv_, t)).collect(Int_Flt.lift(i_ -> vlPairs[i_].t1)).sum());
		}

		return DataSource.of(Read.from(data));
	}

	private int scan(LngFltPair[] pairs, int i, long t) {
		int length = pairs.length;
		while (i < length && pairs[i].t0 <= t)
			i++;
		return i;
	}

	public String write() {
		Streamlet<String> s0 = Read.each( //
				"exchange = " + exchange, //
				"timeZone = 8", //
				time.ymdHms());
		Streamlet<String> s1 = Read.each(dividends, splits).concatMap(this::concat);
		Streamlet<String> s2 = Read.from2(data).concatMap((tag, fs) -> concat(fs).cons(tag));
		return Streamlet //
				.concat(s0, s1, s2) //
				.collect(As.joinedBy("\n"));
	}

	private Streamlet<String> concat(LngFltPair[] pairs) {
		return Streamlet.concat( //
				Read.each("{"), //
				Read.from(pairs).map(pair -> Time.ofEpochSec(pair.t0).ymdHms() + ":" + pair.t1), //
				Read.each("}"));
	}

	private LngFltPair[] adjustPrices(String tag) {
		LngFltPair[] pairs0 = data.get(tag);
		int length = pairs0.length;
		LngFltPair[] pairs1 = new LngFltPair[length];

		int si = splits.length - 1;
		int di = dividends.length - 1;
		float a = 0f, b = 1f;

		for (int i = length - 1; 0 <= i; i--) {
			LngFltPair pair = pairs0[i];
			long t = pair.t0;

			if (0 <= di) {
				LngFltPair dividend = dividends[di];
				if (t < dividend.t0) {
					if (Boolean.TRUE)
						// may got negative prices
						a -= dividend.t1 * b;
					else
						// may got skewed profits
						b *= (pair.t0 - dividend.t0) / pair.t0;
					di--;
				}
			}

			if (0 <= si) {
				LngFltPair split = splits[si];
				if (t < split.t0) {
					b *= split.t1;
					si--;
				}
			}

			pairs1[i] = LngFltPair.of(pair.t0, a + b * pair.t1);
		}

		return pairs1;
	}

	public StockHistory create(Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		return create(isActive, data, dividends, splits);
	}

	private StockHistory create(boolean isActive, Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		return of(null, time, isActive, data, dividends, splits);
	}

}
