package multiplexer.jmx.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import multiplexer.jmx.internal.MultiplexerProtocolHandler;
import multiplexer.jmx.internal.MultiplexerProtocolListener;
import multiplexer.jmx.internal.RawMessageCodecs;
import multiplexer.jmx.internal.RawMessageCodecs.RawMessageFrameDecoder;
import multiplexer.jmx.test.util.JmxServerProvidingTestCase;
import multiplexer.protocol.Constants;
import multiplexer.protocol.Protocol;
import multiplexer.protocol.Protocol.MultiplexerMessage;
import multiplexer.protocol.Protocol.WelcomeMessage;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;

/**
 * 
 * @author Kasia Findeisen
 * @author Piotr Findeisen
 */
public class TestSimpleNettyMultiplexerInteroperability extends
	JmxServerProvidingTestCase {

	// TODO make tests start their own JmxServer on a random free port.
	// If `c' is a value returned from server's bootstrap.bind() then
	// ((InetSocketAddress)c.getLocalAddress()).getPort()
	// is server's port number.

	// FIXME this test doesn't pass with JmxServer

	// TODO write test covering MultiplexerProtocolHandler and other pipeline
	// elements SEPARATELY

	public void testSimpleNettyConnection() throws Exception {
		testSimpleNettyConnection(true);
		testSimpleNettyConnection(false);
	}

	private void testSimpleNettyConnection(boolean useLittleEndianBuffers)
		throws Exception {

		final int PYTHON_TEST_SERVER = TestConstants.PeerTypes.TEST_SERVER;
		final int CONNECTION_WELCOME = Constants.MessageTypes.CONNECTION_WELCOME;
		final int MULTIPLEXER = Constants.PeerTypes.MULTIPLEXER;
		final int PYTHON_TEST_REQUEST = TestConstants.MessageTypes.TEST_REQUEST;

		ChannelFactory factory = new NioClientSocketChannelFactory(Executors
			.newCachedThreadPool(), Executors.newCachedThreadPool());
		SimpleNettyConnection c = new SimpleNettyConnection(factory,
			new InetSocketAddress("localhost", getLocalServerPort()),
			useLittleEndianBuffers);

		// send out invitation
		System.out.println("sending welcome message");
		ByteString message = WelcomeMessage.newBuilder().setType(
			PYTHON_TEST_SERVER).setId(c.getInstanceId()).build().toByteString();
		c.sendMessage(message, CONNECTION_WELCOME).future.await();

		// receive the invitation
		System.out.println("waiting for welcome message");
		MultiplexerMessage mxmsg = c.receive_message();
		System.out.println("validating welcome message");
		assertEquals(mxmsg.getType(), CONNECTION_WELCOME);
		WelcomeMessage peer = WelcomeMessage.parseFrom(mxmsg.getMessage());
		assertEquals(peer.getType(), MULTIPLEXER);
		peer.getId();

		// send a stupid search_query
		ArrayList<Byte> sq = new ArrayList<Byte>();
		for (byte d : "this is a search query with null (\\x00) bytes and other "
			.getBytes())
			sq.add(d);
		for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++)
			sq.add((byte) i);

		Output sqo = ByteString.newOutput();
		for (byte d : sq)
			sqo.write(d);

		System.out.println("sending sample search query");
		long id = c.sendMessage(sqo.toByteString(), PYTHON_TEST_REQUEST).messageId;
		System.out.println("waiting for sample search query");
		mxmsg = c.receive_message();
		System.out.println("validating sample search query");
		assertEquals(mxmsg.getId(), id);
		assertEquals(mxmsg.getType(), PYTHON_TEST_REQUEST);
		assertEquals(mxmsg.getMessage(), sqo.toByteString());

		// send a large search_query
		Random rand = new Random();
		final int size = 1024 * 1024;
		Output lqo = ByteString.newOutput(1024);
		for (int i = 0; i < size; i++)
			lqo.write(rand.nextInt());
		ByteString query = lqo.toByteString();
		assertEquals(query.size(), size);
		System.out.println("sending large search query");
		id = c.sendMessage(query, PYTHON_TEST_REQUEST).messageId;
		System.out.println("waiting for large search query");
		mxmsg = c.receive_message();
		System.out.println("validating large search query");
		assertEquals(mxmsg.getId(), id);
		assertEquals(mxmsg.getType(), PYTHON_TEST_REQUEST);
		assertEquals(mxmsg.getMessage(), query);

		c.close();
		factory.releaseExternalResources();
	}

	public static class SimpleNettyConnection implements
		MultiplexerProtocolListener {

		private final long instanceId;
		private boolean connected = false;
		private boolean connecting = false;
		BlockingQueue<MultiplexerMessage> queue = new LinkedBlockingQueue<MultiplexerMessage>();

		private SocketChannel channel;

		public SimpleNettyConnection(ChannelFactory factory,
			SocketAddress address, boolean useLittleEndianBuffers)
			throws InterruptedException {

			instanceId = new Random().nextLong();
			asyncConnect(factory, address, useLittleEndianBuffers).await();
		}

		private ChannelFuture asyncConnect(ChannelFactory factory,
			SocketAddress address, boolean useLittleEndianBuffers) {
			assert !connected;
			assert !connecting;
			connecting = true;

			ClientBootstrap bootstrap = new ClientBootstrap(factory);

			bootstrap.setOption("tcpNoDelay", true);
			bootstrap.setOption("keepAlive", true);

			ChannelPipeline pipeline = bootstrap.getPipeline();

			// Configuration
			if (useLittleEndianBuffers) {
				pipeline.addFirst("littleEndianEndiannessSetter",
					RawMessageFrameDecoder.LittleEndianEndiannessSettingHandler
						.getInstance());
			}

			// Encoders
			pipeline.addLast("rawMessageEncoder",
				new RawMessageCodecs.RawMessageEncoder());
			pipeline
				.addLast("multiplexerMessageEncoder", new ProtobufEncoder());

			// Decoders
			pipeline.addLast("rawMessageDecoder",
				new RawMessageCodecs.RawMessageFrameDecoder());
			pipeline.addLast("multiplexerMessageDecoder", new ProtobufDecoder(
				Protocol.MultiplexerMessage.getDefaultInstance()));

			// Protocol handler
			pipeline.addLast("multiplexerProtocolHandler",
				new MultiplexerProtocolHandler(this));

			ChannelFuture connectOperation = bootstrap.connect(address);
			connectOperation.addListener(new ChannelFutureListener() {

				public void operationComplete(ChannelFuture future)
					throws Exception {

					System.err.println("connected");
					assert future.isDone();
					assert !connected;
					assert connecting;
					connected = true;
					connecting = false;
					channel = (SocketChannel) future.getChannel();
				}
			});
			return connectOperation;
		}

		long getInstanceId() {
			return instanceId;
		}

		static class SendingResult {
			ChannelFuture future;
			long messageId;

			public SendingResult(ChannelFuture future, long messageId) {
				this.future = future;
				this.messageId = messageId;
			}
		}

		public SendingResult sendMessage(ByteString message, int type)
			throws IOException {
			MultiplexerMessage mxmsg = MultiplexerMessage.newBuilder().setId(
				new Random().nextLong()).setFrom(getInstanceId()).setType(type)
				.setMessage(message).build();

			return new SendingResult(channel.write(mxmsg), mxmsg.getId());
		}

		public MultiplexerMessage receive_message() throws InterruptedException {
			return queue.take();
		}

		private void close() {
			channel.close().awaitUninterruptibly();
		}

		public void channelDisconnected(Channel channel) {
			// TODO Auto-generated method stub
		}

		public void messageReceived(MultiplexerMessage message, Channel channel) {
			boolean offered = queue.offer(message);
			assert offered : "sorry not offered, offiaro";
		}
	}
}
