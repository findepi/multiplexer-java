/**
 * 
 */
package multiplexer.jmx.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelFuture;

import junit.framework.TestCase;
import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.constants.Peers;
import multiplexer.constants.Types;
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
	
	public void testConnectSendReceive() throws UnknownHostException, InterruptedException, NoPeerForTypeException {
		
		// connect
		JmxClient client = new JmxClient(Peers.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));
		
		// create message
		MultiplexerMessage.Builder builder = MultiplexerMessage.newBuilder();
		builder.setTo(client.getInstanceId()).setType(Types.TEST_REQUEST);
		MultiplexerMessage msgSent = client.createMessage(builder);
		
		// wait until connection is established
		Thread.sleep(1000);
		
		// send message
		ChannelFuture sendingOperation = client.send(msgSent, SendingMethod.THROUGH_ONE);
		sendingOperation.await(3000);
		assertTrue(sendingOperation.isSuccess());
		
		// receive message
		IncomingMessageData msgData = client.receive(2, TimeUnit.SECONDS);
		assertNotNull(msgData);
		MultiplexerMessage msgReceived = msgData.getMessage();
		assertEquals(msgSent, msgReceived);
		assertNotSame(msgSent, msgReceived);
	}

}
