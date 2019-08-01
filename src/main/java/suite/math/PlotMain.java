package suite.math;

import primal.primitive.DblDbl_Dbl;
import primal.primitive.IntInt_Int;
import suite.game.Render;
import suite.util.RunUtil;

public class PlotMain {

	public static void main(String[] args) {
		RunUtil.run(() -> new PlotMain().run());
	}

	private boolean run() {
		var size = 1024;
		var scale = 1d / (size + 1);

		DblDbl_Dbl variety = (x, y) -> {
			return y * y - (x + .25f) * (x + .15f) * (x + .05f) * (x - .05f) * (x - .15f) * (x - .25f);
		};

		IntInt_Int fp = (fx, fy) -> {
			var x0 = fx * scale - .5d;
			var y0 = fy * scale - .5d;
			var value = variety.apply(x0, y0);
			if (Double.isNaN(value))
				return -2;
			else if (value < 0)
				return -1;
			else
				return 1;
		};

		return new Render() //
				.renderPixels(size, size, (fx, fy) -> {
					var b0 = fp.apply(fx, fy);
					var b1 = fp.apply(fx + 1, fy);
					var b2 = fp.apply(fx, fy + 1);
					var b3 = fp.apply(fx + 1, fy + 1);
					var c = b0 != b1 || b1 != b2 || b2 != b3 ? 1d : 0d;
					return new R3(c, c, c);
				}) //
				.view();
	}

}
