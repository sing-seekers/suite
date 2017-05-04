package suite.os;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import suite.Constants;
import suite.util.Copy;
import suite.util.Rethrow;
import suite.util.To;

public class Execute {

	public final int code;
	public final String out;
	public final String err;
	private Thread threads[];

	public static String shell(String sh) {
		String[] command = null;

		for (String s : Arrays.asList("/bin/sh", "C:\\cygwin\\bin\\sh.exe", "C:\\cygwin64\\bin\\sh.exe"))
			if (Files.exists(Paths.get(s)))
				command = new String[] { s, };

		if (command != null) {
			Execute execute = new Execute(command, sh);
			if (execute.code == 0)
				return execute.out;
			else
				throw new RuntimeException(execute.toString());
		} else
			throw new RuntimeException("Cannot find shell executable");
	}

	public Execute(String[] command) {
		this(command, "");
	}

	public Execute(String[] command, String in) {
		InputStream bis = new ByteArrayInputStream(in.getBytes(Constants.charset));
		ByteArrayOutputStream bos0 = new ByteArrayOutputStream();
		ByteArrayOutputStream bos1 = new ByteArrayOutputStream();

		Process process = Rethrow.ex(() -> Runtime.getRuntime().exec(command));

		try {
			InputStream pis = process.getInputStream();
			InputStream pes = process.getErrorStream();
			OutputStream pos = process.getOutputStream();

			threads = new Thread[] { //
					Copy.streamByThread(pis, bos0), //
					Copy.streamByThread(pes, bos1), //
					Copy.streamByThread(bis, pos), };

			for (Thread thread : threads)
				thread.start();

			code = process.waitFor();

			for (Thread thread : threads)
				thread.join();
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		} finally {
			process.destroy();
		}

		out = To.string(bos0.toByteArray());
		err = To.string(bos1.toByteArray());
	}

	@Override
	public String toString() {
		return "code = " + code //
				+ "\nout = " + out //
				+ "\nerr = " + err;
	}

}
