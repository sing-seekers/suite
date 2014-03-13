package suite.fp.eval;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;

public class LazyFunTest {

	@Test
	public void testClosure() {
		assertEquals(Suite.parse("4"), eval("define v := number of 4 >> (i => j => v) {1} {2}"));
	}

	@Test
	public void testCorecursion() {
		assertEquals(Atom.TRUE, eval("" //
				+ "define seq := n => n; seq {n} >> \n" //
				+ "head {seq {0}} = 0"));

		assertEquals(Int.create(89), eval("" // Real co-recursion!
				+ "define fib := i1 => i2 => i2; fib {i2} {i1 + i2} >> \n" //
				+ "fib {0} {1} | get {10}"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.create(89), eval("" //
				+ "define fib := \n" //
				+ "    1; 1; zip {`+`} {fib} {tail {fib}} \n" //
				+ ">> fib | get {10}"));
	}

	@Test
	public void testFold() {
		assertEquals(Suite.parse("0; 1; 2; 3; 4;"), eval("" //
				+ "define inf-series := n => n; inf-series {n + 1} >> " //
				+ "0 | inf-series | fold-right {`;`} {} | take {5}"));

		// On the other hand, same call using fold-left would result in infinite
		// loop, like this:
		// define is = (n => n; is {n + 1}) >>
		// 0 | is | fold-left {`;`/} {} | take {5}
	}

	@Test
	public void testIterate() {
		assertEquals(eval("65536"), eval("iterate {`* 2`} {1} | get {16}"));
	}

	@Test
	public void testString() {
		assertEquals(Int.create(-34253924), eval("str-to-int {\"-34253924\"}"));
		assertEquals(Atom.TRUE, eval("\"-34253924\" = int-to-str {-34253924}"));
	}

	@Test
	public void testSubstitution() {
		assertEquals(Int.create(8), eval("define v := 4 >> v + v"));
	}

	@Test
	public void testSystem() {
		assertEquals(Atom.TRUE, eval("1 = 1"));
		assertEquals(Atom.FALSE, eval("1 = 2"));
		eval("cons {1} {}");
		eval("head {1; 2; 3;}");
		eval("tail {1; 2; 3;}");
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, true);
	}

}
