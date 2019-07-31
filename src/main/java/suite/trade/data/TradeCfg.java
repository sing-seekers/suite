package suite.trade.data;

import java.util.Map;
import java.util.Set;

import primal.os.Log_;
import suite.streamlet.Streamlet;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.data.DataSource.AlignKeyDataSource;

public interface TradeCfg {

	public DataSource dataSource(String symbol);

	public DataSource dataSource(String symbol, TimeRange period);

	public Streamlet<Instrument> queryCompanies();

	public Streamlet<Instrument> queryCompaniesByMarketCap(Time time);

	public Instrument queryCompany(String symbol);

	public Streamlet<Trade> queryHistory();

	public Map<String, Float> quote(Set<String> symbols);

	public double transactionFee(double transactionAmount);

	public default AlignKeyDataSource<String> dataSources(TimeRange period, Streamlet<String> symbols) {
		return symbols //
				.map2(symbol -> {
					try {
						return dataSource(symbol, period).validate();
					} catch (Exception ex) {
						Log_.warn("for " + symbol + " " + ex);
						return null;
					}
				}) //
				.filterValue(ds -> ds != null) //
				.collect() //
				.apply(DataSource::alignAll);
	}

}
