package suite.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.util.Rewrite;

public class SymbolicTest {

	private Atom a = Atom.of("a");
	private Atom b = Atom.of("b");
	private Atom c = Atom.of("c");
	private Atom d = Atom.of("d");
	private Atom x = Atom.of("x");
	private Atom y = Atom.of("y");
	private Symbolic sym = new Symbolic();
	private Rewrite rw = new Rewrite();

	@Test
	public void testApply() {
		assertTrue(sym.fun(Suite.parse("x * x"), Atom.of("x")).apply(2f) == 4f);
	}

	@Test
	public void testCubic() {
		verifySimplify("4", "4");

		verifySimplify("(a * x + b) ^ 3", "" //
				+ "(a * a * a) * x * x * x" //
				+ " + ((3 * a * a) * b) * x * x" //
				+ " + ((3 * a) * b * b) * x" //
				+ " + b * b * b");

		verifySimplify("(a * x + neg b) ^ 3", "" //
				+ "(a * a * a) * x * x * x" //
				+ " + ((neg 3 * a * a) * b) * x * x" //
				+ " + ((3 * a) * b * b) * x" //
				+ " + neg 1 * b * b * b");

		Node poly = rw.replace(x, //
				Suite.parse("y + neg (b * inv (3 * a))"), //
				Suite.parse("a * x * x * x + b * x * x + c * x + d"));

		verifySimplify(poly, "" //
				+ "a * y * y * y" //
				+ " + (c + (neg inv 3 * inv a) * b * b) * y" //
				+ " + d + ((neg inv 3 * inv a) * b) * c + ((2 * inv 27) * inv (a * a)) * b * b * b");
	}

	@Test
	public void testDifferentiation() {
		verifyEquals("0", sym.d(Suite.parse("1"), x));
		verifyEquals("1", sym.d(Suite.parse("x"), x));
		verifyEquals("4 * x * x * x", sym.d(Suite.parse("x * x * x * x"), x));
		verifyEquals("neg 2 * inv (x * x)", sym.d(Suite.parse("2 / x"), x));
	}

	@Test
	public void testIntegration() {
		verifyEquals("cos x + x * sin x", sym.i(Suite.parse("x * cos x"), x));
	}

	@Test
	public void testSquare() {
		verifySimplify("(a * x + b) ^ 2", "" //
				+ "(a * a) * x * x" //
				+ " + ((2 * a) * b) * x" //
				+ " + b * b");
	}

	private void verifySimplify(String poly, String expected) {
		verifyEquals(expected, sym.simplify(Suite.parse(poly), y, x, d, c, b, a));
	}

	private void verifySimplify(Node poly, String expected) {
		verifyEquals(expected, sym.simplify(poly, y, x, d, c, b, a));
	}

	private void verifyEquals(String expected, Node node) {
		String actual = node.toString();
		System.out.println(actual);
		assertEquals(expected, actual);
	}

}
