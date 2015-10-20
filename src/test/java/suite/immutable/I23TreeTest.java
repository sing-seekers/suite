package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import suite.util.Util;

public class I23TreeTest {

	private int max = 32;

	private Comparator<Integer> comparator = Util.<Integer> comparator();

	@Test
	public void test0() {
		I23Tree<Integer> tree23 = new I23Tree<>(comparator);

		for (int i = 0; i < max; i++)
			tree23 = tree23.add(i);

		tree23.validate();
		System.out.println(tree23.toString());
		assertEquals(max, tree23.stream().size());

		for (int i = 0; i < max; i++)
			tree23 = tree23.remove(i);

		tree23.validate();
		System.out.println(tree23.toString());
		assertEquals(0, tree23.stream().size());
	}

	@Test
	public void test1() {
		I23Tree<Integer> tree23 = new I23Tree<>(comparator);

		for (int i = 0; i < max; i += 2)
			tree23 = tree23.add(i);
		for (int i = 1; i < max; i += 2)
			tree23 = tree23.add(i);
		tree23.validate();
		assertEquals(max, tree23.stream().size());

		for (int i = 0; i < max; i += 2)
			tree23 = tree23.remove(i);
		for (int i = 1; i < max; i += 2)
			tree23 = tree23.remove(i);
		tree23.validate();
		assertEquals(0, tree23.stream().size());
	}

	@Test
	public void test2() {
		List<Integer> list = new ArrayList<>();
		for (int i = 0; i < max; i++)
			list.add(i);

		I23Tree<Integer> tree23 = new I23Tree<>(comparator);

		Collections.shuffle(list);
		for (int i : list)
			tree23 = tree23.add(i);

		tree23.validate();
		System.out.println(tree23.toString());
		assertEquals(max, tree23.stream().size());

		Collections.shuffle(list);
		for (int i : list)
			tree23 = tree23.remove(i);

		tree23.validate();
		System.out.println(tree23.toString());
		assertEquals(0, tree23.stream().size());
	}

}
