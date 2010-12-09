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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import multiplexer.jmx.backend.AbstractBackend;
import multiplexer.jmx.client.ConnectException;
import multiplexer.jmx.client.IncomingMessageData;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.client.SendingMethod;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.jmx.exceptions.OperationFailedException;
import multiplexer.jmx.server.JmxServer;
import multiplexer.jmx.test.TestConstants.MessageTypes;
import multiplexer.jmx.test.TestConstants.PeerTypes;
import multiplexer.jmx.test.util.JmxServerRunner;
import multiplexer.protocol.Protocol.MultiplexerMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat.ParseException;

/**
 * @author Piotr Findeisen
 */
public class TestThreadsShutdown {

	private static final Logger logger = LoggerFactory.getLogger(TestThreadsShutdown.class);

	private int initialActiveCount;
	private Thread[] threads;

	@Before
	public void setUp() throws Exception {
		System.gc();
		Thread.sleep(50);
		System.gc();
		Thread.sleep(50);

		initialActiveCount = Thread.activeCount();
		threads = new Thread[initialActiveCount + 1];
		int numberCopied = Thread.enumerate(threads);
		assertEquals(initialActiveCount, numberCopied);
	}

	@After
	public void tearDown() throws Exception {
		System.gc();
		Thread.sleep(50);
		System.gc();
		Thread.sleep(50);

		int activeCount = Thread.activeCount();
		Thread[] threads = new Thread[activeCount + 1];
		int numberCopied = Thread.enumerate(threads);
		if (numberCopied != activeCount) {
			logger.error("# of threds changed from " + activeCount + " to " + numberCopied + " during enumeration.");
		}

		Set<Thread> activeThreads = new HashSet<Thread>(Arrays.asList(threads));
		for (Thread th : this.threads) {
			if (th == null)
				continue;
			if (!activeThreads.contains(th))
				fail("Thread " + th + " is no longer alive (active now = " + activeCount + ", at startup = " + initialActiveCount + ")");
		}

		Set<Thread> activeAtStartup = new HashSet<Thread>(Arrays.asList(this.threads));
		for (Thread th : threads) {
			if (th == null)
				continue;
			if (!activeAtStartup.contains(th))
				fail("Thread " + th + " was not active at startup but is active now (active now = " + activeCount + ", at startup = "
					+ initialActiveCount + ")");
		}

		// assertEquals("this should not be triggered", initialActiveCount,
		// activeCount);
	}

	@Test
	public void testNothing() {
	}

	@Test
	public void testJmxClientNoConnections() throws InterruptedException {
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		client.shutdown();
	}

	@Test
	public void testJmxClientConnecting() throws ConnectException, InterruptedException {
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		try {
			client.connect(new InetSocketAddress("127.0.0.1", 1));
			fail("You should not run multiplexer (or anything else) on TCP Port Service Multiplexer (TCPMUX) port.");
		} catch (ConnectException e) {
			// That's OK.
		}
		client.shutdown();
	}

	@Test
	public void testJmxClientAsyncConnectingNoWait() throws ConnectException, InterruptedException {
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		client.asyncConnect(new InetSocketAddress("127.0.0.1", 1));
		client.shutdown();
	}

	@Test
	public void testJmxClientAsyncConnecting() throws ConnectException, InterruptedException {
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		client.asyncConnect(new InetSocketAddress("127.0.0.1", 1));
		Thread.sleep(100);
		client.shutdown();
	}

	@Test
	public void testJmxServerRunner() throws Exception {
		JmxServerRunner serverRunner = new JmxServerRunner();
		serverRunner.start();
		serverRunner.stop();
	}

	@Test
	public void testJmxServerNoDaemon() throws InterruptedException {
		testJmxServer(false);
	}

	@Test
	public void testJmxServerDaemon() throws InterruptedException {
		testJmxServer(true);
	}

	@Test
	public void testJmxServerDefaultDaemon() throws InterruptedException {
		testJmxServer(null);
	}

