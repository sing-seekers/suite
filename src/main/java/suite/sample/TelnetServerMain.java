package suite.sample;

import static primal.statics.Fail.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.os.Log_;
import suite.os.Listen;
import suite.util.Copy;
import suite.util.RunUtil;

public class TelnetServerMain {

	public static void main(String[] args) {
		RunUtil.run(new TelnetServerMain()::run);
	}

	private boolean run() {
		new Listen().io(2323, (sis, sos) -> new Server().serve(sis, sos));
		return true;
	}

	private class Server {
		private void serve(InputStream sis, OutputStream sos) throws IOException {

			// kills the process if client closes the stream;
			// closes the stream if process is terminated/ended output.
			// therefore we need the interruption mechanism.
			var process = Runtime.getRuntime().exec("bash");
			var pis = process.getInputStream();
			var pes = process.getErrorStream();
			var pos = process.getOutputStream();

			try {
				var threads = Read
						.<Thread> each(
								Copy.streamByThread(pis, sos),
								Copy.streamByThread(pes, sos),
								Copy.streamByThread(sis, pos))
						.cons(new InterruptibleThread() {
							protected void run_() throws InterruptedException {
								process.waitFor();
							}
						})
						.collect();

				for (var thread : threads)
					thread.start();
				for (var thread : threads)
					thread.join();
			} catch (InterruptedException ex) {
				fail(ex);
			} finally {
				process.destroy();
			}
		}

		private abstract class InterruptibleThread extends Thread {
			public void run() {
				try {
					run_();
				} catch (InterruptedException | InterruptedIOException ex) {
				} catch (Exception ex) {
					Log_.error(ex);
				} finally {

					// if we are not being interrupted by another thread, issue
					// interrupt signal to other threads
					if (!isInterrupted())
						for (var thread : threads)
							if (thread != this)
								thread.interrupt();
				}
			}

			protected abstract void run_() throws Exception;
		}

		private Set<Thread> threads = new HashSet<>();
	}

}
