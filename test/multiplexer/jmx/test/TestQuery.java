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

package multiplexer.jmx.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import multiplexer.jmx.backend.AbstractBackend;
import multiplexer.jmx.client.ConnectException;
import multiplexer.jmx.client.IncomingMessageData;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.jmx.exceptions.OperationFailedException;
import multiplexer.jmx.test.util.JmxServerProvidingTestCase;
import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Protocol.MultiplexerMessage;

import com.google.protobuf.ByteString;

/**
 * @author Kasia Findeisen
 */
public class TestQuery extends JmxServerProvidingTestCase {

	@Test
	public void testQueryBasic() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {
		testQueryBasic(1);
	}

	@Test
	public void testQueryBasicManyTimes() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {
		testQueryBasic(1000);
	}

	public void testQueryBasic(int times) throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {

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
		backend.connect(getLocalServerAddress());
		Thread backendThread = new Thread(backend);
		backendThread.setName("backend main thread");
		backendThread.start();

		// connect
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(getLocalServerAddress());

		// query
		final ByteString queryString = ByteString.copyFromUtf8("Lama ma kota.");
		for (int i = 0; i < times; i++) {
			IncomingMessageData msgData = client.query(queryString,
				TestConstants.MessageTypes.TEST_REQUEST, 2000);

			assert msgData.getMessage().getMessage().equals(queryString);
		}

		// cleanup
		backend.cancel();
		backendThread.join(3000);
		assertFalse(backendThread.isAlive());
		if (backendThread.isAlive()) {
			backendThread.interrupt();
		}

		client.shutdown();
	}

	@Test
	public void testQueryBackendErrorAA() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {
		testQueryBackendError(BackendError.EXCEPTION, BackendError.EXCEPTION);
	}

	@Test
	public void testQueryBackendErrorAB() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {
		testQueryBackendError(BackendError.EXCEPTION, BackendError.REPORT_ERROR);
	}

	@Test
	public void testQueryBackendErrorBA() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {
		testQueryBackendError(BackendError.REPORT_ERROR, BackendError.EXCEPTION);
	}

	@Test
	public void testQueryBackendErrorBB() throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {
		testQueryBackendError(BackendError.REPORT_ERROR,
			BackendError.REPORT_ERROR);
	}

	private enum BackendError {
		EXCEPTION, REPORT_ERROR
	};

	private void testQueryBackendError(final BackendError backend1ErrorType,
		final BackendError backend2ErrorType) throws UnknownHostException,
		OperationFailedException, NoPeerForTypeException, InterruptedException,
		ConnectException {

		// create backend 1
		AbstractBackend backend1 = new AbstractBackend(
			TestConstants.PeerTypes.TEST_SERVER) {
			@Override
			protected void handleMessage(MultiplexerMessage message)
				throws Exception {

				switch (backend1ErrorType) {
				case EXCEPTION:
					throw new Exception("I am the crazy backend.");
				case REPORT_ERROR:
					reply(createResponse(MessageTypes.BACKEND_ERROR));
				}
			}
		};

		// connect backend 1 and run in new thread
		backend1.connect(new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort()));
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
				case EXCEPTION:
					throw new Exception("I am the crazy backend.");
				case REPORT_ERROR:
					reply(createResponse(MessageTypes.BACKEND_ERROR));
				}
			}
		};

		// connect backend 2 and run in new thread
		backend2.connect(new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort()));
		Thread backend2Thread = new Thread(backend2);
		backend2Thread.setName("backend2 main thread");
		backend2Thread.start();

		// connect
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(),
			getLocalServerPort()));

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

		client.shutdown();
	}
}
