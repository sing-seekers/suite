package suite.lp.doer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.util.Util;

public class FormatterTest {

	@Test
	public void testDisplay() {
		assertEquals("123", Formatter.display(Int.create(123)));
		testDisplay("1 + 2 * 3");
		testDisplay("1 * 2 - 3");
		testDisplay("1 - 2 - 3");
		testDisplay("1 + 2 + 3");
		testDisplay("(1 + 2) * 3");
		testDisplay("a - b - c");
		testDisplay("a - (b - c)");
		testDisplay("(a, b) = (c, d)");
		Util.dump(Util.currentMethod(), Formatter.display(new Reference()));
	}

	@Test
	public void testDump() {
		testDump("-1");
		testDump("'-1'");
		testDump("'+xFEDC3210'");
	}

	private void testDisplay(String s) {
		Node node = Suite.parse(s);
		assertEquals(s, Formatter.display(node));
	}

	private void testDump(String s) {
		Node node = Suite.parse(s);
		assertEquals(s, Formatter.dump(node));
	}

}
