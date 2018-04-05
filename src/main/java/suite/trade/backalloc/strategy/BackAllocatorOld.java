package suite.trade.backalloc.strategy;

import static suite.util.Friends.max;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.MeanVariance;
import suite.streamlet.Streamlet2;
import suite.trade.analysis.MovingAverage;
import suite.trade.analysis.MovingAverage.MovingRange;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.util.String_;
import ts.BollingerBands;
import ts.Quant;
import ts.TimeSeries;

public class BackAllocatorOld {

	public static BackAllocatorOld me = new BackAllocatorOld();

	private BollingerBands bb = new BollingerBands();
	private MovingAverage ma = new MovingAverage();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private BackAllocatorOld() {
	}

	public BackAllocator bbSlope() {
		return BackAllocator_.byPrices(prices -> {
			float[] sds = bb.bb(prices, 32, 0, 2f).sds;
			float[] ma_ = ma.movingAvg(sds, 6);
			float[] diffs = ts.differences(3, ma_);

			return index -> {
				var last = index - 1;
				var sd = ma_[last];
				var diff = diffs[last];
				if (sd < -.3d && .015d < diff)
					return 1d;
				else if (.3d < sd && diff < -.015d)
					return -1d;
				else
					return 0d;
			};
		});
	}

	public BackAllocator bbVariable() {
		return BackAllocator_.byPrices(prices -> Quant.filterRange(1, index -> {
			var last = index - 1;
			var hold = 0d;

			for (var window = 1; hold == 0d && window < 256; window++) {
				var price = prices[last];
				MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(prices, last - window, last));
				var mean = mv.mean;
				var diff = 3d * mv.standardDeviation();

				if (price < mean - diff)
					hold = 1d;
				else if (mean + diff < price)
					hold = -1d;
			}

			return hold;
		}));
	}

	public BackAllocator movingAvgMedian() {
		var windowSize0 = 4;
		var windowSize1 = 12;

		return BackAllocator_.byPrices(prices -> {
			float[] movingAvgs0 = ma.movingAvg(prices, windowSize0);
			float[] movingAvgs1 = ma.movingAvg(prices, windowSize1);
			MovingRange[] movingRanges0 = ma.movingRange(prices, windowSize0);
			MovingRange[] movingRanges1 = ma.movingRange(prices, windowSize1);

			return index -> {
				var last = index - 1;
				var movingAvg0 = movingAvgs0[last];
				var movingAvg1 = movingAvgs1[last];
				var movingMedian0 = movingRanges0[last].median;
				var movingMedian1 = movingRanges1[last].median;
				int sign0 = Quant.sign(movingAvg0, movingMedian0);
				int sign1 = Quant.sign(movingAvg1, movingMedian1);
				return sign0 == sign1 ? (double) sign0 : 0d;
			};
		});
	}

	public BackAllocator movingMedianMeanRevn() {
		var windowSize0 = 1;
		var windowSize1 = 32;

		return BackAllocator_.byPrices(prices -> {
			MovingRange[] movingRanges0 = ma.movingRange(prices, windowSize0);
			MovingRange[] movingRanges1 = ma.movingRange(prices, windowSize1);

			return index -> {
				var last = index - 1;
				return Quant.return_(movingRanges0[last].median, movingRanges1[last].median);
			};
		});
	}

	public BackAllocator pairs(Configuration cfg, String symbol0, String symbol1) {
		return BackAllocatorGeneral.me.rsi //
				.relativeToIndex(cfg, symbol0) //
				.filterByAsset(symbol -> String_.equals(symbol, symbol1));
	}

	public BackAllocator questoQuella(String symbol0, String symbol1) {
		var tor = 64;
		var threshold = 0d;

		BackAllocator ba0 = (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();
			DataSource ds0 = dsBySymbol_.get(symbol0);
			DataSource ds1 = dsBySymbol_.get(symbol1);

			return index -> {
				var ix = index - 1;
				var i0 = ix - tor;
				double p0 = ds0.get(i0).t1, px = ds0.get(ix).t1;
				double q0 = ds1.get(i0).t1, qx = ds1.get(ix).t1;
				double pdiff = Quant.return_(p0, px);
				double qdiff = Quant.return_(q0, qx);

				if (threshold < Math.abs(pdiff - qdiff))
					return List.of( //
							Pair.of(pdiff < qdiff ? symbol0 : symbol1, 1d), //
							Pair.of(pdiff < qdiff ? symbol1 : symbol0, -1d));
				else
					return List.of();
			};
		};

		return ba0.filterByAsset(symbol -> String_.equals(symbol, symbol0) || String_.equals(symbol, symbol1));
	}

	// reverse draw-down; trendy strategy
	public BackAllocator revDrawdown() {
		return BackAllocator_.byPrices(prices -> index -> {
			var i = index - 1;
			int i0 = max(0, i - 128);
			var ix = i;
			var dir = 0;

			var lastPrice = prices[ix];
			var priceo = lastPrice;
			var io = i;

			for (; i0 <= i; i--) {
				var price = prices[i];
				int dir1 = Quant.sign(price, lastPrice);

				if (dir != 0 && dir != dir1) {
					var r = (index - io) / (double) (index - i);
					return .36d < r ? Quant.return_(priceo, lastPrice) * r * 4d : 0d;
				} else
					dir = dir1;

				if (Quant.sign(price, priceo) == dir) {
					priceo = price;
					io = i;
				}
			}

			return 0d;
		});
	}

}
