package multiplexer.jmx.internal;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import multiplexer.jmx.client.ChannelFutureGroup;
import multiplexer.jmx.client.ChannelFutureSet;
import multiplexer.jmx.client.SendingMethod;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.jmx.util.RecentLongPool;
import multiplexer.protocol.Classes;
import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Constants.PeerTypes;
import multiplexer.protocol.Classes.MultiplexerMessage;
import multiplexer.protocol.Classes.WelcomeMessage;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A class for connections management, instantiated by any Classes server's
 * client.
 * 
 * It is responsible for establishing, keeping and closing connections. It
 * handles all system messages accordingly to the Classes's protocol.
 * 
 * Any incoming message which is not a system message is forwarded to the client
 * (client must provide a {@link MessageReceivedListener}). Also, the
 * ConnectionsManager allows the client to send messages asynchronously. The
 * sending method's ({@code sendMessage()}) return type is {@link ChannelFuture}
 * which allows asynchronous and synchronous handling of the initialized sending
 * operation. Apart from the asynchronous sending operation, a method {@code
 * flushAll()} is available for synchronous completion of all initialized
 * sending operations.
 * 
 * @author Kasia Findeisen
 * @author Piotr Findeisen
 */
public class ConnectionsManager {

	private static final Logger logger = LoggerFactory
		.getLogger(ConnectionsManager.class);

	private final long instanceId = new Random().nextLong();
	private final int instanceType;
	private final ClientBootstrap bootstrap;
	private final ConnectionsMap connectionsMap = new ConnectionsMap();
	private MessageReceivedListener messageReceivedListener;
	private final ChannelFutureSet channelFutureSet = new ChannelFutureSet();
	private final Timer idleTimer = new HashedWheelTimer();
	private final Config config = new Config();
	private final RecentLongPool recentMsgIds = new RecentLongPool();

	private final Map<Channel, ChannelFuture> pendingRegistrations = new WeakHashMap<Channel, ChannelFuture>();

	private final Map<Channel, SocketAddress> endpointByChannel = new WeakHashMap<Channel, SocketAddress>();

	/**
	 * Constructs new ConnectionsManager with given type.
	 * 
	 * @param instanceType
	 *            the type of the peer that this {@link ConnectionsManager} will
	 *            represent
	 */
	public ConnectionsManager(final int instanceType) {
		this(instanceType, Executors.newCachedThreadPool());
	}

	/**
	 * Constructs new ConnectionsManager with given type.
	 * 
	 * @param instanceType
	 *            the type of the peer that this {@link ConnectionsManager} will
	 *            represent
	 * @param threadPool
	 *            ExecutorService that will be used to create boss and worker
	 *            threads for handling {@code java.nio}
	 */
	public ConnectionsManager(final int instanceType, ExecutorService threadPool) {
		this(instanceType, threadPool, threadPool);
	}

