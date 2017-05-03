package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.math.MathUtil;
import suite.os.LogUtil;
import suite.os.SerializedStoreCache;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.BackTest;
import suite.trade.DataSource;
import suite.trade.DatePeriod;
import suite.trade.Hkex;
import suite.trade.Portfolio;
import suite.trade.Strategos;
import suite.trade.Strategy;
import suite.trade.Yahoo;
import suite.util.FormatUtil;
import suite.util.Serialize;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		String result = mamrp();

		SmtpSslGmail smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);

		return true;
	}

	private String mamrp() {
		new Portfolio().simulate(1000000f, date -> LocalDate.now().equals(date));
		return "";
	}

	@SuppressWarnings("unused")
	private String mamr() {
		Hkex hkex = new Hkex();
		Yahoo yahoo = new Yahoo();
		Streamlet<Asset> assets = hkex.getCompanies();
		Strategy strategy = new Strategos().movingAvgMeanReverting(64, 8, .15f);

		LogUtil.info("S1 perform back test");

		// identify stocks that are mean-reverting
		Map<String, Boolean> backTestByStockCode = SerializedStoreCache //
				.of(Serialize.mapOfString(Serialize.boolean_)) //
				.get(getClass().getSimpleName() + ".backTestByStockCode", () -> assets //
						.map2(stock -> stock.code, stock -> {
							try {
								DatePeriod period = DatePeriod.threeYears();
								DataSource ds0 = yahoo.dataSource(stock.code, period);
								DataSource ds1 = ds0.limit(period);

								ds1.validateTwoYears();
								BackTest backTest = BackTest.test(ds1, strategy);
								return MathUtil.isPositive(backTest.account.cash());
							} catch (Exception ex) {
								LogUtil.warn(ex.getMessage() + " for " + stock);
								return false;
							}
						}) //
						.toMap());

		LogUtil.info("S2 query lot sizes");

		Map<String, Integer> lotSizeByStockCode = hkex.queryLotSizeByStockCode(assets);

		DatePeriod period = DatePeriod.daysBefore(128);
		String sevenDaysAgo = FormatUtil.formatDate(LocalDate.now().plusDays(-7));
		List<String> messages = new ArrayList<>();

		LogUtil.info("S3 capture signals");

		for (Asset asset : assets) {
			String stockCode = asset.code;

			if (backTestByStockCode.get(stockCode)) {
				String prefix = asset.toString();
				int lotSize = lotSizeByStockCode.get(stockCode);

				try {
					DataSource ds0 = yahoo.dataSource(stockCode, period);
					String datex = ds0.get(-1).date;

					if (0 <= datex.compareTo(sevenDaysAgo))
						ds0.validate();
					else
						throw new RuntimeException("ancient data: " + datex);

					Map<String, Float> latest = yahoo.quote(Read.each(stockCode));
					String latestDate = FormatUtil.formatDate(LocalDate.now());
					float latestPrice = latest.values().iterator().next();

					DataSource ds1 = ds0.cons(latestDate, latestPrice);
					float[] prices = ds1.prices;

					int last = prices.length - 1;
					int signal = strategy.analyze(prices).get(last);
					String message = asset + " has signal " + prices[last] + " * " + signal * lotSize;

					if (signal != 0) {
						LogUtil.info(message);
						messages.add(message);
					}
				} catch (Exception ex) {
					LogUtil.warn(ex.getMessage() + " in " + prefix);
				}
			}
		}

		String result = Read.from(messages).collect(As.joined("\n"));
		return result;
	}

}
