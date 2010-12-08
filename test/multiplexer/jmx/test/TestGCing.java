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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
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

import org.junit.Test;

import com.google.protobuf.ByteString;

/**
 * @author Piotr Findeisen
 */
public class TestGCing extends JmxServerProvidingTestCase {
	@Test
	public void testJmxClientGCing() throws Exception {
		ensureGCed(createAndWarmUpClient());
	}

	private JmxClient createClient() throws UnknownHostException,
		ConnectException {
		JmxClient client = new JmxClient(TestConstants.PeerTypes.TEST_CLIENT);
		client.connect(getLocalServerAddress());
		return client;
	}

	private JmxClient createAndWarmUpClient() throws NoPeerForTypeException,
		InterruptedException, UnknownHostException, ConnectException {

		final int threadCount = Thread.activeCount();

		JmxClient client = createClient();
		// make sure the connections are established
		warmUp(client, client.getInstanceId());
		assertTrue("there were no new threads started", threadCount < Thread
			.activeCount());

		return client;
	}

	@Test
	public void testBackendGCing() throws Exception {
		ensureGCed(createBackendAndConnect());
	}

	private AbstractBackend createBackendAndConnect()
		throws UnknownHostException, NoPeerForTypeException,
		InterruptedException, ConnectException {

		AbstractBackend backend = new AbstractBackend(
			TestConstants.PeerTypes.ECHO_SERVER) {
			@Override
			protected void handleMessage(MultiplexerMessage message)
				throws Exception {
				fail("this should not be called -- backend not run");
			}
		};
		backend.connect(getLocalServerAddress());
		warmUp(backend.getJmxClient(), backend.getJmxClient().getInstanceId());
		return backend;
	}

	private void warmUp(JmxClient client, long to)
		throws NoPeerForTypeException, InterruptedException {
		final long times = 50;
		for (int i = 0; i < times; i++) {
			client.send(client.createMessageBuilder().setType(
				TestConstants.MessageTypes.TEST_REQUEST).setTo(to).setMessage(
				ByteString.copyFromUtf8("test message")).build(),
				SendingMethod.THROUGH_ONE);
		}

		for (int i = 0; i < times; i++) {
			IncomingMessageData imd = client.receive(1000,
				TimeUnit.MILLISECONDS);
			assertNotNull(imd);
			assertEquals(imd.getMessage().getMessage(), ByteString
				.copyFromUtf8("test message"));
		}
	}

	private <T> void ensureGCed(T client) throws IllegalArgumentException,
		InterruptedException {

		ReferenceQueue<T> clientQueue = new ReferenceQueue<T>();
		WeakReference<T> clientReference = new WeakReference<T>(client,
			clientQueue);
		client = null;
		ensureGCed(clientReference, clientQueue);
	}

	private <T, Y> void ensureGCed(WeakReference<T> reference,
		ReferenceQueue<T> queue) throws IllegalArgumentException,
		InterruptedException {

		System.gc();
		Reference<? extends T> finalizedReference = queue.remove(1000);
		assertNotNull("ReferenceQueue still empty, remove() timed out",
			finalizedReference);
		assertSame("ReferenceQueue contains some other reference", reference,
			finalizedReference);
		assertNull("JmxClient has not been GC-ed", reference.get());
	}
}