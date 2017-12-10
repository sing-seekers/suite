package suite.nn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.math.Sigmoid;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.util.FunUtil2.Fun2;
import suite.util.To;

public class NeuralNetwork {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();
	private Random random = new Random();

	private float learningRate = 1f;

	public interface Layer<I, O> {
		public O forward(I inputs);

		public O backprop(I inputs, O outputs, O errors);
	}

	public Fun2<float[], float[], float[]> ml(int[] sizes) {
		int nLayers = sizes.length - 1;
		List<Layer<float[], float[]>> layers = new ArrayList<>();
		int size = sizes[0];

		for (int i = 0; i < nLayers;) {
			int size0 = size;
			layers.add(new FeedForwardNnLayer(size0, size = sizes[++i]));
		}

		return (ins, expect) -> {
			float[][] inputs = new float[nLayers][];
			float[][] outputs = new float[nLayers][];
			float[] result = ins;

			for (int i = 0; i < nLayers; i++)
				result = outputs[i] = layers.get(i).forward(inputs[i] = result);

			if (expect != null) {
				float[] errors = vec.sub(expect, result);

				for (int i = nLayers - 1; 0 <= i; i--)
					errors = layers.get(i).backprop(inputs[i], outputs[i], errors);
			}

			return result;
		};
	}

	private class FeedForwardNnLayer implements Layer<float[], float[]> {
		private int nInputs;
		private int nOutputs;
		private float[][] weights;

		private FeedForwardNnLayer(int nInputs, int nOutputs) {
			this.nInputs = nInputs;
			this.nOutputs = nOutputs;
			weights = To.arrayOfFloats(nInputs, nOutputs, (i, j) -> random.nextFloat());
		}

		public float[] forward(float[] inputs) {
			float[] m = mtx.mul(inputs, weights);
			for (int j = 0; j < nOutputs; j++)
				m[j] = (float) Sigmoid.sigmoid(m[j]);
			return m;
		}

		public float[] backprop(float[] inputs, float[] outputs, float[] errors) {
			for (int j = 0; j < nOutputs; j++) {
				float e = errors[j] *= (float) Sigmoid.sigmoidGradient(outputs[j]);
				for (int i = 0; i < nInputs; i++)
					weights[i][j] += learningRate * inputs[i] * e;
			}
			return mtx.mul(weights, errors);
		}
	}

	@SuppressWarnings("unused")
	private class SpawnLayer<I, O> implements Layer<I, O[]> {
		private Class<O> clazz;
		private Layer<I, O>[] layers;

		private SpawnLayer(int size, Class<O> clazz, Layer<I, O>[] layers) {
			this.clazz = clazz;
			this.layers = layers;
		}

		public O[] forward(I inputs) {
			return To.array(layers.length, clazz, i -> layers[i].forward(inputs));
		}

		public O[] backprop(I inputs, O[] outputs, O[] errors) {
			return To.array(layers.length, clazz, i -> layers[i].backprop(inputs, outputs[i], errors[i]));
		}
	}

	@SuppressWarnings("unused")
	private class ConvNnLayer implements Layer<float[][], float[][]> {
		private int sx, sy;
		private float[][] kernel;
		private float bias;

		private ConvNnLayer(int sx, int sy) {
			this.sx = sx;
			this.sy = sy;
			kernel = To.arrayOfFloats(sx, sy, (x, y) -> random.nextFloat());
		}

		public float[][] forward(float[][] inputs) {
			int hsx = mtx.height(inputs) - sx + 1;
			int hsy = mtx.width(inputs) - sy + 1;
			return To.arrayOfFloats(hsx, hsy, (ox, oy) -> {
				double sum = bias;
				for (int x = 0; x < sx; x++)
					for (int y = 0; y < sy; y++)
						sum += inputs[ox + x][oy + y] * (double) kernel[x][y];
				return relu(sum);
			});
		}

		public float[][] backprop(float[][] inputs, float[][] outputs, float[][] errors) {
			int hsx = mtx.height(inputs) - sx + 1;
			int hsy = mtx.width(inputs) - sy + 1;
			float errors1[][] = new float[hsx][hsy];
			for (int ox = 0; ox < hsx; ox++)
				for (int oy = 0; oy < hsy; oy++) {
					float e = errors[ox][oy] *= (float) reluGradient(outputs[ox][oy]);
					bias += e;
					for (int x = 0; x < sx; x++)
						for (int y = 0; y < sy; y++) {
							int ix = ox + x;
							int iy = oy + y;
							errors1[ix][iy] += e * (double) (kernel[x][y] += learningRate * inputs[ix][iy] * e);
						}
				}

			return errors1;
		}
	}

	private double relu(double d) {
		return Math.min(0d, d);
	}

	private double reluGradient(double value) {
		return value < 0f ? 0f : 1f;
	}

}
