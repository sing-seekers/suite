package suite.net.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import suite.net.NioDispatcher;
import suite.net.RequestResponseMatcher;
import suite.primitive.Bytes;
import suite.util.FunUtil.Fun;

/**
 * Channel that will reconnect if failed for any reason.
 */
public class PersistentChannel extends RequestResponseChannel implements ServiceChannel {

	private NioDispatcher<PersistentChannel> dispatcher;
	private InetSocketAddress address;
	private boolean isStarted;

	public PersistentChannel( //
			NioDispatcher<PersistentChannel> dispatcher, //
			RequestResponseMatcher matcher, //
			ThreadPoolExecutor executor, //
			InetSocketAddress address, //
			Fun<Bytes, Bytes> handler) {
		super(matcher, executor, handler);
		this.dispatcher = dispatcher;
		this.address = address;
	}

	@Override
	public synchronized void start() {
		isStarted = true;
		reconnect();
	}

	@Override
	public synchronized void stop() {
		isStarted = false;
	}

	@Override
	public void onClose() {
		reconnect();
	}

	private void reconnect() {
		if (isStarted && !isConnected())
			try {
				dispatcher.reconnect(this, address);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

}
