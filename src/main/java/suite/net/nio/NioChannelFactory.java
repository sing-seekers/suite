package suite.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import suite.net.NetUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.Reactive;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Sink2;
import suite.util.Util;

public interface NioChannelFactory {

	public static char RESPONSE = 'P';
	public static char REQUEST = 'Q';

	public class PersistentNioChannel extends RequestResponseNioChannel {
		private NioDispatcher<PersistentNioChannel> nio;
		private InetSocketAddress address;
		private boolean isStarted;

		public PersistentNioChannel(NioDispatcher<PersistentNioChannel> nio, InetSocketAddress address) {
			this.nio = nio;
			this.address = address;
		}

		public void start() {
			isStarted = true;
			reconnect();
		}

		public void stop() {
			isStarted = false;
		}

		private void reconnect() {
			if (isStarted && !isConnected())
				try {
					nio.reconnect(this, address);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
		}
	}

	public class RequestResponseNioChannel extends PacketedNioChannel {
		public static char RESPONSE = 'P';
		public static char REQUEST = 'Q';
		private final Object mutex = new Object();
		private boolean isConnected;

		public void send(char type, int token, Bytes data) {
			if (!isConnected)
				synchronized (mutex) {
					while (!isConnected)
						Util.wait(mutex);
				}

			sendPacket(new BytesBuilder() //
					.append((byte) type) //
					.append(NetUtil.intToBytes(token)) //
					.append(data) //
					.toBytes());
		}

		public void setConnected(boolean isConnected) {
			synchronized (mutex) {
				this.isConnected = isConnected;
				mutex.notify();
			}
		}

		public boolean isConnected() {
			return isConnected;
		}
	}

	public class PacketedNioChannel extends BufferedNioChannel {
		public final Reactive<Bytes> onReceivePacket = new Reactive<>();

		public void sendPacket(Bytes packet) {
			send(new BytesBuilder() //
					.append(NetUtil.intToBytes(packet.size())) //
					.append(packet) //
					.toBytes());
		}
	}

	public class BufferedNioChannel extends NioChannel {
		private Bytes toSend = Bytes.empty;
		private Fun<Bytes, Bytes> sender;

		public void send(Bytes out) {
			toSend = toSend.append(out);
			trySend();
		}

		public void setSender(Fun<Bytes, Bytes> sender) {
			this.sender = sender;
			trySend();
		}

		public void trySend() {
			if (sender != null)
				toSend = sender.apply(toSend);
		}
	}

	public class NioChannel {
		public final Reactive<Fun<Bytes, Bytes>> onConnected = new Reactive<>();
		public final Reactive<Bytes> onReceive = new Reactive<>();
		public final Reactive<Boolean> onTrySend = new Reactive<>();
	}

	public static <C extends PersistentNioChannel> C persistent( //
			Source<C> source, //
			RequestResponseMatcher matcher, //
			ThreadPoolExecutor executor, //
			Fun<Bytes, Bytes> handler) {
		return requestResponse(source, matcher, executor, handler);
	}

	public static <C extends RequestResponseNioChannel> C requestResponse( //
			Source<C> source, //
			RequestResponseMatcher matcher, //
			ThreadPoolExecutor executor, //
			Fun<Bytes, Bytes> handler) {
		C channel = packeted(source);
		channel.onConnected.register(sender -> channel.setConnected(sender != null));
		channel.onReceivePacket.register(packet -> {
			if (5 <= packet.size()) {
				char type = (char) packet.get(0);
				int token = NetUtil.bytesToInt(packet.subbytes(1, 5));
				Bytes contents = packet.subbytes(5);

				if (type == RESPONSE)
					matcher.onResponseReceived(token, contents);
				else if (type == REQUEST)
					executor.execute(() -> channel.send(RESPONSE, token, handler.apply(contents)));
			}
		});
		return channel;
	}

	public static <C extends PacketedNioChannel> C packeted(Source<C> source) {
		C channel = buffered(source, new Sink2<C, Bytes>() {
			private Bytes received = Bytes.empty;

			public void sink2(C channel, Bytes message) {
				received = received.append(message);
				int size = received.size();

				if (4 <= size) {
					int end = 4 + NetUtil.bytesToInt(received.subbytes(0, 4));

					if (end <= size) {
						Bytes in = received.subbytes(4, end);
						received = received.subbytes(end);
						channel.onReceivePacket.fire(in);
					}
				}
			}
		});
		return channel;
	}

	public static <C extends BufferedNioChannel> C buffered(Source<C> source, Sink2<C, Bytes> onReceive) {
		C channel = source.source();
		channel.onConnected.register(channel::setSender);
		channel.onReceive.register(in -> onReceive.sink2(channel, in));
		channel.onTrySend.register(dummy -> channel.trySend());
		return channel;
	}

}
