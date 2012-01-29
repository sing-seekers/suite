package org.instructionexecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.node.Tree;

public class FunctionCompilerTest {

	private static final String and = "" //
			+ "and = (x => y => x ? y | false) >> \n";

	private static final String concat = "" //
			+ "concat = split {h => t => \n" //
			+ "    if-tree {h} \n" //
			+ "        {h1 => t1 => cons {h1} {concat {t1:t}}} \n" //
			+ "        {concat {t}} \n" //
			+ "} >> \n";

	private static final String contains = "" //
			+ "contains = (e => \n" //
			+ "    join {map {e1 => e1 = e}} {fold {or}} \n" //
			+ ") >> \n";

	private static final String filter0 = "" //
			+ "filter0 = (f => \n" //
			+ "    split {h => t => \n" //
			+ "        others = filter0 {f} {t} >> \n" //
			+ "        f {h} ? h:others | others \n" //
			+ "    } \n" //
			+ ") >> \n";

	private static final String filter = "" //
			+ "filter = (f => fold-right {i => list => f {i} ? i:list | list} {()}) >> \n";

	private static final String fold = "" //
			+ "fold = (f => list => \n" //
			+ "    h = head {list} >> \n" //
			+ "    t = tail {list} >> \n" //
			+ "    is-tree {t} ? f {h} {fold {f} {t}} | h \n" //
			+ ") >> \n";

	private static final String foldLeft = "" //
			+ "fold-left = (f => i => list => \n" //
			+ "    if-tree {list} \n" //
			+ "        {h => t => fold-left {f} {f {i} {h}} {t}} \n" //
			+ "        {i} \n" //
			+ ") >> \n";

	private static final String foldRight = "" //
			+ "fold-right = (f => i => list => \n" //
			+ "    if-tree {list} \n" //
			+ "        {h => t => f {h} {fold-right {f} {i} {t}}} \n" //
			+ "        {i} \n" //
			+ ") >> \n";

	private static final String ifTree = "" //
			+ "if-tree = (list => f1 => f2 => \n" //
			+ "    if (is-tree {list}) then ( \n" //
			+ "        f1 {head {list}} {tail {list}} \n" //
			+ "    ) \n" //
			+ "    else f2 \n" //
			+ ") >> \n";

	private static final String join = "" //
			+ "join = (f => g => x => g {f {x}}) >> \n";

	private static final String map0 = "" //
			+ "map0 = (f => split {h => t => (f {h}):(map0 {f} {t})}) >> \n";

	private static final String map = "" //
			+ "map = (f => fold-right {i => list => (f {i}):list} {()}) >> \n";

	private static final String or = "" //
			+ "or = (x => y => x ? true | y) >> \n";

	private static final String split = "" //
			+ "split = (f => list => if-tree {list} {f} {()}) >> \n";

	@Test
	public void testClosure() {
		assertEquals(Int.create(7), eval("" //
				+ "add = (p => q => p + q) >> add {3} {4}"));
		assertEquals(Int.create(20), eval("" //
				+ "p = (n => n + 1) >> \n" //
				+ "q = (n => p {n} * 2) >> \n" //
				+ "q {9}"));
	}

	@Test
	public void testConcat() {
		assertEquals(SuiteUtil.parse("1:2:3:4:5:6:"), eval("" //
				+ ifTree + split + concat //
				+ "concat {(1:2:):(3:4:):(5:6:):}"));
	}

