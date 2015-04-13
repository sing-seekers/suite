package suite.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.inspect.Inspect;
import suite.lp.Configuration.ProverConfig;
import suite.node.Node;

public class NodifyTest {

	private Nodify nodify = new Nodify(new Inspect());

	public interface I {
	}

	public static class Container {
		private List<I> is;
	}

	public static class A implements I {
		private int i = 123;
	}

	public static class B implements I {
		private String s = "test";
	}

	@Test
	public void testMapify() {
		ProverConfig pc0 = new ProverConfig();
		pc0.setRuleSet(null);

		Node node = nodify.nodify(ProverConfig.class, pc0);
		assertNotNull(node);
		System.out.println(node);

		ProverConfig pc1 = nodify.unnodify(ProverConfig.class, node);
		System.out.println(pc1);

		assertEquals(pc0, pc1);
		assertTrue(pc0.hashCode() == pc1.hashCode());
	}

	// When mapifying a field with interface type, it would automatically embed
	// object type information (i.e. class name), and un-mapify accordingly.
	@Test
	public void testPolymorphism() {
		A a = new A();
		B b = new B();
		Container object0 = new Container();
		object0.is = Arrays.asList(a, b);

		Node node = nodify.nodify(Container.class, object0);
		assertNotNull(node);
		System.out.println(node);

		Container object1 = nodify.unnodify(Container.class, node);
		assertEquals(A.class, object1.is.get(0).getClass());
		assertEquals(B.class, object1.is.get(1).getClass());
		assertEquals(123, ((A) object1.is.get(0)).i);
		assertEquals("test", ((B) object1.is.get(1)).s);
	}

}
