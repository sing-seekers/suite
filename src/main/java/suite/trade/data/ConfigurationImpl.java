package suite.trade.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade;
import suite.trade.data.Broker.Hsbc;
import suite.util.String_;
import suite.util.To;

public class ConfigurationImpl implements Configuration {

	private Broker broker = new Hsbc();
	private Hkd hkd = new Hkd();
	private Hkex hkex = new Hkex();
	private HkexFactBook hkexFactBook = new HkexFactBook();
	private QuoteDatabase quoteDatabase = new QuoteDatabase();
	private Yahoo yahoo = new Yahoo();
	private YahooHistory yahooHistory = new YahooHistory();

	private enum Source_ {
		HKD__, YAHOO,
	};

	public DataSource dataSource(String symbol) {
		return dataSource_(symbol, DatePeriod.ages());
	}

	public DataSource dataSource(String symbol, DatePeriod period) {
		return dataSource_(symbol, period);
	}

	public DataSource dataSourceWithLatestQuote(String symbol) {

		// count as tomorrow open if market is closed (after 4pm)
		String date = To.string(HkexUtil.getTradeTimeAfter(LocalDateTime.now()).toLocalDate());
		DataSource dataSource0 = dataSource_(symbol, DatePeriod.ages());
		DataSource dataSource1;

		if (!String_.equals(dataSource0.last().date, date))
			dataSource1 = dataSource0.cons(date, quote_(Collections.singleton(symbol)).get(symbol));
		else
			dataSource1 = dataSource0;

		return dataSource1;
	}

	public Streamlet<Asset> queryCompanies() {
		return hkex.queryCompanies();
	}

	public Asset queryCompany(String symbol) {
		return hkex.queryCompany(symbol);
	}

	public Streamlet<Trade> queryHistory() {
		return broker.queryHistory();
	}

	public Streamlet<Asset> queryLeadingCompaniesByMarketCap(LocalDate date) {
		int year = date.getYear() - 1;
		return Read.from(hkexFactBook.queryLeadingCompaniesByMarketCap(year)).map(this::queryCompany);
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return quote_(symbols);
	}

	public double transactionFee(double transactionAmount) {
		return broker.transactionFee(transactionAmount);
	}

	private Map<String, Float> quote_(Set<String> symbols) {
		Map<Source_, Set<String>> map = new HashMap<>();
		for (String symbol : symbols)
			map.computeIfAbsent(source_(symbol), s -> new HashSet<>()).add(symbol);
		return To.map_(hkd.quote(map.getOrDefault(Source_.HKD__, Collections.emptySet())),
				yahoo.quote(map.getOrDefault(Source_.YAHOO, Collections.emptySet())));
	}

	private DataSource dataSource_(String symbol, DatePeriod period) {
		switch (source_(symbol)) {
		case HKD__:
			return hkd.dataSource(symbol, period);
		case YAHOO:
			if (yahooHistory.isContainsData(symbol))
				return yahooHistory.dataSource(symbol, period);
			else if (Boolean.TRUE)
				return yahoo.dataSourceL1(symbol, period);
			else
				return quoteDatabase.get(symbol, "o");
		default:
			throw new RuntimeException();
		}
	}

	private Source_ source_(String symbol) {
		if (String_.equals(symbol, Asset.cashSymbol))
			return Source_.HKD__;
		else
			return Source_.YAHOO;
	}

}
