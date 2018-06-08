package suite.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import suite.Constants;
import suite.os.LogUtil;
import suite.os.SocketUtil;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.Th;
import suite.util.Thread_;
import suite.util.Util;

/**
 * A very crude HTTP proxy.
 *
 * @author yw.sing
 */
public class HttpProxy {

	private int port;
	private Fun<String, IntObjPair<String>> target;

	public HttpProxy() {
		this(8051, path -> IntObjPair.of(9051, "127.0.0.1"));
	}

	public HttpProxy(int port, Fun<String, IntObjPair<String>> target) {
		this.port = port;
		this.target = target;
	}

	public void serve() {
		try {
			serve_();
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	private void serve_() throws IOException {
		new SocketUtil().listenIo(port, (is, os) -> {
			var line = Util.readLine(is);
			LogUtil.info("PROXY " + line);

			var url = line.split(" ")[1];
			var pp = String_.split2(url, "://");
			var path = pp != null ? String_.split2l(pp.t1, "/").t1 : url;
			var client = target.apply(path).map((port1, host1) -> Rethrow.ex(() -> new Socket(host1, port1)));

			try (var socket1 = client; //
					var is0 = is; //
					var os0 = os; //
					var is1 = socket1.getInputStream(); //
					var os1 = socket1.getOutputStream();) {
				os1.write((line + "\r\nConnection: close\r\n").getBytes(Constants.charset));
				var threads = Read.each(streamByThread(is0, os1, line), streamByThread(is1, os0, line));
				Thread_.startJoin(threads);
			}
		});
	}

	private static Th streamByThread(InputStream is, OutputStream os, String s) {
		return Thread_.newThread(() -> {
			try {
				stream(is, os);
			} catch (InterruptedIOException ex) {
			} catch (SocketException ex) {
				// if (!String_.equals(ex.getMessage(), "Socket closed"))
				throw new RuntimeException(s, ex);
			}
		});
	}

	private static void stream(InputStream in, OutputStream out) throws IOException {
		var buffer = new byte[Constants.bufferSize];
		int len;
		while (0 <= (len = in.read(buffer))) {
			out.write(buffer, 0, len);
			out.flush();
		}
	}

}
