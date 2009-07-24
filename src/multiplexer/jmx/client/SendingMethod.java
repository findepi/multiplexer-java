package multiplexer.jmx.client;

import multiplexer.jmx.internal.Connection;

/**
 * TODO javadoc
 * 
 * @author Piotr Findeisen
 * 
 */
public final class SendingMethod {

	public static final SendingMethod THROUGH_ONE = new SendingMethod();
	public static final SendingMethod THROUGH_ALL = new SendingMethod();

	public static ViaConnection via(Connection connection) {
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		return new ViaConnection(connection);
	}
	
	public static ViaPeer via(long peerId) {
		return new ViaPeer(peerId);
	}

	private SendingMethod() {
	}

	public static final class ViaConnection {
		private final Connection connection;

		private ViaConnection(Connection connection) {
			super();
			this.connection = connection;
		}

		public Connection getConnection() {
			return connection;
		}
	}

	public static final class ViaPeer {
		private final long peerId;

		public ViaPeer(long peerId) {
			super();
			this.peerId = peerId;
		}

		public long getPeerId() {
			return peerId;
		}
	}
}
