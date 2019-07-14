package suite.math.linalg;

import static suite.util.Friends.abs;
import static suite.util.Friends.forInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.adt.pair.Pair;
import suite.primitive.Int_Dbl;
import suite.primitive.adt.pair.DblObjPair;
import suite.util.To;

public class Eigen {

	private Matrix mtx = new Matrix();
	private Random random = new Random();
	private Vector vec = new Vector();

	// Machine Learning - An Algorithm Perspective
	// 6.2 Principal Components Analysis
	public float[] pca(float[][] m0) {
		var m1 = mtx.copyOf(m0);
		var height = mtx.height(m1);
		var width_ = mtx.width(m1);

		for (var j = 0; j < width_; j++) {
			var j_ = j;
			var mean = forInt(height).toDouble(Int_Dbl.sum(i -> m1[i][j_])) / height;
			for (var i = 0; i < height; i++)
				m1[i][j_] -= mean;
		}

		var cov = mtx.scale(mtx.mul_mTn(m1, m1), 1d / height);
		return powerIteration(cov).t1;
		// var evs = eigen.power(cov);
		// return eigen.values(cov, evs);
	}

	// Paul Wilmott on Quantitative Finance, Second Edition
	// 37.13.1 The Power Method, page 620
	// symmetric positive definite matrices only (e.g. covariance matrix)
	public List<DblObjPair<float[]>> power0(float[][] m0) {
		var m = mtx.copyOf(m0);
		var size = mtx.sqSize(m);
		var pairs = new ArrayList<DblObjPair<float[]>>();

		for (var v = 0; v < size; v++) {
			var pair = powerIteration(m);
			pairs.add(pair);
			var eigenValue = pair.t0;

			for (var i = 0; i < size; i++)
				m[i][i] -= eigenValue;
		}

		return pairs;
	}

	// http://macs.citadel.edu/chenm/344.dir/08.dir/lect4_2.pdf
	public List<DblObjPair<float[]>> power1(float[][] a) {
		var b = mtx.copyOf(a);
		var size = a.length;

		var rprev = 0d;
		var uprev = new float[size];
		var xprev = new float[size];
		var pairs = new ArrayList<DblObjPair<float[]>>();

		for (var i = 0; i < size; i++) {
			var pair = powerIteration(b);
			var r = pair.t0;
			var u = pair.t1;
			var x = vec.scale(b[i], 1d / (r * u[i]));
			var v = vec.addOn(vec.scale(u, r - rprev), vec.scale(uprev, rprev * vec.dot(xprev, u)));
			b = mtx.subOn(b, mtx.scaleOn(mtx.mul(u, x), r));
			rprev = r;
			uprev = u;
			xprev = x;
			pairs.add(DblObjPair.of(r, v));
		}

		return pairs;
	}

	private DblObjPair<float[]> powerIteration(float[][] m) {
		var size = mtx.sqSize(m);
		var xs = To.vector(size, i -> random.nextFloat());
		var maxy = Double.NaN;

		for (var iter = 0; iter < 512; iter++) {
			var ys = mtx.mul(m, xs);
			maxy = 0f;
			for (var y : ys)
				if (abs(maxy) < abs(y))
					maxy = y;
			xs = vec.scale(ys, 1d / maxy);
		}

		// xs = vec.normalizeOn(xs);
		return DblObjPair.of(vec.dot(xs, mtx.mul(m, xs)) / vec.dot(xs), xs);
	}

	// https://en.wikipedia.org/wiki/Lanczos_algorithm
	// returns V and T, where m ~= V T V*
	public Pair<float[][], float[][]> lanczos(float[][] m) {
		var n = mtx.sqSize(m);
		var nIterations = 20; // n
		var alphas = new float[nIterations];
		var betas = new float[nIterations];
		var vs = new float[nIterations][];
		var ws = new float[nIterations][];
		float[] vj1 = null;

		for (var j = 1; j < nIterations; j++) {
			var beta = 0d;
			float[] prevw;
			float[] vj;

			if (0 < j && (beta = vec.dot(prevw = ws[j - 1])) != 0d)
				vj = vec.scale(prevw, 1d / (betas[j] = (float) beta));
			else
				vj = vec.normalizeOn(To.vector(n, i -> random.nextFloat()));

			var wp = mtx.mul(m, vs[j] = vj);
			var sub0 = vec.scale(vj, alphas[0] = (float) vec.dot(wp, vj));
			var sub1 = 0 < j ? vec.add(sub0, vec.scale(vj1, beta)) : sub0;

			vj1 = vj;
			ws[j] = vec.sub(wp, sub1);
		}

		var t = new float[nIterations][nIterations];

		for (var i = 0; i < nIterations; i++)
			t[i][i] = alphas[i];
		for (var i = 1; i < nIterations; i++)
			t[i - 1][i] = t[i][i - 1] = betas[i];

		return Pair.of(mtx.transpose(vs), t);
	}

}
