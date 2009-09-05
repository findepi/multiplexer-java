package multiplexer.jmx.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import multiplexer.jmx.backend.AbstractBackend;
import multiplexer.jmx.client.ConnectException;
import multiplexer.jmx.client.IncomingMessageData;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.client.SendingMethod;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.jmx.test.util.JmxServerProvidingTestCase;
import multiplexer.protocol.Protocol.MultiplexerMessage;

import org.jboss.netty.channel.ChannelFuture;

import com.google.protobuf.ByteString;

/**
 * @author Kasia Findeisen
 */
public class TestConnectivity extends JmxServerProvidingTestCase {

	public void testJmxServerStartup() {
		assertTrue(getLocalServerPort() > 0);
	}
	
	public void testConnect() throws UnknownHostException, InterruptedException, ConnectException {
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort()));
		client.shutdown();
	}

	public void testConnectSendReceive() throws UnknownHostException,
		InterruptedException, NoPeerForTypeException, ConnectException {

		// connect
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort()));

		// create message
		MultiplexerMessage.Builder builder = MultiplexerMessage.newBuilder();
		builder.setTo(client.getInstanceId()).setType(
			TestConstants.MessageTypes.TEST_REQUEST);
		MultiplexerMessage msgSent = client.createMessage(builder);

		// send message
		ChannelFuture sendingOperation = client.send(msgSent,
			SendingMethod.THROUGH_ONE);
		sendingOperation.await(3000);
		assertTrue(sendingOperation.isSuccess());

		// receive message
		IncomingMessageData msgData = client.receive(2, TimeUnit.SECONDS);
		assertNotNull(msgData);
		MultiplexerMessage msgReceived = msgData.getMessage();
		assertEquals(msgSent, msgReceived);
		assertNotSame(msgSent, msgReceived);

		client.shutdown();
	}

	public void testBackend() throws UnknownHostException,
		InterruptedException, NoPeerForTypeException, ConnectException {

		ByteString msgBody = ByteString.copyFromUtf8("Więcej budynió!");

		// create backend
		AbstractBackend backend = new AbstractBackend(
			TestConstants.PeerTypes.TEST_SERVER) {
			@Override
			protected void handleMessage(MultiplexerMessage message)
				throws Exception {
				// reply with the same message, directly to the sender
				reply(createResponse(message.getType(), message.getMessage()));
			}
		};

		// connect backend and run in new thread
		backend.connect(new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort()));
		Thread backendThread = new Thread(backend);
		backendThread.setName("backend main thread");
		backendThread.start();

		// connect
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort()));

		// create message
		MultiplexerMessage.Builder builder = MultiplexerMessage.newBuilder();
		builder.setType(TestConstants.MessageTypes.TEST_REQUEST).setMessage(
			msgBody);
		MultiplexerMessage msgSent = client.createMessage(builder);
		assertFalse(msgSent.hasTo());

		// send message
		ChannelFuture sendingOperation = client.send(msgSent,
			SendingMethod.THROUGH_ONE);
		sendingOperation.await(1, TimeUnit.SECONDS);
		assertTrue(sendingOperation.isSuccess());

		// receive message
		IncomingMessageData msgData = client.receive(1, TimeUnit.SECONDS);
		assertNotNull(msgData);
		MultiplexerMessage msgReceived = msgData.getMessage();
		assertNotSame(msgSent, msgReceived);
		assertEquals(msgReceived.getType(), msgSent.getType());
		assertEquals(msgReceived.getMessage(), msgBody);

		// cleanup
		backend.cancel();
		backendThread.join(3000);
		assertFalse(backendThread.isAlive());
		if (backendThread.isAlive()) {
			backendThread.interrupt();
		}

		client.shutdown();
	}
}
