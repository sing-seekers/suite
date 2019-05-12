package suite.net.cluster.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import suite.net.NetUtil;
import suite.net.cluster.ClusterProbe;
import suite.net.nio.NioDispatch;
import suite.net.nio.NioDispatch.AsyncRw;
import suite.object.Object_;
import suite.os.Log_;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Pusher;

public class NioCluster implements Closeable {

	private String me;
	private Map<String, InetSocketAddress> peers;
	private ClusterProbe probe;

	private NioDispatch nd = new NioDispatch();
	private Sink<IOException> f = Log_::error;
	private Closeable unlisten;

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, AsyncRw> rws = new HashMap<>();

	private Pusher<String> onJoined;
	private Pusher<String> onLeft;
	private Map<Class<?>, Fun<Object, Object>> onReceive = new HashMap<>();

	public NioCluster(String me, Map<String, InetSocketAddress> peers) throws IOException {
		this.me = me;
		this.peers = peers;
		probe = new ClusterProbeImpl(me, peers);
	}

	@Override
	public void close() throws IOException {
		nd.close();
	}

	public void start() throws IOException {
		unlisten = nd.new Responder().listen(peers.get(me).getPort(), req -> {
			var request = NetUtil.deserialize(req);
			var handler = onReceive.get(request.getClass());
			return NetUtil.serialize(handler.apply(request));
		}, f);

		onJoined = probe.getOnJoined();

		onLeft = probe.getOnLeft().map(node -> {
			var rw = rws.get(node);
			if (rw != null)
				rw.close();
			return node;
		});

		probe.start();
	}

	public void stop() throws IOException {
		for (var rw : rws.values())
			rw.close();

		probe.stop();
		Object_.closeQuietly(unlisten);
		nd.stop();
	}

	public void requestForResponse(String peer, Object request, Sink<Object> okay, Sink<IOException> fail) {
		if (probe.isActive(peer)) {
			var req = NetUtil.serialize(request);
			nd.new Requester(peers.get(peer)).request(req, rsp -> {
				var response = NetUtil.deserialize(rsp);
				okay.f(response);
			});
		} else
			fail.f(new IOException("peer " + peer + " is not active"));
	}

	public void run() {
		nd.run();
	}

	@SuppressWarnings("unchecked")
	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive) {
		this.onReceive.put(clazz, (Fun<Object, Object>) onReceive);
	}

	public Set<String> getActivePeers() {
		return probe.getActivePeers();
	}

	public Pusher<String> getOnJoined() {
		return onJoined;
	}

	public Pusher<String> getOnLeft() {
		return onLeft;
	}

	public String getMe() {
		return me;
	}

}
