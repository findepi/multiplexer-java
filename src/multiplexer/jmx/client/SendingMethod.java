package multiplexer.jmx.client;

import multiplexer.jmx.internal.Connection;
import multiplexer.protocol.Constants.PeerTypes;
import multiplexer.protocol.Protocol.MultiplexerMessageDescription.RoutingRule;

/**
 * TODO javadoc
 * 
 * @author Piotr Findeisen
 * 
 */
public final class SendingMethod {

	public static final RoutingRule.Whom ANY = RoutingRule.Whom.ANY;
	public static final RoutingRule.Whom ALL = RoutingRule.Whom.ALL;

	public static final ViaConnectionsOfType THROUGH_ONE = via(
		PeerTypes.MULTIPLEXER, ANY);

	public static final ViaConnectionsOfType THROUGH_ALL = via(
		PeerTypes.MULTIPLEXER, ALL);

	public static ViaConnection via(Connection connection) {
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		return new ViaConnection(connection);
	}

	public static ViaPeer via(long peerId) {
		return new ViaPeer(peerId);
	}

	public static ViaConnectionsOfType via(int peerType,
		RoutingRule.Whom quantity) {
		return new ViaConnectionsOfType(peerType, quantity);
	}

	private SendingMethod() {
		assert false;
	}

	public static final class ViaConnectionsOfType {
		private int peerType;
		private final RoutingRule.Whom quantity;

		public ViaConnectionsOfType(int peerType, RoutingRule.Whom quantity) {
			super();
			this.peerType = peerType;
			this.quantity = quantity;
		}

		public int getPeerType() {
			return peerType;
		}

		public RoutingRule.Whom getQuantity() {
			return quantity;
		}
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
