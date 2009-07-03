package multiplexer.jmx.test.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import multiplexer.Multiplexer;
import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.Multiplexer.WelcomeMessage;
import multiplexer.jmx.RawMessageCodecs;

import org.jboss.netty.bootstrap.ClientBootstrap;
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

public class SimpleNettyConnection {

	private final long instanceId;
	private boolean connected = false;
	private boolean connecting = false;
	BlockingQueue<MultiplexerMessage> queue = new LinkedBlockingQueue<MultiplexerMessage>();

	private SocketChannel channel;

	public SimpleNettyConnection() {
		instanceId = new Random().nextLong();
	}

	public SimpleNettyConnection(ChannelFactory factory, SocketAddress address)
		throws InterruptedException {

		this();
		asyncConnect(factory, address).await();
	}

	public ChannelFuture asyncConnect(ChannelFactory factory,
		SocketAddress address) {
		assert !connected;
		assert !connecting;
		connecting = true;

		ClientBootstrap bootstrap = new ClientBootstrap(factory);

		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		ChannelPipeline pipeline = bootstrap.getPipeline();

		// Encoders
		pipeline.addLast("rawMessageEncoder",
			new RawMessageCodecs.RawMessageEncoder());
		pipeline.addLast("multiplexerMessageEncoder", new ProtobufEncoder());

		// Decoders
		pipeline.addLast("rawMessageDecoder",
			new RawMessageCodecs.RawMessageFrameDecoder());
		pipeline.addLast("multiplexerMessageDecoder", new ProtobufDecoder(
			Multiplexer.MultiplexerMessage.getDefaultInstance()));

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

	class SendingResult {
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

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
		InterruptedException {

		final int PYTHON_TEST_SERVER = 106;
		final int CONNECTION_WELCOME = 2;
		final int MULTIPLEXER = 1;
		final int PYTHON_TEST_REQUEST = 110;

		ChannelFactory factory = new NioClientSocketChannelFactory(Executors
			.newCachedThreadPool(), Executors.newCachedThreadPool());
		SimpleNettyConnection c = new SimpleNettyConnection(factory,
			new InetSocketAddress("localhost", 1980));

		// send out invitation
		System.out.println("sending welcome message");
		ByteString message = WelcomeMessage.newBuilder().setType(
			PYTHON_TEST_SERVER).setId(c.getInstanceId()).build().toByteString();
		c.sendMessage(message, CONNECTION_WELCOME).future.await();

		// receive the invitation
		System.out.println("waiting for welcome message");
		MultiplexerMessage mxmsg = c.receive_message();
		System.out.println("validating welcome message");
		assert mxmsg.getType() == CONNECTION_WELCOME;
		WelcomeMessage peer = WelcomeMessage.parseFrom(mxmsg.getMessage());
		assert peer.getType() == MULTIPLEXER;
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
		assert mxmsg.getId() == id;
		assert mxmsg.getType() == PYTHON_TEST_REQUEST;
		assert mxmsg.getMessage().equals(sqo.toByteString());

		// send a large search_query
		System.out.println("before seed");
		byte[] random = new SecureRandom().generateSeed(100);
		System.out.println("after seed");
		ByteString query = ByteString.copyFrom(random);
		System.out.println("sending large search query");
		id = c.sendMessage(query, PYTHON_TEST_REQUEST).messageId;
		System.out.println("waiting for large search query");
		mxmsg = c.receive_message();
		System.out.println("validating large search query");
		assert mxmsg.getId() == id;
		assert mxmsg.getType() == PYTHON_TEST_REQUEST;
		assert mxmsg.getMessage().equals(sqo.toByteString());

		c.close();
		factory.releaseExternalResources();
	}

}
