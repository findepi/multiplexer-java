package multiplexer.jmx.client;

import multiplexer.protocol.Constants.PeerTypes;
import multiplexer.protocol.Protocol.MultiplexerMessageDescription.RoutingRule;

/**
 * Specifies how a message should be transferred from Multiplexer client to
 * server or from server further to recipients.
 * 
 * @author Piotr Findeisen
 */
public final class SendingMethod {

	public static final RoutingRule.Whom ANY = RoutingRule.Whom.ANY;
	public static final RoutingRule.Whom ALL = RoutingRule.Whom.ALL;

	public static final ViaConnectionsOfType THROUGH_ONE = via(
		PeerTypes.MULTIPLEXER, ANY);

	public static final ViaConnectionsOfType THROUGH_ALL = via(
		PeerTypes.MULTIPLEXER, ALL);

	/**
	 * Send a message over given {@link Connection}.
	 */
	public static ViaConnection via(Connection connection) {
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		return new ViaConnection(connection);
	}

	/**
	 * Send a message over a connection to peer with given id.
	 */
	public static ViaPeer via(long peerId) {
		return new ViaPeer(peerId);
	}

	/**
	 * Send a message to peer types with given type. If parameter {@code
	 * quantity} is {@code RoutingRule.Whom.ALL}, a message will be sent to all
	 * matching peers. If it's {@code RoutingRule.Whom.ANY}, a message will be
	 * sent to some chosen matching peer. With {@code RoutingRule.Whom.ANY}, the
	 * peers are guaranteed to be chosen with equal distribution.
	 */
	public static ViaConnectionsOfType via(int peerType,
		RoutingRule.Whom quantity) {
		return new ViaConnectionsOfType(peerType, quantity);
	}

	private SendingMethod() {
		assert false;
	}

	/**
	 * Not directly instantiatable. See
	 * {@link SendingMethod#via(int, multiplexer.protocol.Protocol.MultiplexerMessageDescription.RoutingRule.Whom)}
	 * .
	 * 
	 * @author Piotr Findeisen
	 * 
	 */
	public static final class ViaConnectionsOfType {
		private int peerType;
		private final RoutingRule.Whom quantity;

		private ViaConnectionsOfType(int peerType, RoutingRule.Whom quantity) {
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

	/**
	 * Not directly instantiatable. See {@link SendingMethod#via(Connection)}.
	 * 
	 * @author Piotr Findeisen
	 */
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

	/**
	 * Not directly instantiatable. See {@link SendingMethod#via(long)}.
	 * 
	 * @author Piotr Findeisen
	 */
	public static final class ViaPeer {
		private final long peerId;

		private ViaPeer(long peerId) {
			super();
			this.peerId = peerId;
		}

		public long getPeerId() {
			return peerId;
		}
	}
}
