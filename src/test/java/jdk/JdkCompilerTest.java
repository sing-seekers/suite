package jdk;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import suite.util.FileUtil;
import suite.util.JdkUtil;

public class JdkCompilerTest {

	@Test
	public void test() throws IOException, ReflectiveOperationException {
		String srcDir = FileUtil.tmp + "/src";
		String binDir = FileUtil.tmp + "/bin";

		String className = "HelloWorld";

		new File(srcDir).mkdirs();
		new File(binDir).mkdirs();

		String src = "" //
				+ "public class " + className + " implements Runnable {" //
				+ "  public void run() {" //
				+ "    System.out.println(\"TEST\");" //
				+ "  }" //
				+ "}";

		try (JdkUtil jdkUtil = new JdkUtil(srcDir, binDir)) {
			jdkUtil.newInstance(Runnable.class, src, "", className).run();
		}
	}

}