	@Test
	public void testContains() {
		assertEquals(Atom.create("true"), eval("" //
				+ join + fold + or + ifTree + foldRight + map + contains //
				+ "contains {9} {7:8:9:10:11:}"));
		assertEquals(Atom.create("false"), eval("" //
				+ join + fold + or + ifTree + foldRight + map + contains //
				+ "contains {12} {7:8:9:10:11:}"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.create(89), eval("" //
				+ "fib = (n => \n" //
				+ "    if (n > 1) then ( \n" //
				+ "        fib {n - 1} + fib {n - 2} \n" //
				+ "    ) \n" //
				+ "    else 1 \n" //
				+ ") >> \n" //
				+ "fib {10}"));
	}

	@Test
	public void testFilter() {
		assertEquals(SuiteUtil.parse("4:6:"), eval("" //
				+ ifTree + split + filter0 //
				+ "filter0 {n => n % 2 = 0} {3:4:5:6:}"));
		assertEquals(SuiteUtil.parse("4:6:"), eval("" //
				+ ifTree + foldRight + filter //
				+ "filter {n => n % 2 = 0} {3:4:5:6:}"));
	}

	@Test
	public void testFold() {
		assertEquals(Int.create(324), eval("" //
				+ fold //
				+ "fold {a => b => a * b} {2:3:6:9:}"));
		assertEquals(Int.create(79), eval("" //
				+ ifTree + foldLeft //
				+ "fold-left {a => b => a - b} {100} {6:7:8:}"));
		assertEquals(Int.create(-93), eval("" //
				+ ifTree + foldRight //
				+ "fold-right {a => b => a - b} {100} {6:7:8:}"));
	}

	@Test
	public void testJoin() {
		assertEquals(Int.create(19), eval("" //
				+ join //
				+ "p = (n => n * 2) >> \n" //
				+ "q = (n => n + 1) >> \n" //
				+ "r = (join {p} {q}) >> \n" //
				+ "r {9}"));
	}

	@Test
	public void testLog() {
		assertEquals(Int.create(1), eval("" //
				+ "if (1 = 1) then 1 else (1 / 0)"));
	}

	@Test
	public void testSwitch() {
		assertEquals(new Str("C"), eval("" //
				+ "switch = (p => \n" //
				+ "    p = 1 ? \"A\" | \n" //
				+ "    p = 2 ? \"B\" | \n" //
				+ "    p = 3 ? \"C\" | \n" //
				+ "    \"D\" \n" //
				+ ") >> \n" //
				+ "switch {3}"));
	}

	@Test
	public void testIf() {
		assertEquals(Int.create(0), eval("3 > 4 ? 1 | 0"));
		assertEquals(Int.create(1), eval("3 = 3 ? 1 | 0"));
	}

	@Test
	public void testMap() {
		assertEquals(SuiteUtil.parse("5:6:7:"), eval("" //
				+ ifTree + split + map0 //
				+ "map0 {n => n + 2} {3:4:5:}"));
		assertEquals(SuiteUtil.parse("5:6:7:"), eval("" //
				+ ifTree + foldRight + map //
				+ "map {n => n + 2} {3:4:5:}"));
	}

	@Test
	public void testOperator() {
		assertEquals(Atom.create("true"), eval("" //
				+ and + or //
				+ "and {1 = 1} {or {1 = 0} {1 = 1}}"));
	}

	@Test
	public void testRange() {
		assertEquals(SuiteUtil.parse("2:5:8:11:"), eval("" //
				+ "range = (i => j => inc => \n" //
				+ "    if (i != j) then ( \n" //
				+ "        i:(range {i + inc} {j} {inc}) \n" //
				+ "    ) \n" //
				+ "    else () \n" //
				+ ") >> \n" //
				+ "range {2} {14} {3}"));
	}

	@Test
	public void testSplit() {
		assertEquals(Int.create(1), eval("" //
				+ ifTree + split //
				+ "split {h => l => h} {1:2:}"));
	}

	@Test
	public void testSys() {
		assertNotNull(Tree.decompose(eval("cons {1} {2:}")));
		assertEquals(Int.create(1), eval("head {1:2:3:}"));
		assertNotNull(Tree.decompose(eval("tail {1:2:3:}")));
	}

	private static Node eval(String f) {
		return SuiteUtil.evaluateFunctional(f);
	}

}
