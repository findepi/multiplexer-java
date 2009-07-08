package multiplexer.jmx.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import multiplexer.jmx.backend.AbstractBackend;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.jmx.exceptions.OperationFailedException;
import multiplexer.jmx.internal.IncomingMessageData;
import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Classes.MultiplexerMessage;

import com.google.protobuf.ByteString;

import junit.framework.TestCase;

/**
 * @author Kasia Findeisen
 * 
 */
public class TestQuery extends TestCase {
	
	// TODO cleanup client after every test case
	
	public void testQueryBasic() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException {

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
		backend
			.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));
		Thread backendThread = new Thread(backend);
		backendThread.setName("backend main thread");
		backendThread.start();

		// connect
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));

		// query
		IncomingMessageData msgData = client.query(ByteString
			.copyFromUtf8("Lama ma kota."),
			TestConstants.MessageTypes.TEST_REQUEST, 2000);

		assertEquals(msgData.getMessage().getMessage(), ByteString
			.copyFromUtf8("Lama ma kota."));

		// cleanup
		backend.cancel();
		backendThread.join(3000);
		assertFalse(backendThread.isAlive());
		if (backendThread.isAlive()) {
			backendThread.interrupt();
		}
	}

	public void testQueryBackendErrorAA() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException {
		testQueryBackendError(0, 0);
	}

	public void testQueryBackendErrorAB() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException {
		testQueryBackendError(0, 1);
	}

	public void testQueryBackendErrorBA() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException {
		testQueryBackendError(1, 0);
	}

	public void testQueryBackendErrorBB() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException {
		testQueryBackendError(1, 1);
	}

	private void testQueryBackendError(final int backend1ErrorType,
		final int backend2ErrorType) throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException {

		// create backend 1
		AbstractBackend backend1 = new AbstractBackend(
			TestConstants.PeerTypes.TEST_SERVER) {
			@Override
			protected void handleMessage(MultiplexerMessage message)
				throws Exception {

				switch (backend1ErrorType) {
				case 0:
					throw new Exception("I am the crazy backend.");
				case 1:
					reply(createResponse(MessageTypes.BACKEND_ERROR));
				}
			}
		};

		// connect backend 1 and run in new thread
		backend1
			.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));
		Thread backend1Thread = new Thread(backend1);
		backend1Thread.setName("backend1 main thread");
		backend1Thread.start();

		// create backend 2
		AbstractBackend backend2 = new AbstractBackend(
			TestConstants.PeerTypes.TEST_SERVER) {
			@Override
			protected void handleMessage(MultiplexerMessage message)
				throws Exception {
				switch (backend1ErrorType) {
				case 0:
					throw new Exception("I am the crazy backend.");
				case 1:
					reply(createResponse(MessageTypes.BACKEND_ERROR));
				}
			}
		};

		// connect backend 2 and run in new thread
		backend2
			.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));
		Thread backend2Thread = new Thread(backend2);
		backend2Thread.setName("backend2 main thread");
		backend2Thread.start();

		// connect
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));

		// query
		IncomingMessageData msgData = client.query(ByteString
			.copyFromUtf8("Lama ma kota."),
			TestConstants.MessageTypes.TEST_REQUEST, 2000);

		assertEquals(msgData.getMessage().getType(), MessageTypes.BACKEND_ERROR);

		// cleanup
		backend1.cancel();
		backend1Thread.join(3000);
		assertFalse(backend1Thread.isAlive());
		if (backend1Thread.isAlive()) {
			backend1Thread.interrupt();
		}
		backend2.cancel();
		backend2Thread.join(3000);
		assertFalse(backend2Thread.isAlive());
		if (backend2Thread.isAlive()) {
			backend2Thread.interrupt();
		}
	}
}
