package suite.algo;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import suite.Constants;
import suite.os.FileUtil;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;
import suite.util.To;

public class LempelZivWelchTest {

	@Test
	public void test0() {
		var s = "";
		assertEquals(s, doTest(s));
	}

	@Test
	public void test1() {
		var s = "abababa";
		assertEquals(s, doTest(s));
	}

	@Test
	public void test2() {
		var s = "abababababababababababababababab";
		assertEquals(s, doTest(s));
	}

	@Test
	public void test3() {
		var s = FileUtil.read("src/main/java/suite/algo/LempelZivWelch.java");
		assertEquals(s, doTest(s));
	}

	private String doTest(String s0) {
		var bs = s0.getBytes(Constants.charset);

		Source<Byte> source0 = new Source<>() {
			private int index;

			public Byte source() {
				return index < bs.length ? bs[index++] : null;
			}
		};

		LempelZivWelch<Byte> lzw = new LempelZivWelch<>(allBytes());
		Source<Integer> source1 = lzw.encode(source0);
		Source<Byte> source2 = lzw.decode(source1);

		var bb = new BytesBuilder();
		Byte b;

		while ((b = source2.source()) != null)
			bb.append(b);

		return To.string(bb.toBytes());
	}

	private List<Byte> allBytes() {
		var bytes = new ArrayList<Byte>();
		byte b = 0;

		do
			bytes.add(b);
		while (++b != 0);
		return bytes;
	}

}
