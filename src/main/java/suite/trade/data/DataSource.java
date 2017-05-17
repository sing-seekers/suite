package suite.trade.data;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

import suite.math.Matrix;
import suite.primitive.DataInput_;
import suite.primitive.DataOutput_;
import suite.trade.DatePeriod;
import suite.trade.assetalloc.AssetAllocator;
import suite.util.FormatUtil;
import suite.util.Object_;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.Util;

public class DataSource {

	public static Matrix mtx = new Matrix();

	public static Serializer<DataSource> serializer = new Serializer<DataSource>() {
		private Serializer<String[]> sas = Serialize.array(String.class, Serialize.string(10));
		private Serializer<float[]> fas = Serialize.arrayOfFloats;

		public DataSource read(DataInput_ dataInput) throws IOException {
			String[] dates = sas.read(dataInput);
			float[] prices = fas.read(dataInput);
			return new DataSource(dates, prices);
		}

		public void write(DataOutput_ dataOutput, DataSource dataSource) throws IOException {
			sas.write(dataOutput, dataSource.dates);
			fas.write(dataOutput, dataSource.prices);
		}
	};

	public final String[] dates;
	public final float[] prices;

	public class Datum {
		public final String date;
		public final float price;

		private Datum(String date, float price) {
			this.date = date;
			this.price = price;
		}
	}

	public DataSource(String[] dates, float[] prices) {
		this.dates = dates;
		this.prices = prices;
	}

	public DataSource cons(String date, float price) {
		String[] dates1 = Util.add(String.class, dates, new String[] { date, });
		float[] prices1 = mtx.concat(prices, new float[] { price, });
		return new DataSource(dates1, prices1);

	}

	public void cleanse() {

		// ignore price sparks caused by data source bugs
		for (int i = 2; i < prices.length; i++) {
			float price0 = prices[i - 2];
			float price1 = prices[i - 1];
			float price2 = prices[i - 0];
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2))
				prices[i - 1] = price0;
		}
	}

	public DataSource after(LocalDate date) {
		return range_(DatePeriod.of(date, DatePeriod.ages().to));
	}

	public DataSource range(DatePeriod period) {
		return range_(period);
	}

	public DataSource rangeBefore(LocalDate date) {
		return range_(DatePeriod.daysBefore(date, AssetAllocator.historyWindow));
	}

	public void validate() {
		int length = prices.length;
		String date0 = dates[0];
		float price0 = prices[0];

		if (length != dates.length)
			throw new RuntimeException("mismatched dates and prices");

		for (int i = 1; i < length; i++) {
			String date1 = dates[i];
			float price1 = prices[i];

			if (0 <= date0.compareTo(date1))
				throw new RuntimeException("wrong date order: " + date0 + "/" + date1);

			if (price1 == 0f)
				throw new RuntimeException("price is zero: " + price1 + "/" + date1);

			if (!Float.isFinite(price1))
				throw new RuntimeException("price is not finite: " + price1 + "/" + date1);

			if (!isValid(price0, price1))
				throw new RuntimeException("price varied too much: " + price0 + "/" + date0 + " => " + price1 + "/" + date1);

			date0 = date1;
			price0 = price1;
		}
	}

	public double nYears() { // approximately
		LocalDate date0 = FormatUtil.date(first().date);
		LocalDate datex = FormatUtil.date(last().date);
		return DatePeriod.of(date0, datex).nYears();
	}

	public Datum first() {
		return get(0);
	}

	public Datum last() {
		return get(-1);
	}

	public Datum get(int pos) {
		if (pos < 0)
			pos += prices.length;
		return new Datum(dates[pos], prices[pos]);
	}

	private DataSource range_(DatePeriod period) {
		String s0 = FormatUtil.formatDate(period.from);
		String sx = FormatUtil.formatDate(period.to);
		String[] dates1 = new String[dates.length];
		float[] prices1 = new float[prices.length];
		int j = 0;

		for (int i = 0; i < prices.length; i++) {
			String date = dates[i];
			float price = prices[i];
			if (Object_.compare(s0, date) <= 0 && Object_.compare(date, sx) < 0) {
				dates1[j] = date;
				prices1[j] = price;
				j++;
			}
		}

		return new DataSource(Arrays.copyOf(dates1, j), Arrays.copyOf(prices1, j));
	}

	private boolean isValid(float price0, float price1) {
		float ratio = price1 / price0;
		return 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
