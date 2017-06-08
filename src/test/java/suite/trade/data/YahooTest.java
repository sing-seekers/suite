package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.function.BiFunction;

import org.junit.Test;

import suite.trade.DatePeriod;
import suite.trade.Time;

public class YahooTest {

	private Yahoo yahoo = new Yahoo();

	@Test
	public void testCsv() {
		test(yahoo::dataSourceCsv);
	}

	@Test
	public void testL1() {
		test(yahoo::dataSourceL1);
	}

	@Test
	public void testYql() {
		test(yahoo::dataSourceYql);
	}

	private void test(BiFunction<String, DatePeriod, DataSource> fun) {
		DataSource dataSource = fun.apply("0005.HK", DatePeriod.of(Time.of(2016, 1, 1), Time.of(2017, 1, 1)));

		dataSource.validate();

		int datesLength = dataSource.dates.length;
		int pricesLength = dataSource.prices.length;
		assertTrue(datesLength == pricesLength);
		assertTrue(0 < datesLength);
	}

}
