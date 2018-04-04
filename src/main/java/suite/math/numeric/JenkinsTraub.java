package suite.math.numeric;

import java.util.Arrays;
import java.util.Random;

import suite.math.Complex;
import suite.primitive.Floats_;
import suite.util.Fail;
import suite.util.To;

public class JenkinsTraub {

	private double epsilon = .00001d;
	private double epsilon2 = epsilon * epsilon;

	private Random random = new Random();

	public Complex jt(Complex[] poly0) {
		Complex inv0 = poly0[0].inverse();
		Complex[] poly1 = To.array(poly0.length, Complex.class, i -> Complex.mul(poly0[i], inv0));
		if (poly0[0].abs2() != 0d)
			return jt_(poly1);
		else
			return Complex.zero;
	}

	private Complex jt_(Complex[] poly) {
		var length = poly.length;
		Complex[] h = d(poly);

		// stage 1 no-shift process
		for (int i = 0; i < 5; i++)
			h = shift(poly, h, Complex.zero);

		// stage 2 fixed-shift process
		float[] equation = To.vector(length, i -> Math.sqrt(poly[length - 1 - i].abs2()));
		equation[0] = -equation[0];

		double root = newtonRaphson(equation, 0f);
		var n = 0;
		var nSteps = 0;
		var maxIterations = 9;

		re: while (true) {
			if (21 < ++n)
				Fail.t();
			else if (3 < ++nSteps) {
				nSteps = 0;
				maxIterations *= 2;
			}

			Complex s = Complex.expi(2d * Math.PI * random.nextDouble()).scale(root);
			Complex ph;

			Complex ar0 = Complex.sub(s, ph = ph(poly, h, s));
			h = shift(poly, h, s, ph);
			Complex ar1 = Complex.sub(s, ph = ph(poly, h, s));
			h = shift(poly, h, s, ph);
			Complex ar2 = Complex.sub(s, ph = ph(poly, h, s));
			{
				var i = 3;
				while (!(Complex.sub(ar0, ar1).abs2() <= .25d * ar0.abs2() && Complex.sub(ar1, ar2).abs2() <= .25d * ar1.abs2())) {
					h = shift(poly, h, s, ph);
					ar0 = ar1;
					ar1 = ar2;
					ar2 = Complex.sub(s, ph = ph(poly, h, s));
					if (maxIterations < ++i)
						continue re;
				}
			}

			// stage 3 variable-shift process
			Complex ar0_ = ar2;
			h = shift(poly, h, ar0_, ph);
			Complex ar1_ = Complex.sub(ar0_, ph = ph(poly, h, ar0_));
			{
				var i = 2;
				while (!(Complex.sub(ar0_, ar1_).abs2() <= epsilon2 * ar0_.abs2())) {
					h = shift(poly, h, ar1_, ph);
					ar0_ = ar1_;
					ar1_ = Complex.sub(ar0_, ph = ph(poly, h, ar0_));
					if (maxIterations < ++i)
						continue re;
				}
				return ar1_;
			}
		}
	}

	private double newtonRaphson(float[] poly, double x) {
		float[] d = d(poly);
		double diff;
		do
			x = x - (diff = evaluate(poly, x) / evaluate(d, x));
		while (Math.abs(x * epsilon) < Math.abs(diff));
		return x;
	}

	private Complex[] shift(Complex[] poly, Complex[] h, Complex s) {
		return shift(poly, h, s, ph(poly, h, s));
	}

	private Complex[] shift(Complex[] poly, Complex[] h, Complex s, Complex ph) {
		Complex[] scaled0 = scale(h, ph);
		Complex[] scaled1 = Arrays.copyOf(scaled0, scaled0.length + 1);
		scaled1[h.length] = Complex.zero;
		Complex[] sub = To.array(poly.length, Complex.class, i -> Complex.sub(poly[i], scaled1[i]));
		return Boolean.TRUE ? divXms(sub, s) : div(sub, new Complex[] { s.scale(-1d), Complex.of(1f, 0f), });
	}

	private Complex ph(Complex[] poly, Complex[] h, Complex s) {
		return div(evaluate(poly, s), evaluate(h, s));
	}

	private Complex[] divXms(Complex[] num, Complex s) {
		var lengthm1 = num.length - 1;
		Complex[] div = new Complex[lengthm1];
		Complex numx = num[lengthm1];
		for (int i = lengthm1 - 1; 0 <= i; i--) {
			div[i] = numx;
			numx = Complex.add(num[i], Complex.mul(numx, s));
		}
		return div;
	}

	private Complex[] div(Complex[] num, Complex[] denom) {
		var numLength = num.length;
		var denomLength = denom.length;
		int diff;
		while (0 <= (diff = numLength - denomLength)) {
			Complex scale = div(num[numLength - 1], denom[denomLength - 1]);
			Complex[] scaled0 = scale(denom, scale);
			Complex[] scaled1 = new Complex[numLength];
			{
				var i = -1;
				while (++i < diff)
					scaled1[i] = Complex.zero;
				while (++i < numLength)
					scaled1[i] = scaled0[i - diff];
			}
			Complex[] num_ = num;
			Complex[] sub = To.array(numLength, Complex.class, i -> Complex.sub(num_[i], scaled1[i]));
			num = Arrays.copyOfRange(sub, 0, numLength - 1);
		}
		return num;
	}

	private Complex div(Complex num, Complex denom) {
		return Complex.mul(num, denom.inverse());
	}

	private Complex[] scale(Complex[] cs, Complex scale) {
		return To.array(cs.length, Complex.class, i -> Complex.mul(cs[i], scale));
	}

	private Complex[] d(Complex[] poly) {
		return To.array(poly.length - 1, Complex.class, i -> {
			var i1 = i + 1;
			return poly[i1].scale(i1);
		});
	}

	private float[] d(float[] poly) {
		return Floats_.toArray(poly.length - 1, i -> {
			var i1 = i + 1;
			return poly[i1] * i1;
		});
	}

	private Complex evaluate(Complex[] poly, Complex x) {
		Complex y = Complex.zero;
		for (int i = poly.length - 1; 0 <= i; i--)
			y = Complex.add(Complex.mul(y, x), poly[i]);
		return y;
	}

	private double evaluate(float[] poly, double x) {
		double y = 0d;
		for (int i = poly.length - 1; 0 <= i; i--)
			y = y * x + poly[i];
		return y;
	}

}
