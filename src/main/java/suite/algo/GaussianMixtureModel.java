package suite.algo;

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static suite.util.Streamlet_.forInt;

import java.util.List;
import java.util.Random;

import primal.MoreVerbs.Read;
import primal.primitive.FltMoreVerbs.ReadFlt;
import primal.primitive.fp.AsDbl;
import suite.inspect.Dump;
import suite.math.linalg.GaussSeidel;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.util.To;

/**
 * https://towardsdatascience.com/how-to-code-gaussian-mixture-models-from-scratch-in-python-9e7975df5252
 *
 * https://www.geeksforgeeks.org/gaussian-mixture-model/
 *
 * @author ywsing
 */
public class GaussianMixtureModel {

	private GaussSeidel gs = new GaussSeidel();
	private Matrix mtx = new Matrix();
	private Random random = new Random();
	private Vector vec = new Vector();

	public final List<GaussComponent> components;

	private double hinvpi = .5d / PI;

	public class GaussComponent {
		public float[] mean;
		public float[][] covar;
		public double scale;

		public GaussComponent(float[] mean, float[][] covar, double scale) {
			this.mean = mean;
			this.covar = covar;
			this.scale = scale;
		}
	}

	public GaussianMixtureModel(int n, float[][] obs) {
		var nObs = obs.length;
		var dim = obs[0].length;

		var comps = forInt(n).map(i -> new GaussComponent( //
				To.vector(dim, j -> random.nextGaussian()), //
				mtx.identity(dim), //
				1d / n)).toList();

		for (var iter = 0; iter < 16; iter++) {
			var comps_ = comps;

			var dets = To.vector(n, i -> mtx.det(comps_.get(i).covar));

			// expectation
			var bks = Read.from(obs).map(x -> {
				var fs = To.vector(n, k -> {
					var mvs = comps_.get(k);
					var d = vec.sub(x, mvs.mean);
					var f = sqrt(hinvpi / dets[k]) * exp(-.5d * vec.dot(d, gs.solve(mvs.covar, d)));
					return f * mvs.scale;
				});

				return vec.scaleOn(fs, 1d / ReadFlt.from(fs).sum());
			}).toArray(float[].class);

			System.out.println(iter);
			Dump.details(bks);

			// maximization
			comps = forInt(n).map(k -> {
				var bksum = Read.from(bks).toDouble(AsDbl.sum(bk -> bk[k]));
				var ibksum = 1d / bksum;
				var mean_ = comps_.get(k).mean;

				var mean1 = vec.scaleOn(forInt(nObs).fold(new float[dim], (o, sum) -> {
					return vec.addOn(sum, vec.scale(obs[o], bks[o][k]));
				}), ibksum);

				var covar1 = mtx.scaleOn(forInt(nObs).fold(new float[dim][dim], (o, sum) -> {
					var d = vec.sub(obs[o], mean_);
					return mtx.addOn(sum, mtx.scaleOn(mtx.mul(d), bks[o][k]));
				}), ibksum);

				var scale1 = bksum / nObs;

				return new GaussComponent(mean1, covar1, scale1);
			}).toList();

			Dump.details(comps);
		}

		components = comps;
	}

}
