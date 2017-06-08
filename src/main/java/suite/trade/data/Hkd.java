package suite.trade.data;

import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.util.String_;

public class Hkd {

	public DataSource dataSource(String symbol, DatePeriod period) {
		if (String_.equals(symbol, Asset.cashSymbol))
			return new DataSource(new String[] { period.to.ymd(), }, new float[] { 1f, });
		else
			throw new RuntimeException();
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return Read.from(symbols) //
				.map2(symbol -> {
					if (String_.equals(symbol, Asset.cashSymbol))
						return 1f;
					else
						throw new RuntimeException();
				}) //
				.toMap();
	}

}
