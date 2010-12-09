// Copyright 2009 Warsaw University, Faculty of Physics
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package multiplexer.jmx.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import multiplexer.jmx.client.Connection;
import multiplexer.jmx.client.SendingMethod;
import multiplexer.jmx.exceptions.NoPeerForPeerIdException;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.jmx.internal.ByteCountingHandler;
import multiplexer.jmx.internal.ConnectionsManager;
import multiplexer.jmx.internal.MessageCountingHandler;
import multiplexer.jmx.internal.MessageReceivedListener;
import multiplexer.jmx.util.LongDeltaCounter;
import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Constants.PeerTypes;
import multiplexer.protocol.Protocol.BackendForPacketSearch;
import multiplexer.protocol.Protocol.DeliveryError;
import multiplexer.protocol.Protocol.MultiplexerMessage;
import multiplexer.protocol.Protocol.MultiplexerMessageDescription;
import multiplexer.protocol.Protocol.MultiplexerPeerDescription;
import multiplexer.protocol.Protocol.MultiplexerRules;
import multiplexer.protocol.Protocol.MultiplexerMessageDescription.RoutingRule;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;

/**
 * A pure-Java implementation of a Multiplexer server.
 * 
 * @author Piotr Findeisen
 */
public class JmxServer implements MessageReceivedListener, Runnable {

	public static final String UNKOWN_TYPE_NAME = "unknown";
	public static final String UNNAMED_TYPE_NAME = "unnamed";

	private static final Logger logger = LoggerFactory
		.getLogger(JmxServer.class);

	private final Object lock = new Object();

	protected ConnectionsManager connectionsManager;
	protected SocketAddress serverAddress;
	private SocketAddress serverEffectiveAddress;

	protected Map<String, Integer> peerTypeNamesToPeerTypeIds = Maps
		.newHashMap();
	protected Map<Integer, MultiplexerMessageDescription> messageTypeIdsToDescription = Maps
		.newHashMap();

	protected long transferUpdateIntervalMillis = 1000;

	private volatile boolean started = false;
	private volatile boolean running = true;
	private volatile Thread serverThread;

	private ServerChannelPipelineFactory channelPipelineFactory;

	private volatile int localPort = -1;

	private ByteString multiplexerPassword;

	/**
	 * Constructs the server that will listen for incoming connections on the
	 * given {@link SocketAddress}.
	 * 
	 * @param serverAddress
	 */
	public JmxServer(SocketAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Get the port number on which the server listens for incoming connections.
	 * Especially useful when the server was created using {@link SocketAddress}
	 * with port {@code 0}. Use this function to retrieve the actual port number
	 * assigned to this server.
	 * 
	 * If the server has not been yet started, the returned value is {@code -1}.
	 * 
	 * @return local port number
	 */
	public int getLocalPort() {
		return localPort;
	}

	/**
	 * Run the server. Before calling this function the server does not try to
	 * open any sockets and does not provide {@code localPort} information.
	 */
	public void run() {

		synchronized (lock) {
			if (serverThread != null)
				throw new RuntimeException(JmxServer.class.getSimpleName()
					+ " started twice.");
			serverThread = Thread.currentThread();
		}
		serverThread.setName(JmxServer.class.getSimpleName());

		logger.debug("starting {} @ {}", JmxServer.class.getSimpleName(),
			serverAddress);

		try {
			// Configure the server.
			ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors
					.newCachedThreadPool());
			ServerBootstrap bootstrap = new ServerBootstrap(factory);

			// initialize the connectionsManager
			connectionsManager = new ConnectionsManager(PeerTypes.MULTIPLEXER,
				bootstrap);
			channelPipelineFactory = new ServerChannelPipelineFactory(bootstrap
				.getPipelineFactory());
			bootstrap.setPipelineFactory(channelPipelineFactory);
			connectionsManager.setMessageReceivedListener(this);
			if (multiplexerPassword != null)
				connectionsManager.setMultiplexerPassword(multiplexerPassword);

			// Bind & start the server.
			Channel listeningChannel = bootstrap.bind(serverAddress);
			connectionsManager.channelOpen(listeningChannel);
			if (listeningChannel.getLocalAddress() instanceof InetSocketAddress) {
				localPort = ((InetSocketAddress) listeningChannel
					.getLocalAddress()).getPort();
			}

			started = true;
			synchronized (this) {
				// Someone may be waiting for hasStarted() to become true (e.g.
				// until localPort information becomes available -- for example
				// JmxServerRunner).
				this.notifyAll();
			}

			serverEffectiveAddress = serverAddress;
			if (serverAddress instanceof InetSocketAddress
				&& ((InetSocketAddress) serverAddress).getPort() == 0) {
				assert localPort > 0;
				// InetSocketAddress.getHostName() may cause IP resolving and
				// this may last e.g. 4 seconds in very common case of 0.0.0.0.
				// We delay this operation after notifying listeners about
				// server start and making localPort information accessible to
				// them.
				String hostName = getHostString((InetSocketAddress) serverAddress);
				serverEffectiveAddress = InetSocketAddress.createUnresolved(
					hostName, localPort);
			}
			logger.info("started {} @ {}", JmxServer.class.getSimpleName(),
				serverEffectiveAddress);

			loopPrintingStatistics();

		} finally {
			try {
				connectionsManager.shutdown();
			} catch (InterruptedException e) {
				logger.warn("Exception ignored", e);
			} finally {
				synchronized (lock) {
					serverThread = null;
				}
			}
		}
	}