	/**
	 * Constructs new ConnectionsManager with given type.
	 * 
	 * @param instanceType
	 *            the type of the peer that this {@link ConnectionsManager} will
	 *            represent
	 * @param bossExecutor
	 *            {@link ExecutorService} that will be used to create boss
	 *            thread
	 * @param workerExecutor
	 *            {@link ExecutorService} that will be used to create I/O worker
	 *            threads; it must be able to create at least a thread per CPU
	 *            core
	 */
	public ConnectionsManager(final int instanceType, Executor bossExecutor,
		Executor workerExecutor) {

		this.instanceType = instanceType;
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(
			bossExecutor, workerExecutor);
		bootstrap = new ClientBootstrap(channelFactory);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
			// Encoders
			private RawMessageCodecs.RawMessageEncoder rawMessageEncoder = new RawMessageCodecs.RawMessageEncoder();
			private ProtobufEncoder multiplexerMessageEncoder = new ProtobufEncoder();
			// Decoders
			private ProtobufDecoder multiplexerMessageDecoder = new ProtobufDecoder(
				Classes.MultiplexerMessage.getDefaultInstance());
			// Protocol handler
			private MultiplexerProtocolHandler multiplexerProtocolHandler = new MultiplexerProtocolHandler(
				ConnectionsManager.this);

			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				// Encoders
				pipeline.addLast("rawMessageEncoder", rawMessageEncoder);
				pipeline.addLast("multiplexerMessageEncoder",
					multiplexerMessageEncoder);

				// Decoders
				pipeline.addLast("rawMessageDecoder",
					new RawMessageCodecs.RawMessageFrameDecoder());
				pipeline.addLast("multiplexerMessageDecoder",
					multiplexerMessageDecoder);

				// Heartbits
				pipeline.addLast("idleHandler", new IdleStateHandler(idleTimer,
					Config.INITIAL_READ_IDLE_TIME,
					Config.INITIAL_WRITE_IDLE_TIME, Long.MAX_VALUE,
					TimeUnit.SECONDS));

				// Protocol handler
				pipeline.addLast("multiplexerProtocolHandler",
					multiplexerProtocolHandler);
				return pipeline;
			}
		};

		bootstrap.setPipelineFactory(pipelineFactory);

	}

	public MultiplexerMessage.Builder createMessageBuilder() {
		return initializeMessageBuilder(MultiplexerMessage.newBuilder());
	}

	private MultiplexerMessage.Builder initializeMessageBuilder(
		MultiplexerMessage.Builder message) {
		return message.setId(new Random().nextLong()).setFrom(instanceId)
			.setTimestamp((int) (System.currentTimeMillis() / 1000));
	}

	public MultiplexerMessage createMessage(ByteString message, int type) {
		return createMessageBuilder().setMessage(message).setType(type).build();
	}

	public MultiplexerMessage createMessage(MultiplexerMessage.Builder message) {
		return initializeMessageBuilder(message).build();
	}

	public synchronized ChannelFuture asyncConnect(SocketAddress address) {
		// TODO support for reconnecting; each lost connection (or a connection
		// that could not be estabilished at all) should be retired after
		// specific amout of time
		// TODO send THROUGH_ALL/THROUGH_ALL in case of no connections should
		// also try to reconnect immediately
		// TODO send via(Connection) should try to reconnect to the same
		// address, if connection is lost
		ChannelFuture connectOperation = bootstrap.connect(address);
		final Channel channel = connectOperation.getChannel();
		assert channel != null;
		connectionsMap.addNew(channel);
		endpointByChannel.put(channel, address);

		// TODO make registrationFuture cancellable
		final ChannelFuture registrationFuture = Channels
			.future(channel, false);
		synchronized (pendingRegistrations) {
			pendingRegistrations.put(channel, registrationFuture);
		}

		connectOperation.addListener(new ChannelFutureListener() {

			public void operationComplete(ChannelFuture future)
				throws Exception {
				assert future.isDone();
				if (future.isCancelled())
					return;
				if (!future.isSuccess()) {
					registrationFuture.setFailure(future.getCause());
					synchronized (pendingRegistrations) {
						pendingRegistrations.remove(channel);
					}
					return;
				}

				// send out welcome message
				// TODO(findepi) In case of server's ConnectionsManager, don't
				// send WelcomeMessage before receiving such message from the
				// other party.
				WelcomeMessage welcomeMessage = WelcomeMessage.newBuilder()
					.setType(instanceType).setId(instanceId).build();
				logger.debug("Sending welcome message\n{}", welcomeMessage);
				ByteString message = welcomeMessage.toByteString();
				sendMessage(createMessage(message,
					MessageTypes.CONNECTION_WELCOME), future.getChannel());
			}
		});
		return registrationFuture;
	}

	public void messageReceived(MultiplexerMessage message, Channel channel) {

		if (!recentMsgIds.add(message.getId())) {
			logger.debug("Duplicate message received:\n{}, dropped.", message
				.getId());
			return;
		}

		if (message.getType() == MessageTypes.CONNECTION_WELCOME) {
			WelcomeMessage welcome;
			try {
				welcome = WelcomeMessage.parseFrom(message.getMessage());
			} catch (InvalidProtocolBufferException e) {
				logger.warn("Malformed CONNECTION_WELCOME received.", e);
				close(channel);
				return;
			}
			int peerType = welcome.getType();
			Channel oldChannel = connectionsMap.add(channel, message.getFrom(),
				peerType);
			ChannelFuture registartionFuture;
			synchronized (pendingRegistrations) {
				registartionFuture = pendingRegistrations.remove(channel);
			}
			assert registartionFuture != null;
			registartionFuture.setSuccess();

			channel.getPipeline().replace(
				"idleHandler",
				"idleHandler",
				new IdleStateHandler(idleTimer, config
					.getReadIdleTime(peerType), config
					.getWriteIdleTime(peerType), Long.MAX_VALUE,
					TimeUnit.SECONDS));

			if (oldChannel != null) {
				logger
					.warn("Another CONNECTION_WELCOME received from connected peer; closing previous connection.");
				close(oldChannel);
				return;
			}

		} else if (message.getType() == MessageTypes.HEARTBIT) {
			// Ignored, functionality of HEARTBITs handled by the pipeline.

		} else {
			if (!fireOnMessageReceived(message, channel)) {
				System.err.println("Unhandled message\n" + message);
				// TODO logger?
			}
		}
	}

	/**
	 * Request closing the channel and removing it from connections maps.
	 * 
	 * @param channel
	 *            channel to be closed
	 */
	void close(Channel channel) {
		// TODO remove `channel' from the connections maps
		channel.close();
		connectionsMap.remove(channel);

		SocketAddress endpoint = endpointByChannel.get(channel);
		if (endpoint != null) {
			asyncConnect(endpoint);
		}
	}

	public MessageReceivedListener getMessageReceivedListener() {
		return messageReceivedListener;
	}

	public void setMessageReceivedListener(
		MessageReceivedListener messageReceivedListener) {
		if (messageReceivedListener == null) {
			throw new NullPointerException("messageReceivedListener");
		}
		this.messageReceivedListener = messageReceivedListener;
	}

	private boolean fireOnMessageReceived(MultiplexerMessage message,
		Channel channel) {
		if (messageReceivedListener != null) {
			messageReceivedListener.onMessageReceived(message, new Connection(
				channel));
			return true;
		} else {
			return false;
		}
	}

	private ChannelFuture sendMessage(MultiplexerMessage message,
		Channel channel) {
		ChannelFuture cf = channel.write(message);
		channelFutureSet.add(cf);
		return cf;
	}

	public ChannelFutureGroup sendMessage(MultiplexerMessage message,
		SendingMethod method) throws NoPeerForTypeException {

		if (method == SendingMethod.THROUGH_ONE) {
			Channel channel;
			channel = connectionsMap.getAny(PeerTypes.MULTIPLEXER);
			return new ChannelFutureGroup(sendMessage(message, channel));
		} else if (method == SendingMethod.THROUGH_ALL) {
			Iterator<Channel> channels = connectionsMap
				.getAll(PeerTypes.MULTIPLEXER);
			Channel channel;
			ChannelFutureGroup channelFutureGroup = new ChannelFutureGroup();
			while (channels.hasNext()) {
				channel = channels.next();
				channelFutureGroup.add(sendMessage(message, channel));
			}
			return channelFutureGroup;
		}
		throw new RuntimeException("Unsupported SendingMethod");
	}

	public ChannelFutureGroup sendMessage(MultiplexerMessage message,
		SendingMethod.ViaConnection method) {
		return new ChannelFutureGroup(sendMessage(message, method
			.getConnection().getChannel()));
	}

	public void flushAll() throws InterruptedException {
		copyActiveChannelFutures().await();
	}

	public boolean flushAll(long timeout, TimeUnit unit)
		throws InterruptedException {
		return copyActiveChannelFutures().await(timeout, unit);

	}

	public boolean flushAll(long timeoutMillis) throws InterruptedException {
		return copyActiveChannelFutures().await(timeoutMillis);
	}

	private ChannelFutureGroup copyActiveChannelFutures() {
		synchronized (channelFutureSet) {
			return new ChannelFutureGroup(channelFutureSet);
		}
	}

	void sendHeartbit(Channel channel) {
		MultiplexerMessage.Builder heartbitBuilder = createMessageBuilder();
		MultiplexerMessage heartbit = heartbitBuilder.setType(
			MessageTypes.HEARTBIT).build();
		sendMessage(heartbit, channel);
	}

	public Timer getTimer() {
		return idleTimer;
	}

	public long getInstanceId() {
		return instanceId;
	}
}