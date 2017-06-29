package suite.primitive;

public interface LngDbl_Int {

	public int apply(long c, double f);

	public default LngDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
