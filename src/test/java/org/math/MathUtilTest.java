package org.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.Suite;
import org.suite.node.Int;

public class MathUtilTest {

	@Test
	public void test() {
		assertEquals(Int.create(3), MathUtil.simplify(Suite.parse("1 + 2")));
	}

}
