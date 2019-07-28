package suite.primitive;

import static suite.util.Fail.fail;

import suite.object.Object_;

/**
 * An indirect reference to a primitive double. Double.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class DblMutable {

	private static double empty = DblFunUtil.EMPTYVALUE;

	private double value;

	public static DblMutable nil() {
		return DblMutable.of(empty);
	}

	public static DblMutable of(double c) {
		var p = new DblMutable();
		p.update(c);
		return p;
	}

	public double increment() {
		return value++;
	}

	public void set(double c) {
		if (value == empty)
			update(c);
		else
			fail("value already set");
	}

	public void update(double c) {
		value = c;
	}

	public double value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == DblMutable.class && value == ((DblMutable) object).value;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public String toString() {
		return value != empty ? Double.toString(value) : "null";
	}

}
