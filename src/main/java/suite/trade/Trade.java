package suite.trade;

import static suite.util.Friends.abs;

import suite.math.MathUtil;
import suite.util.String_;
import suite.util.To;

public class Trade {

	public static String NA = "-";

	public final String date;
	public final int buySell;
	public final String symbol;
	public final float price;
	public final String strategy;
	public final String remark;

	public static Trade of(String[] array) {
		return new Trade(array[0], Integer.parseInt(array[1]), array[2], Float.parseFloat(array[3]), array[4]);
	}

	public static Trade of(int buySell, String symbol, float price) {
		return of(NA, buySell, symbol, price, "-");
	}

	public static Trade of(String date, int buySell, String symbol, float price, String strategy) {
		return new Trade(date, buySell, symbol, price, strategy);
	}

	private Trade(String date, int buySell, String symbol, float price, String strategy) {
		String date_, remark_;

		if (date.endsWith("#")) {
			date_ = date.substring(0, date.length() - 1);
			remark_ = "#";
		} else {
			date_ = date;
			remark_ = "-";
		}

		this.date = date_;
		this.buySell = buySell;
		this.symbol = symbol;
		this.price = price;
		this.strategy = strategy;
		this.remark = remark_;
	}

	public String record() {
		return date //
				+ (!String_.equals(remark, "-") ? remark : "") //
				+ "\t" + buySell //
				+ "\t" + symbol //
				+ "\t" + price //
				+ "\t" + strategy;
	}

	public float amount() {
		return buySell * price;
	}

	@Override
	public String toString() {
		return (!String_.equals(date, NA) ? date + " " : "") //
				+ MathUtil.posNeg(buySell) //
				+ symbol //
				+ ":" + To.string(price) + "*" + abs(buySell);
	}

}
