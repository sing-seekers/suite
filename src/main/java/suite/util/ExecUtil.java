package suite.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ExecUtil {

	private int code;
	private String out;
	private String err;
	private Thread threads[];

	public ExecUtil(String command[], String in) throws IOException {
		InputStream bis = new ByteArrayInputStream(in.getBytes(FileUtil.charset));
		ByteArrayOutputStream bos0 = new ByteArrayOutputStream();
		ByteArrayOutputStream bos1 = new ByteArrayOutputStream();

		Process process = Runtime.getRuntime().exec(command);

		try {
			InputStream pis = process.getInputStream();
			InputStream pes = process.getErrorStream();
			OutputStream pos = process.getOutputStream();

			threads = new Thread[] { Copy.streamByThread(pis, bos0) //
					, Copy.streamByThread(pes, bos1) //
					, Copy.streamByThread(bis, pos) };

			for (Thread thread : threads)
				thread.start();

			code = process.waitFor();
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		} finally {
			process.destroy();
		}

		out = new String(bos0.toByteArray(), FileUtil.charset);
		err = new String(bos1.toByteArray(), FileUtil.charset);
	}

	@Override
	public String toString() {
		return "code = " + code //
				+ "\nout = " + out //
				+ "\nerr = " + err;
	}

	public int getCode() {
		return code;
	}

	public String getOut() {
		return out;
	}

	public String getErr() {
		return err;
	}

}