	private static String getHostString(InetSocketAddress address) {
		String hostAndPort = address.toString();
		return hostAndPort.substring(0, hostAndPort.lastIndexOf(':'));
	}

	/**
	 * Returns {@code true} if and only if the server was started using call to
	 * {@link #run()}. The server must have been started before
	 * {@link #getLocalPort()} can be used. If you delegated call to this method
	 * to a separate thread, use {@link Object#wait} to wait for the server to
	 * start.
	 * 
	 * Example
	 * 
	 * <pre>
	 * JmxServer server = new JmxServer(new InetSocketAddress(&quot;0.0.0.0&quot;, 0));
	 * // further initialize the server
	 * new Thread(server).start();
	 * synchronized (server) {
	 * 	if (!server.hasStarted())
	 * 		server.wait();
	 * }
	 * System.out.println(&quot;server local port is &quot; + server.getLocalPort());
	 * </pre>
	 */
	public boolean hasStarted() {
		return started;
	}

	private void loopPrintingStatistics() {
		LongDeltaCounter bytesIn = new LongDeltaCounter();
		LongDeltaCounter bytesOut = new LongDeltaCounter();
		LongDeltaCounter messagesIn = new LongDeltaCounter();
		LongDeltaCounter messagesOut = new LongDeltaCounter();
		LongDeltaCounter time = new LongDeltaCounter(System.currentTimeMillis());

		final ByteCountingHandler bytesCounter = channelPipelineFactory
			.getByteCountingHandler();
		final MessageCountingHandler messageCounter = channelPipelineFactory
			.getMessageCountingHandler();

		while (running) {
			try {
				Thread.sleep(transferUpdateIntervalMillis);
			} catch (InterruptedException e) {
				logger.info("loop interrupted, server is going to shut down");
				break;
			}

			// timeDelta is double so that division results are not rounded.
			double timeDelta = time.deltaTo(System.currentTimeMillis()) * 1.0
				/ TimeUnit.SECONDS.toMillis(1);
			if (timeDelta <= 0) {
				// system clock changed?
				continue;
			}

			System.err.format("IN: %s %9.2f msg/s      OUT: %s %9.2f msg/s%n",
				renderBytesPerSecondCount(bytesIn.deltaTo(bytesCounter
					.getBytesInCount())
					/ timeDelta), messagesIn.deltaTo(messageCounter
					.getMessagesInCount())
					/ timeDelta, renderBytesPerSecondCount(bytesOut
					.deltaTo(bytesCounter.getBytesOutCount())
					/ timeDelta), messagesOut.deltaTo(messageCounter
					.getMessagesOutCount())
					/ timeDelta);
		}
	}

