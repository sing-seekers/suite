package suite.math;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class FiniteDifference {

	private double step = .001d;
	private double invStep = 1d / step;

	public Fun<float[], float[]> forward(Obj_Dbl<float[]> fun) {
		return xs -> {
			var ys = fun.apply(xs);
			return To.vector(xs.length, i -> {
				var x0 = xs[i];
				xs[i] += step;
				var gradient = (fun.apply(xs) - ys) * invStep;
				xs[i] = x0;
				return gradient;
			});
		};
	}

}
