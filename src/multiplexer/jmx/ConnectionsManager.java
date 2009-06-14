package multiplexer.jmx;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Executors;

import multiplexer.Multiplexer;
import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.Multiplexer.WelcomeMessage;
import multiplexer.constants.Peers;
import multiplexer.constants.Types;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 
 * @author Kasia Findeisen
 * 
 */
class ConnectionsManager {

	private final long instanceId = new Random().nextLong();
	private final int instanceType;
	private ClientBootstrap bootstrap;
	private ConnectionsMap connectionsMap = new ConnectionsMap();
	private MessageReceivedListener messageReceivedListener;

	public ConnectionsManager(final int instanceType) {
		this.instanceType = instanceType;
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors
						.newCachedThreadPool());

		bootstrap = new ClientBootstrap(channelFactory);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
			// Encoders
			private RawMessageCodecs.RawMessageEncoder rawMessageEncoder = new RawMessageCodecs.RawMessageEncoder();
			private ProtobufEncoder multiplexerMessageEncoder = new ProtobufEncoder();
			// Decoders
			private ProtobufDecoder multiplexerMessageDecoder = new ProtobufDecoder(
					Multiplexer.MultiplexerMessage.getDefaultInstance());
			// Protocol handler
			private MultiplexerProtocolHandler multiplexerProtocolHandler = new MultiplexerProtocolHandler(
					ConnectionsManager.this);

			@Override
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

				// Protocol handler
				pipeline.addLast("multiplexerProtocolHandler",
						multiplexerProtocolHandler);
				return pipeline;
			}
		};

		bootstrap.setPipelineFactory(pipelineFactory);

	}

	public MultiplexerMessage createMessage(ByteString message, int type) {
		return createMessage(MultiplexerMessage.newBuilder()
				.setMessage(message).setType(type));
	}

	public MultiplexerMessage createMessage(MultiplexerMessage.Builder message) {
		return message.setId(new Random().nextLong()).setFrom(instanceId)
				.setTimestamp((int) (System.currentTimeMillis() / 1000))
				.build();
	}

	public ChannelFuture asyncConnect(SocketAddress address) {
		ChannelFuture connectOperation = bootstrap.connect(address);
		connectOperation.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future)
					throws Exception {
				assert future.isDone();
				// TODO zrobić, żeby user, gdy dostanie future, nie mógł zrobić
				// getChannel
				SocketChannel channel = (SocketChannel) future.getChannel();
				assert channel != null;
				connectionsMap.addNew(channel);

				// send out welcome message
				System.out.println("sending welcome message"); // TODO
				ByteString message = WelcomeMessage.newBuilder().setType(
						instanceType).setId(instanceId).build().toByteString();
				channel.write(createMessage(message, Types.CONNECTION_WELCOME));
			}
		});
		return connectOperation;
	}

	public void messageReceived(MultiplexerMessage message, Channel channel) {
		if (message.getType() == Types.CONNECTION_WELCOME) {
			ByteString msg = message.getMessage();
			WelcomeMessage welcome;
			try {
				welcome = WelcomeMessage.newBuilder().mergeFrom(msg).build();
				connectionsMap.add(channel, message.getFrom(), welcome
						.getType());
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO Nie akceptować drugiego WELCOME na tym channelu
		} else if (message.getType() == Types.HEARTBIT) {
		} else {
			if (messageReceivedListener != null) {
				messageReceivedListener.onMessageReceived(message,
						new Connection(channel));
			} else {
				System.err.println("Unhandled message\n" + message);
			}
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

	private void sendMessage(MultiplexerMessage message, Channel channel) {
		channel.write(message);
	}

	public int sendMessage(MultiplexerMessage message, SendingMethod method) {
		if (method == SendingMethod.THROUGH_ONE) {
			Channel channel = connectionsMap.getAny(Peers.MULTIPLEXER);
			sendMessage(message, channel);
			return 1;
		} else if (method == SendingMethod.THROUGH_ALL) {
			int count = 0;
			Iterator<Channel> channels = connectionsMap
					.getAll(Peers.MULTIPLEXER);
			Channel channel;
			while (channels.hasNext()) {
				channel = channels.next();
				sendMessage(message, channel);
				count++;
			}
			return count;
		} else {
			sendMessage(message, method.getConnection().getChannel());
			return 1;
		}
	}

}

class IncomingMessageData {
	private MultiplexerMessage message;
	private final Connection connection;

	public IncomingMessageData(MultiplexerMessage message, Connection connection) {
		this.message = message;
		this.connection = connection;
	}

	public MultiplexerMessage getMessage() {
		return message;
	}

	public Connection getConnection() {
		return connection;
	}
}
