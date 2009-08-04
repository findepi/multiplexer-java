package multiplexer.jmx.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map;

import multiplexer.jmx.client.SendingMethod;
import multiplexer.jmx.exceptions.NoPeerForPeerIdException;
import multiplexer.jmx.internal.Connection;
import multiplexer.jmx.internal.ConnectionsManager;
import multiplexer.jmx.internal.MessageReceivedListener;
import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Constants.PeerTypes;
import multiplexer.protocol.Protocol.MultiplexerMessage;
import multiplexer.protocol.Protocol.MultiplexerMessageDescription;
import multiplexer.protocol.Protocol.MultiplexerPeerDescription;
import multiplexer.protocol.Protocol.MultiplexerRules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;

/**
 * @author Piotr Findeisen
 */
// TODO javadoc
public class JmxServer implements MessageReceivedListener, Runnable {

	public static final String UNKOWN_TYPE_NAME = "unknown";

	private static final Logger logger = LoggerFactory
		.getLogger(JmxServer.class);

	protected final ConnectionsManager connectionsManager;
	protected final SocketAddress serverAddress;

	protected Map<String, Integer> peerNamesToPeerIds = Maps.newHashMap();
	protected Map<Integer, MultiplexerMessageDescription> messageIdsToDescription = Maps
		.newHashMap();

	public JmxServer(SocketAddress serverAddress) {
		this.serverAddress = serverAddress;
		connectionsManager = new ConnectionsManager(PeerTypes.MULTIPLEXER);
		connectionsManager.setMessageReceivedListener(this);
	}

	public void run() {
		// TODO(findepi) start listening on serverAddress
	}

	protected MultiplexerMessageDescription registerMessageDescription(MultiplexerMessageDescription description) {
		assert description.hasType();
		return messageIdsToDescription.put(description.getType(), description);
	}
	
	public void loadMessageDefinitions(MultiplexerRules additionalRules) {
		for (MultiplexerPeerDescription peer : additionalRules.getPeerList()) {
			if (!peer.hasName() || !peer.hasType()) {
				logger.error(
					"MultiplexerPeerDescription without name or type:\n{}",
					peer);
				continue;
			}
			if (peerNamesToPeerIds.containsKey(peer.getName())) {
				logger.error("Peer name '{}' already exists.", peer.getName());
				continue;
			}
			peerNamesToPeerIds.put(peer.getName(), peer.getType());
		}
		for (MultiplexerMessageDescription msgd : additionalRules.getTypeList()) {

			if (!msgd.hasType()) {
				logger.error("MultiplexerMessageDescription without type:\n{}",
					msgd);
				continue;
			}

			MultiplexerMessageDescription.Builder msgdCopy = MultiplexerMessageDescription
				.newBuilder();
			if (msgd.hasName()) {
				msgdCopy.setName(msgd.getName());
			}
			msgdCopy.setType(msgd.getType());
			
			// Convert the 'to' list using peer' name→ID lookup.
			for (MultiplexerMessageDescription.RoutingRule rRule : msgd
				.getToList()) {

				if (!rRule.hasPeer()) {
					logger.error("RoutingRule without peer name:\n{}", rRule);
					continue;
				}
				if (!peerNamesToPeerIds.containsKey(rRule.getPeer())) {
					logger.error("Unknown peer name: '{}'", rRule.getPeer());
					continue;
				}
				int peerId = peerNamesToPeerIds.get(rRule.getPeer());
				if (rRule.hasPeerType() && rRule.getPeerType() != peerId) {
					logger
						.error(
							"RoutingRule has both peer name and ID but ID is wrong:\n{}",
							rRule);
					continue;
				}

				// Create a copy of rRule with 'peerType' set (and 'peer'
				// cleared ─ it's no longer used).
				msgdCopy.addTo(MultiplexerMessageDescription.RoutingRule
					.newBuilder(rRule).clearPeer().setPeerType(peerId));

			}
			registerMessageDescription(msgdCopy.build());
		}
	}

	public void loadMessageDefinitions(File file) throws ParseException,
		FileNotFoundException, IOException {
		// TODO(findepi)
		MultiplexerRules.Builder rulesBuilder = MultiplexerRules.newBuilder();
		TextFormat.merge(new FileReader(file), rulesBuilder);
		MultiplexerRules additionalRules = rulesBuilder.build();
		loadMessageDefinitions(additionalRules);
	}

	public void loadMessageDefinitionsFromFile(String fileName)
		throws ParseException, FileNotFoundException, IOException {
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
