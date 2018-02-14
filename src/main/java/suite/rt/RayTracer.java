package suite.rt;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import suite.image.Render;
import suite.math.R3;

/**
 * TODO fix RayTracerTest.testLight() etc cases black-out issues
 *
 * TODO test accurate Fresnel (and Schlick's approximation?)
 *
 * @author ywsing
 */
public class RayTracer {

	public static double negligibleAdvance = .0001d;

	private int depth = 4;

	private double airRefractiveIndex = 1d;
	private double glassRefractiveIndex = 1.1d;
	private double adjustFresnel = 0d;

	private R3 ambient = R3.origin;

	private Collection<LightSource> lightSources;
	private RtObject scene;

	public interface RtObject {

		/**
		 * Calculates hit point with a ray. Assumes direction is normalized.
		 */
		public List<RayHit> hit(Ray ray);
	}

	public interface RayHit {
		public double advance();

		public RayIntersection intersection();

		public Comparator<RayHit> comparator = (rh0, rh1) -> rh0.advance() < rh1.advance() ? -1 : 1;
	}

	public interface RayIntersection {
		public R3 hitPoint();

		public R3 normal();

		public Material material();
	}

	public interface Material {
		public R3 surfaceColor();

		public boolean isReflective();

		public double transparency();
	}

	public static class Ray {
		public R3 startPoint;
		public R3 dir;

		public Ray(R3 startPoint, R3 dir) {
			this.startPoint = startPoint;
			this.dir = dir;
		}

		public R3 hitPoint(double advance) {
			return R3.add(startPoint, dir.scale(advance));
		}

		public String toString() {
			return startPoint + " => " + dir;
		}
	}

	public interface LightSource {
		public R3 source();

		public R3 lit(R3 point);
	}

	public RayTracer(Collection<LightSource> lightSources, RtObject scene) {
		this.lightSources = lightSources;
		this.scene = scene;
	}

	public R3 test() {
		return test(new Ray(R3.origin, new R3(0d, 0d, 1d)));
	}

	public R3 test(Ray ray) {
		return traceRay(depth, ray);
	}

	public BufferedImage trace(int width, int height, int viewDistance) {
		double ivd = viewDistance / width;

		return new Render().render(width, height, (x, y) -> {
			R3 dir = new R3(x, y, ivd);
			return traceRay(depth, new Ray(R3.origin, dir));
		});
	}

	private R3 traceRay(int depth, Ray ray) {
		RayHit rayHit = nearestHit(scene.hit(ray));
		R3 color1;

		if (rayHit != null) {
			RayIntersection i = rayHit.intersection();
			R3 hitPoint = i.hitPoint();
			R3 normal0 = i.normal().norm();

			double dot0 = R3.dot(ray.dir, normal0);
			boolean isInside = 0d < dot0;
			R3 normal;
			double dot;

			if (!isInside) {
				normal = normal0;
				dot = dot0;
			} else {
				normal = normal0.neg();
				dot = -dot0;
			}

			Material material = i.material();
			boolean reflective = material.isReflective();
			double transparency = material.transparency();
			R3 color;

			if ((reflective || transparency < 0d) && 0 < depth) {
				double cos = -dot / Math.sqrt(ray.dir.abs2());

				// account reflection
				R3 reflectDir = R3.add(ray.dir, normal.scale(-2f * dot));
				R3 reflectPoint = R3.add(hitPoint, negligible(normal));
				R3 reflectColor = traceRay(depth - 1, new Ray(reflectPoint, reflectDir));

				// account refraction
				double eta = isInside ? glassRefractiveIndex / airRefractiveIndex : airRefractiveIndex / glassRefractiveIndex;
				double k = 1d - eta * eta * (1d - cos * cos);
				R3 refractColor;

				if (0 <= k) {
					R3 refractDir = R3.add(ray.dir.scale(eta / Math.sqrt(ray.dir.abs2())), normal.scale(eta * cos - Math.sqrt(k)));
					R3 refractPoint = R3.sub(hitPoint, negligible(normal));
					refractColor = traceRay(depth - 1, new Ray(refractPoint, refractDir));
				} else
					refractColor = R3.origin;

				// accurate Fresnel equation
				// double cos1 = Math.sqrt(k);
				// double f0 = (eta * cos - cos1) / (eta * cos + cos1);
				// double f1 = (cos - eta * cos1) / (cos + eta * cos1);
				// double fresnel = (f0 * f0 + f1 * f1) / 2d;

				// schlick approximation
				boolean isDramaticMix = true;
				double r = (airRefractiveIndex - glassRefractiveIndex) / (airRefractiveIndex + glassRefractiveIndex);
				double mix = isDramaticMix ? .1d : r * r;
				double cos1 = 1d - cos;
				double cos2 = cos1 * cos1;
				double fresnel = mix + (1d - mix) * cos1 * cos2 * cos2;

				// fresnel is often too low. Mark it up for visual effect.
				double fresnel1 = adjustFresnel + fresnel * (1d - adjustFresnel);

				color = R3.add(reflectColor.scale(fresnel1), refractColor.scale((1d - fresnel1) * transparency));
			} else {
				color = R3.origin;

				// account light sources
				for (LightSource lightSource : lightSources) {
					R3 lightDir = R3.sub(lightSource.source(), hitPoint);
					double lightDot = R3.dot(lightDir, normal);

					if (0d < lightDot) { // facing the light
						R3 lightPoint = R3.add(hitPoint, negligible(normal));
						RayHit lightRayHit = nearestHit(scene.hit(new Ray(lightPoint, lightDir)));

						if (lightRayHit == null || 1d < lightRayHit.advance()) {
							R3 lightColor = lightSource.lit(hitPoint);
							double cos = lightDot / Math.sqrt(lightDir.abs2());
							color = R3.add(color, lightColor.scale(cos));
						}
					}
				}
			}

			color1 = mc(color, material.surfaceColor());
		} else
			color1 = ambient;

		return color1;
	}

	private RayHit nearestHit(List<RayHit> rayHits) {
		return RayHit_.filter(rayHits).minOrNull(RayHit.comparator);
	}

	/**
	 * Multiply vector components.
	 */
	private static R3 mc(R3 u, R3 v) {
		return new R3(u.x * v.x, u.y * v.y, u.z * v.z);
	}

	private static R3 negligible(R3 v) {
		return new R3(negligible(v.x), negligible(v.y), negligible(v.z));
	}

	private static double negligible(double d) {
		if (0d < d)
			return negligibleAdvance;
		else if (d < 0d)
			return -negligibleAdvance;
		else
			return 0d;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setAdjustFresnel(double adjustFresnel) {
		this.adjustFresnel = adjustFresnel;
	}

	public void setAmbient(R3 ambient) {
		this.ambient = ambient;
	}

}
