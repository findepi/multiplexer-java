package multiplexer.jmx;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

import multiplexer.Multiplexer;
import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.Multiplexer.WelcomeMessage;
import multiplexer.constants.Types;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import com.google.protobuf.ByteString;

/**
 * 
 * @author Kasia Findeisen
 *
 */
class ConnectionsManager {

	private static final int UNGROUPPED_CHANNELS = 0;
	private final long instanceId = new Random().nextLong();
	private final int instanceType;
	private ClientBootstrap bootstrap;
	private Map<Integer, ChannelGroup> channelsByType = new HashMap<Integer, ChannelGroup>();
	// mapa instanceId peera --- channel; dodawanie przy odbiorze welcomeMessage

	public ConnectionsManager(final int instanceType) {
		this.instanceType = instanceType;
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(
			Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

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
				synchronized (channelsByType) {
					if (!channelsByType.containsKey(UNGROUPPED_CHANNELS)) {
						channelsByType.put(UNGROUPPED_CHANNELS,
							new DefaultChannelGroup());
					}
					channelsByType.get(UNGROUPPED_CHANNELS).add(channel);
				}
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
		// TODO Auto-generated method stub
		
	}
}
