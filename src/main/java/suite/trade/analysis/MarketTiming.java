package suite.trade.analysis;

import suite.math.stat.Statistic;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.streamlet.IntStreamlet;

public class MarketTiming {

	public final int strgBear = 1 << 4;
	public final int weakBear = 1 << 3;
	public final int rngBound = 1 << 2;
	public final int weakBull = 1 << 1;
	public final int strgBull = 1 << 0;

	private MovingAverage ma = new MovingAverage();
	private Statistic stat = new Statistic();

	public float[] hold(float[] prices, float h0, float h1, float h2) {
		int[] flags = time(prices);
		int length = flags.length;
		float[] holds = new float[length];
		float hold = 0f;

		for (int i = 0; i < length; i++) {
			if ((flags[i] & strgBear) != 0)
				hold = -h2;
			else if ((flags[i] & strgBull) != 0)
				hold = +h2;
			else if ((flags[i] & weakBear) != 0)
				hold = -h1;
			else if ((flags[i] & weakBull) != 0)
				hold = +h1;
			else
				hold = h0;
			holds[i] = hold;
		}

		return holds;
	}

	public int[] time(float[] prices) {
		int length = prices.length;
		int lookback = 40;

		float[] ma20 = ma.movingAvg(prices, 20);
		float[] ma50 = ma.movingAvg(prices, 50);
		double lookback80 = lookback * .8d;
		int[] flags = new int[length];

		for (int i = 0; i < length; i++) {
			int past = Math.max(0, i - lookback);
			IntStreamlet past_i = Ints_.range(past, i);
			IntStreamlet past1_i = past_i.drop(1);

			int ma20abovema50 = past_i.filter(j -> ma50[j] < ma20[j]).size();
			int ma50abovema20 = past_i.filter(j -> ma20[j] < ma50[j]).size();
			double r = ma50abovema20 / (double) ma20abovema50;

			boolean isStrglyBullish = true //
					&& lookback <= ma20abovema50 //
					&& past1_i.isAll(j -> ma20[j - 1] <= ma20[j]) //
					&& past1_i.isAll(j -> ma50[j - 1] <= ma50[j]) //
					&& (1.02d * ma50[i] <= ma20[i] || ma20[past] - ma50[past] < ma20[i] - ma50[i]) //
					&& past_i.isAll(j -> ma20[j] <= prices[j]);

			boolean isWeaklyBullish = true //
					&& lookback80 <= ma20abovema50 //
					&& past1_i.isAll(j -> ma50[j - 1] <= ma50[j]) //
					&& past_i.isAll(j -> ma50[j] <= prices[j]);

			boolean isStrglyBearish = true //
					&& lookback <= ma50abovema20 //
					&& past1_i.isAll(j -> ma20[j] <= ma20[j - 1]) //
					&& past1_i.isAll(j -> ma50[j] <= ma50[j - 1]) //
					&& (1.02d * ma20[i] <= ma50[i] || ma50[past] - ma20[past] < ma50[i] - ma20[i]) //
					&& past_i.isAll(j -> prices[j] <= ma20[j]);

			boolean isWeaklyBearish = true //
					&& lookback80 <= ma50abovema20 //
					&& past1_i.isAll(j -> ma50[j] <= ma50[j - 1]) //
					&& past_i.isAll(j -> prices[j] <= ma50[j]);

			boolean isRangeBound__ = true // non-trending
					&& 2d / 3d <= r && r <= 3d / 2d //
					&& stat.meanVariance(past_i.collect(Int_Flt.lift(j -> ma50[j])).toArray()).volatility() < .02d //
					&& .02d < stat.meanVariance(past_i.collect(Int_Flt.lift(j -> ma20[j])).toArray()).volatility() //
					&& (ma20[i] + ma50[i]) * .02d <= Math.abs(ma20[i] - ma50[i]);

			int flag = 0 //
					+ (isStrglyBearish ? strgBear : 0) //
					+ (isWeaklyBearish ? weakBear : 0) //
					+ (isRangeBound__ ? rngBound : 0) //
					+ (isWeaklyBullish ? weakBull : 0) //
					+ (isStrglyBullish ? strgBull : 0);

			flags[i] = flag;
		}

		return flags;
	}

}
