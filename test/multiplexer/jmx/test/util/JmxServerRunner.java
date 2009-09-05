package multiplexer.jmx.test.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

import com.google.protobuf.ByteString;

import multiplexer.jmx.server.JmxServer;

/**
 * @author Piotr Findeisen
 * 
 */
public final class JmxServerRunner {

	private JmxServer server;
	private Thread serverThread;

	@SuppressWarnings("unchecked")
	public void start() throws Exception {
		start((Map<String, Object>) Collections.EMPTY_MAP);
	}

	public void start(final Map<String, Object> options) throws Exception {
		assert server == null;

		server = new JmxServer(new InetSocketAddress("0.0.0.0", 0));
		server.setTransferUpdateIntervalMillis(1000);
		server.loadMessageDefinitionsFromFile("test.rules");
		if (options.containsKey("multiplexerPassword"))
			server.setMultiplexerPassword((ByteString) options
				.get("multiplexerPassword"));

		serverThread = new Thread(server);
		serverThread.setDaemon(true);
		serverThread.start();

		synchronized (server) {
			if (!server.hasStarted()) {
				server.wait(5000);
			}
			assert server.hasStarted() : JmxServer.class.getSimpleName()
				+ " failed to start.";
		}
	}

	public int getLocalServerPort() {
		assert server != null;
		return server.getLocalPort();
	}

	public InetSocketAddress getLocalServerAddress()
		throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort());
	}

	public void stop() {
		stop(true);
	}

	public void stop(boolean check) {
		assert !check || server != null;
		assert !check || serverThread.isAlive();

		server.shutdown();
		try {
			serverThread.join(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (serverThread.isAlive())
			serverThread.interrupt();

		server = null;
		serverThread = null;
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		stop(false);
	}
}
