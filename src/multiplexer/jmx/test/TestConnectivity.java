/**
 * 
 */
package multiplexer.jmx.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelFuture;

import com.google.protobuf.ByteString;

import junit.framework.TestCase;
import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.constants.Peers;
import multiplexer.constants.Types;
import multiplexer.jmx.AbstractBackend;
import multiplexer.jmx.IncomingMessageData;
import multiplexer.jmx.JmxClient;
import multiplexer.jmx.SendingMethod;
import multiplexer.jmx.exceptions.NoPeerForTypeException;

/**
 * @author Kasia Findeisen
 * 
 */
public class TestConnectivity extends TestCase {

	public void testConnect() throws UnknownHostException {
		JmxClient client = new JmxClient(Peers.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));
	}

	public void testConnectSendReceive() throws UnknownHostException,
		InterruptedException, NoPeerForTypeException {

		// connect
		JmxClient client = new JmxClient(Peers.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));

		// create message
		MultiplexerMessage.Builder builder = MultiplexerMessage.newBuilder();
		builder.setTo(client.getInstanceId()).setType(Types.TEST_REQUEST);
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
	}

	public void testBackend() throws UnknownHostException, InterruptedException, NoPeerForTypeException {
		
		ByteString msgBody = ByteString.copyFromUtf8("Więcej budynió!");

		// create backend
		AbstractBackend backend = new AbstractBackend(Peers.TEST_SERVER) {
			@Override
			protected void handleMessage(MultiplexerMessage message)
				throws Exception {
				// reply with the same message, directly to the sender
				reply(createResponse(message.getType(), message.getMessage()));
			}
		};

		// connect backend and run in new thread
		backend
			.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));
		Thread backendThread = new Thread(backend);
		backendThread.setName("backend main thread");
		backendThread.start();

		// connect
		JmxClient client = new JmxClient(Peers.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));

		// create message
		MultiplexerMessage.Builder builder = MultiplexerMessage.newBuilder();
		builder.setType(Types.TEST_REQUEST).setMessage(msgBody);
		MultiplexerMessage msgSent = client.createMessage(builder);
		assertFalse(msgSent.hasTo());
		
		// send message
		ChannelFuture sendingOperation = client.send(msgSent,
			SendingMethod.THROUGH_ONE);
		sendingOperation.await(3000);
		assertTrue(sendingOperation.isSuccess());

		// receive message
		IncomingMessageData msgData = client.receive(2, TimeUnit.SECONDS);
		assertNotNull(msgData);
		MultiplexerMessage msgReceived = msgData.getMessage();
		assertNotSame(msgSent, msgReceived);
		assertEquals(msgReceived.getType(), msgSent.getType());
		assertEquals(msgReceived.getMessage(), msgBody);
		
		// cleanup
		backend.cancel();
		backendThread.join(3000);
		assertFalse(backendThread.isAlive());
		if (backendThread.isAlive())  {
			backendThread.interrupt();
		}
	}

}