	public void testJmxServer(Boolean daemon) throws InterruptedException {
		JmxServer server = new JmxServer(new InetSocketAddress(0));
		Thread serverThread = new Thread(server);
		if (daemon != null)
			serverThread.setDaemon(daemon.booleanValue());
		serverThread.start();
		synchronized (server) {
			if (!server.hasStarted()) {
				server.wait();
			}
			assertTrue("server did not start", server.hasStarted());
		}
		server.shutdown();
		serverThread.join(TimeUnit.SECONDS.toMillis(3));
		assertFalse("server thread is still alive", serverThread.isAlive());
	}

	@Test
	public void testJmxClientAndServer() throws ParseException, FileNotFoundException, IOException, InterruptedException, ConnectException {
		JmxServerRunner serverRunner = new JmxServerRunner();
		serverRunner.start();
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		client.connect(serverRunner.getLocalServerAddress());
		client.shutdown();
		serverRunner.stop();
	}

	@Test
	public void testJmxClientServerAndMessage() throws ParseException, FileNotFoundException, IOException, InterruptedException,
		ConnectException, NoPeerForTypeException {

		JmxServerRunner serverRunner = new JmxServerRunner();
		serverRunner.start();
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		client.connect(serverRunner.getLocalServerAddress());
		MultiplexerMessage message = client.createMessageBuilder().setMessage(ByteString.copyFromUtf8("test string")).setType(
			MessageTypes.TEST_REQUEST).setTo(client.getInstanceId()).build();
		assertEquals(ByteString.copyFromUtf8("test string"), message.getMessage());
		client.send(message, SendingMethod.THROUGH_ONE);
		IncomingMessageData incoming = client.receive(1, TimeUnit.SECONDS);
		assertNotNull(incoming);
		assertNotNull(incoming.getMessage());
		assertEquals(message.getMessage(), incoming.getMessage().getMessage());
		assertEquals(message.getId(), incoming.getMessage().getId());
		client.shutdown();
		serverRunner.stop();
	}

	@Test
	public void testJmxClientServerAndBackend() throws ParseException, FileNotFoundException, IOException, InterruptedException,
		ConnectException, NoPeerForTypeException {

		JmxServerRunner serverRunner = new JmxServerRunner();
		serverRunner.start();

		// create backend
		AbstractBackend backend = createEchoBackend();
		backend.connect(serverRunner.getLocalServerAddress());
		Thread backendThread = new Thread(backend);
		backendThread.start();
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		client.connect(serverRunner.getLocalServerAddress());
		backend.cancel();
		client.shutdown();
		backendThread.join(100);
		serverRunner.stop();
	}

	@Test
	public void testJmxClientServerBackendAndQuery() throws ParseException, FileNotFoundException, IOException, InterruptedException,
		ConnectException, NoPeerForTypeException, OperationFailedException {

		testJmxClientServerBackendAndQuery(1);
	}

	@Test
	public void testJmxClientServerBackendAndMultipleQuery() throws ParseException, FileNotFoundException, IOException,
		InterruptedException, ConnectException, NoPeerForTypeException, OperationFailedException {

		testJmxClientServerBackendAndQuery(1000);
	}

	public void testJmxClientServerBackendAndQuery(int times) throws ParseException, FileNotFoundException, IOException,
		InterruptedException, ConnectException, NoPeerForTypeException, OperationFailedException {

		JmxServerRunner serverRunner = new JmxServerRunner();
		serverRunner.start();

		// create backend
		AbstractBackend backend = createEchoBackend();
		backend.connect(serverRunner.getLocalServerAddress());
		Thread backendThread = new Thread(backend);
		backendThread.start();
		JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
		client.connect(serverRunner.getLocalServerAddress());

		final ByteString queryString = ByteString.copyFromUtf8("test message");
		for (int i = 0; i < times; i++) {
			IncomingMessageData msgData = client.query(queryString, TestConstants.MessageTypes.TEST_REQUEST, 2000);
			assertEquals(queryString, msgData.getMessage().getMessage());
		}

		backend.cancel();
		client.shutdown();
		backendThread.join(100);
		serverRunner.stop();
	}

	private AbstractBackend createEchoBackend() {
		AbstractBackend backend = new AbstractBackend(TestConstants.PeerTypes.TEST_SERVER) {
			@Override
			protected void handleMessage(MultiplexerMessage message) throws Exception {
				// reply with the same message, directly to the sender
				reply(createResponse(message.getType(), message.getMessage()));
			}
		};
		return backend;
	}
}
