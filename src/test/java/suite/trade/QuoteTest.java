package suite.trade;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.TempDir;

public class QuoteTest {

	public class Record {
		public String date;
		public int buySell;
		public String stockCode;
		public float price;
		public String remark;

		public Record(String[] records) {
			this.date = records[0];
			this.buySell = Integer.parseInt(records[1]);
			this.stockCode = records[2];
			this.price = Float.parseFloat(records[3]);
			this.remark = 4 < records.length ? records[4] : null;
		}
	}

	@Test
	public void testQuote() {
		Yahoo yahoo = new Yahoo();

		List<Record> table = Read.bytes(TempDir.resolve("stock.txt")) //
				.collect(As::table) //
				.map(Record::new) //
				.toList();

		Map<String, Integer> sizeByStockCodes = Read.from(table) //
				.map2(record -> record.stockCode, record -> record.buySell) //
				.groupBy(sizes -> sizes.fold(0, (size0, size1) -> size0 + size1)) //
				.toMap();

		Map<String, Float> priceByStockCodes = yahoo.quote(Read.from(sizeByStockCodes.keySet()));

		float amount0 = Read.from(table) //
				.map(record -> record.buySell * record.price) //
				.collect(this::sum);
		float amount1 = Read.from2(sizeByStockCodes) //
				.map((stockCode, size) -> priceByStockCodes.get(stockCode) * size) //
				.collect(this::sum);

		priceByStockCodes.forEach((stockCode, price) -> System.out.println(stockCode + " := " + price));

		System.out.println("AMOUNT0 = " + amount0);
		System.out.println("AMOUNT1 = " + amount1);
	}

	private float sum(Outlet<Float> outlet) {
		Source<Float> source = outlet.source();
		Float f = source.source();
		float result = 0f;
		while ((f = source.source()) != null)
			result += f;
		return result;
	}

}
