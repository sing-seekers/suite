package suite.fp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.streamlet.Read;
import suite.util.String_;

public class Fun23TreeTest {

	@Test
	public void test() throws IOException {
		var n = 100;
		String list100 = "0 until " + n + " | map {insert} | apply | {Tree (9999, Empty;)}";

		Node fp0 = Suite.substitute("use 23-TREE >> " + list100);
		Node result0 = Suite.evaluateFun(Suite.fcc(fp0, false));
		String out0 = Formatter.dump(result0);
		System.out.println("OUT:\n" + out0);

		var nPars0 = Read.from(String_.chars(out0)).filter(c -> c == '(').size();
		assertTrue(3 <= nPars0);

		Node fp1 = Suite.substitute("use 23-TREE >> 0 until " + n / 2 + " | map {remove} | apply | {" + list100 + "}");
		Node result1 = Suite.evaluateFun(Suite.fcc(fp1, false));
		String out1 = Formatter.dump(result1);
		System.out.println("OUT:\n" + out1);

		var nPars1 = Read.from(String_.chars(out1)).filter(c -> c == '(').size();
		assertTrue(3 <= nPars1);
	}

}
