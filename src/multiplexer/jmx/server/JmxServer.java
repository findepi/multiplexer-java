package multiplexer.jmx.server;

import java.io.File;
import java.net.SocketAddress;

import multiplexer.jmx.client.SendingMethod;
import multiplexer.jmx.exceptions.NoPeerForPeerIdException;
import multiplexer.jmx.internal.Connection;
import multiplexer.jmx.internal.ConnectionsManager;
import multiplexer.jmx.internal.MessageReceivedListener;
import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Constants.PeerTypes;
import multiplexer.protocol.Protocol.MultiplexerMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Piotr Findeisen
 */
public class JmxServer implements MessageReceivedListener, Runnable {

	public static final String UNKOWN_TYPE_NAME = "unknown";

	private static final Logger logger = LoggerFactory
		.getLogger(JmxServer.class);

	protected final ConnectionsManager connectionsManager;
	protected final SocketAddress serverAddress;

	public JmxServer(SocketAddress serverAddress) {
		this.serverAddress = serverAddress;
		connectionsManager = new ConnectionsManager(PeerTypes.MULTIPLEXER);
		connectionsManager.setMessageReceivedListener(this);
	}

	public void run() {
		// TODO(findepi) start listening on serverAddress
	}

	public void loadMessageDefinitions(File file) {
		// TODO(findepi)
	}

	public void loadMessageDefinitionsFromFile(String fileName) {
		loadMessageDefinitions(new File(fileName));
	}

	public void onMessageReceived(MultiplexerMessage message,
		Connection connection) {

		// TODO Auto-generated method stub

		logger.debug("message received\n{}\n", message);

		// routing based on to
		if (message.hasTo()) {
			schedule(message);
			return;
		}

		// routing based on overridden rules
		if (message.getOverrideRrulesCount() != 0) {
			assert message.getOverrideRrulesCount() > 0;
			// TODO(findepi) routing based on overridden rules
			return;
		}

		// routing based on type
		if (!message.hasType()) {
			logger.warn("message without type received\n{}\n", message);
			return;
		}
		switch (message.getType()) {
		case MessageTypes.PING:
			if (!message.hasFrom()) {
				logger.warn("received PING without from and to set:\n{}\n",
					message);
			} else {
				MultiplexerMessage response = connectionsManager
					.createMessageBuilder().setMessage(message.getMessage())
					.setType(MessageTypes.PING).setTo(message.getFrom())
					.build();
				schedule(response);
			}
			break;

		default:
			if (message.getType() > MessageTypes.MAX_MULTIPLEXER_META_PACKET) {
				// TODO use the generic rules
				break;
			}
			// fall through
		case MessageTypes.BACKEND_ERROR:
		case MessageTypes.DELIVERY_ERROR:
		case MessageTypes.CONNECTION_WELCOME:
		case MessageTypes.HEARTBIT:
		case MessageTypes.REQUEST_RECEIVED:
			logger.warn("don't know what to do with message type {} ({})",
				message.getType(), getMessageTypeName(message.getType()));
			break;

		case MessageTypes.BACKEND_FOR_PACKET_SEARCH:
			// TODO
			break;
		}
	}

	/**
	 * Send {@code message} to a client directly connected to this server and
	 * having ID {@code message.getType()}.
	 * 
	 * @param message
	 *            to be sent
	 */
	void schedule(MultiplexerMessage message) {
		assert message.hasTo();
		try {
			connectionsManager.sendMessage(message, SendingMethod.via(message
				.getTo()));
		} catch (NoPeerForPeerIdException e) {
			// TODO operation failed OR ignore ?
			// e.printStackTrace();
		}
	}

	static String getMessageTypeName(int type) {
		String name;
		name = MessageTypes.getConstantsNames().get(type);
		if (name != null)
			return name;
		// TODO retrieve the name from the data read from a config file
		return UNKOWN_TYPE_NAME;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented");
	}

}