	private static String renderBytesPerSecondCount(double bytes) {
		assert bytes >= 0;
		Formatter formatter = new Formatter();
		if (bytes >= 1024 * 1024) {
			formatter.format("%7.2f MiB/s", bytes / 1024 / 1024);
		} else if (bytes >= 1024) {
			formatter.format("%7.2f KiB/s", bytes / 1024);
		} else {
			formatter.format("%7.2f B/s  ", bytes);
		}
		return formatter.toString();
	}

	/**
	 * Stop the server.
	 */
	public void shutdown() {
		running = false;
		logger.info("stopping {} @ {}", JmxServer.class.getSimpleName(),
			serverEffectiveAddress);

		Thread serverThread;
		synchronized (lock) {
			serverThread = this.serverThread;
			if (serverThread != null) {
				serverThread.interrupt();
			}
		}
		if (serverThread == null) {
			// Perform the IO outside of synchronized block.
			logger.warn("There is no serverThread to be shut down.");
		}
	}

	protected MultiplexerMessageDescription registerMessageDescription(
		MultiplexerMessageDescription description) {
		assert description.hasType();
		return messageTypeIdsToDescription.put(description.getType(),
			description);
	}

	/**
	 * Load routing rules from an instance of {@link MultiplexerRules}.
	 */
	public void loadMessageDefinitions(MultiplexerRules additionalRules) {
		for (MultiplexerPeerDescription peerDesc : additionalRules
			.getPeerList()) {
			if (!peerDesc.hasName() || !peerDesc.hasType()) {
				logger.error(
					"MultiplexerPeerDescription without name or type:\n{}",
					peerDesc);
				continue;
			}
			if (peerTypeNamesToPeerTypeIds.containsKey(peerDesc.getName())) {
				logger.error("Peer name '{}' already exists.", peerDesc
					.getName());
				continue;
			}
			peerTypeNamesToPeerTypeIds.put(peerDesc.getName(), peerDesc
				.getType());
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

				int peerId;
				if (rRule.hasPeer()) {
					// We have peer name specified.
					if (!peerTypeNamesToPeerTypeIds
						.containsKey(rRule.getPeer())) {
						logger
							.error("Unknown peer name: '{}'", rRule.getPeer());
						continue;
					}
					peerId = peerTypeNamesToPeerTypeIds.get(rRule.getPeer());
					if (rRule.hasPeerType() && rRule.getPeerType() != peerId) {
						logger
							.error(
								"RoutingRule has both peer name and ID but ID is wrong:\n{}",
								rRule);
						continue;
					}
				} else if (rRule.hasPeerType()) {
					// We don't have peer name but we have peer_type.
					peerId = rRule.getPeerType();
				} else {
					// Oops, we have neither peer name nor type.
					logger.error(
						"RoutingRule without peer name or peer_type:\n{}",
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

	/**
	 * Load routing rules from a {@link File}.
	 */
	public void loadMessageDefinitions(File file) throws ParseException,
		FileNotFoundException, IOException {
		MultiplexerRules.Builder rulesBuilder = MultiplexerRules.newBuilder();
		TextFormat.merge(new FileReader(file), rulesBuilder);
		MultiplexerRules additionalRules = rulesBuilder.build();
		loadMessageDefinitions(additionalRules);
	}

	/**
	 * Load routing rules from a file named {@code fileName}.
	 */
	public void loadMessageDefinitionsFromFile(String fileName)
		throws ParseException, FileNotFoundException, IOException {
		loadMessageDefinitions(new File(fileName));
	}

	public void onMessageReceived(MultiplexerMessage message,
		Connection connection) {

		logger.debug("message received, type=" + message.getType());
		logger.trace("message received\n{}\n", message);

		// routing based on to
		if (message.hasTo()) {
			scheduleByTo(connection, message);
			return;
		}

		// routing based on overridden rules
		if (message.getOverrideRrulesCount() != 0) {
			assert message.getOverrideRrulesCount() > 0;
			scheduleByRoutingRules(connection, message, message
				.getOverrideRrulesList());
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
				connectionsManager.sendMessage(response, SendingMethod
					.via(connection));
			}
			break;

		case MessageTypes.BACKEND_ERROR:
		case MessageTypes.DELIVERY_ERROR:
		case MessageTypes.CONNECTION_WELCOME:
		case MessageTypes.HEARTBIT:
		case MessageTypes.REQUEST_RECEIVED:
			logger.warn("don't know what to do with message type {} ({})",
				message.getType(), getMessageTypeName(message.getType()));
			break;

		case MessageTypes.BACKEND_FOR_PACKET_SEARCH:
			try {
				BackendForPacketSearch backendSearchMessage = BackendForPacketSearch
					.parseFrom(message.getMessage());
				MultiplexerMessageDescription msgDesc = messageTypeIdsToDescription
					.get(backendSearchMessage.getPacketType());
				if (msgDesc == null || msgDesc.getToCount() == 0) {
					logger.warn("BACKEND_FOR_PACKET_SEARCH msg type "
						+ backendSearchMessage.getPacketType()
						+ " has no routing rules");
					if (isReportDeliveryErrorRequested(message)) {
						reportDeliveryError(connection, message,
							createDeliveryError(message).setIsKnownType(false));
					}
				} else {
					RoutingRule routingRule = msgDesc.getTo(0);
					routingRule = RoutingRule.newBuilder(routingRule).setWhom(
						RoutingRule.Whom.ALL).setReportDeliveryError(true)
						.setIncludeOriginalPacketInReport(false).build();
					List<RoutingRule> ruleSingleton = new ArrayList<RoutingRule>(
						1);
					ruleSingleton.add(routingRule);
					scheduleByRoutingRules(connection, message, ruleSingleton);
				}
			} catch (InvalidProtocolBufferException e) {
				logger.warn("Malformed BACKEND_FOR_PACKET_SEARCH", e);
				if (isReportDeliveryErrorRequested(message)) {
					reportDeliveryError(connection, message,
						createDeliveryError(message).setIsKnownType(false));
				}
			}
			break;

		default:
			if (message.getType() > MessageTypes.MAX_MULTIPLEXER_META_PACKET) {
				MultiplexerMessageDescription msgDesc = messageTypeIdsToDescription
					.get(message.getType());
				if (msgDesc != null) {
					scheduleByRoutingRules(connection, message, msgDesc
						.getToList());
					break;
				}
			}
			if (logger.isWarnEnabled()) {
				logger.warn("don't know what to do with message type {} ({})",
					message.getType(), getMessageTypeName(message.getType()));
			}
			break;
		}
	}

	private void scheduleByRoutingRules(Connection from,
		MultiplexerMessage message, List<RoutingRule> routingRules) {
		DeliveryError.Builder deliveryError = null;
		if (isReportDeliveryErrorRequested(message))
			deliveryError = createDeliveryError(message);

		for (RoutingRule rule : routingRules) {
			try {
				connectionsManager.sendMessage(message, SendingMethod.via(rule
					.getPeerType(), rule.getWhom()));
			} catch (NoPeerForTypeException e) {
				e.printStackTrace();
				if (deliveryError != null)
					deliveryError.addFailedType(rule.getPeerType());
			}
		}
		if (deliveryError != null)
			reportDeliveryError(from, message, deliveryError);
	}

	/**
	 * Send {@code message} to a client directly connected to this server and
	 * having ID {@code message.getType()}.
	 * 
	 * @param message
	 *            to be sent
	 */
	void scheduleByTo(Connection from, MultiplexerMessage message) {
		assert message.hasTo();
		try {
			connectionsManager.sendMessage(message, SendingMethod.via(message
				.getTo()));
		} catch (NoPeerForPeerIdException e) {
			logger.warn("message #{} to {} while it's not connected", message
				.getId(), message.getTo());
			reportDeliveryError(from, message, message.getTo());
		}
	}

	boolean isReportDeliveryErrorRequested(MultiplexerMessage message) {
		return message.getReportDeliveryError() && message.getFrom() != 0
			&& message.getFrom() != connectionsManager.getInstanceId();
	}

	/**
	 * Report that delivery of {@code message} to peer {@code peerId} failed.
	 * Suppress the report if the sender of the {@code message} did not request
	 * it ({@code message.getReportDeliveryError()} returns false).
	 */
	void reportDeliveryError(Connection from, MultiplexerMessage message,
		long peerId) {
		if (!isReportDeliveryErrorRequested(message))
			return;
		reportDeliveryError(from, message, createDeliveryError(message)
			.setFailedTo(peerId));
	}

	DeliveryError.Builder createDeliveryError(MultiplexerMessage message) {
		DeliveryError.Builder deliveryError = DeliveryError.newBuilder();
		if (message.getIncludeOriginalPacketInReport())
			deliveryError.setOriginalMessage(message);
		return deliveryError;
	}

	void reportDeliveryError(Connection from, MultiplexerMessage message,
		DeliveryError deliveryError) {
		assert isReportDeliveryErrorRequested(message);
		MultiplexerMessage errorMessage = connectionsManager
			.createMessageBuilder().setTo(message.getFrom())
			.setReportDeliveryError(false).setType(MessageTypes.DELIVERY_ERROR)
			.setMessage(deliveryError.toByteString()).setReferences(
				message.getId()).setWorkflow(message.getWorkflow()).build();
		connectionsManager.sendMessage(errorMessage, SendingMethod.via(from));
	}

	void reportDeliveryError(Connection from, MultiplexerMessage message,
		DeliveryError.Builder deliveryError) {
		reportDeliveryError(from, message, deliveryError.build());
	}

	String getMessageTypeName(int type) {
		String name;
		name = MessageTypes.instance.getConstantsNames().get(type);
		if (name != null)
			return name;
		MultiplexerMessageDescription msgDesc = messageTypeIdsToDescription
			.get(type);
		if (msgDesc != null) {
			if (msgDesc.hasName())
				return msgDesc.getName();
			else
				return UNNAMED_TYPE_NAME;
		}
		return UNKOWN_TYPE_NAME;
	}

	/**
	 * Returns a value of the {@code transferUpdateIntervalMillis}.
	 */
	public long getTransferUpdateIntervalMillis() {
		return transferUpdateIntervalMillis;
	}

	/**
	 * Sets the {@code transferUpdateIntervalMillis}.
	 */
	public void setTransferUpdateIntervalMillis(
		long transferUpdateIntervalMillis) {
		this.transferUpdateIntervalMillis = transferUpdateIntervalMillis;
	}

	/**
	 * Returns the {@code SocketAddress} used to construct this server instance.
	 * The returned value does not reflect automatic port number allocation that
	 * occurs if a {@link SocketAddress} has port value of 0.
	 */
	public SocketAddress getServerAddress() {
		return serverAddress;
	}

	/**
	 * Sets the {@link SocketAddress} to be used for listening for incoming
	 * connections. Calling this method after calling run has no effect.
	 * 
	 * @param serverAddress
	 */
	public void setServerAddress(SocketAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	public ByteString getMultiplexerPassword() {
		return multiplexerPassword;
	}

	/**
	 * Sets the {@code multiplexerPassword} used by the server. This can be
	 * called only before a call to {@link #run}.
	 * 
	 * @param multiplexerPassword
	 */
	public void setMultiplexerPassword(ByteString multiplexerPassword) {
		this.multiplexerPassword = multiplexerPassword;
	}

	/**
	 * Starts a {@link JmxServer}. To be used from command line and in scripts.
	 */
	public static void main(String[] args) throws ParseException,
		FileNotFoundException, IOException {

		Options options = new Options();
		CmdLineParser optionsParser = new CmdLineParser(options);
		try {
			optionsParser.parseArgument(args);
		} catch (CmdLineException e) {
			usage(e.getMessage(), optionsParser);
			System.exit(1);
		}

		// initialize the server
		JmxServer server = new JmxServer(new InetSocketAddress(
			options.localHost, options.localPort));
		for (String fileName : options.rulesFiles) {
			server.loadMessageDefinitionsFromFile(fileName);
		}
		server
			.setTransferUpdateIntervalMillis(options.transferUpdateIntervalMillis);

		server.run();
	}

	private static void usage(String error, CmdLineParser optionsParser) {
		System.err.println(error);
		System.err.println("java " + JmxServer.class.getName()
			+ " [options...] <multiplexer rules file>");
		System.err
			.println("java -jar ....jar server [options...] <multiplexer rules file>");
		System.err.println();
		System.err.println("Available options are listed below.");
		optionsParser.printUsage(System.err);
	}
}
