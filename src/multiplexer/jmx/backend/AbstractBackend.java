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

package multiplexer.jmx.backend;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import multiplexer.jmx.client.ConnectException;
import multiplexer.jmx.client.IncomingMessageData;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Protocol.MultiplexerMessage;

import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

/**
 * <p>
 * Abstract base class for backends providing services through Multiplexer
 * connections. A subclass needs to define only
 * {@link AbstractBackend#handleMessage} method in order to have fully working
 * Multiplexer backend that supports {@code PING} and {@code
 * BACKEND_FOR_PACKET_SEARCH} messages and sends {@code BACKEND_ERROR} on
 * errors.
 * <p>
 * A simple example of Echo service may look like this (this example is little
 * incomplete &mdash; the backend does not connect to any servers):
 * 
 * <pre>
 * new AbstractBackend(Peers.ECHO_SERVER) {
 * 	&#064;Override
 * 	protected void handleMessage(MultiplexerMessage message) throws Exception {
 * 		// reply with the same message, directly to the sender
 * 		reply(createResponse(message.getType(), message.getMessage()));
 * 	}
 * }.run();
 * </pre>
 * 
 * @author Piotr Findeisen
 */
public abstract class AbstractBackend implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(AbstractBackend.class);

	/**
	 * A handler to Multiplexer server connections.
	 */
	protected final JmxClient connection;

	/**
	 * true if this backend has been cancelled through a call to
	 * {@link AbstractBackend#cancel}
	 */
	private AtomicBoolean cancelled = new AtomicBoolean(false);
	private volatile Thread thread;
	protected IncomingMessageData lastIncomingRequest;
	protected MultiplexerMessage lastMessage;
	private MessageContext currentContext;

	protected AbstractBackend(int peerType) {
		connection = new JmxClient(peerType);
	}

	/**
	 * Begins asynchronously an attempt of connection with the specified {@code
	 * address}.
	 * 
	 * @param address
	 * @return a future object which notifies when this connection attempt
	 *         succeeds or fails
	 */
	public ChannelFuture asyncConnect(SocketAddress address) {
		return connection.asyncConnect(address);
	}

	/**
	 * Connects synchronously with the specified {@code address}.
	 * 
	 * @param address
	 * @throws ConnectException
	 */
	public void connect(SocketAddress address) throws ConnectException {
		connection.connect(address);
	}

	/**
	 * Returns underlying {@link JmxClient} connection. Use with caution.
	 */
	public JmxClient getJmxClient() {
		return connection;
	}

	/**
	 * Subclasses need to define this method to get complete and working
	 * backend.
	 * 
	 * @param message
	 *            message being handled
	 * @throws Exception
	 * @see AbstractBackend
	 */
	abstract protected void handleMessage(MultiplexerMessage message) throws Exception;

	public void run() {
		checkState(thread == null);
		thread = Thread.currentThread();
		try {
			try {
				while (!isCancelled()) {
					runOne();
				}
			} catch (InterruptedException e) {
				if (isCancelled()) {
					return;
				} else {
					logger.warn("worker interruped, use AbstractBackend.cancel to stop the backend cleanly", e);
					throw e;
				}
			}
		} catch (Exception e) {
			logger.warn("Unhandled exception", e);
		} finally {
			thread = null;
		}

		try {
			connection.flush();
		} catch (InterruptedException e) {
			logger.warn("final flush interruped", e);
		}
		try {
			connection.shutdown();
		} catch (InterruptedException e) {
			logger.warn("shutdown interruped", e);
		}
	}

	private void runOne() throws Exception {
		lastIncomingRequest = checkNotNull(connection.receive(), "lastIncomingRequest");
		lastMessage = checkNotNull(lastIncomingRequest.getMessage(), "lastMessage");
		currentContext = new DefaultMessageContext(lastMessage, connection, lastIncomingRequest.getConnection());

		try {
			switch (lastMessage.getType()) {
			case MessageTypes.CONNECTION_WELCOME:
				throw new AssertionError("Unexpected CONNECTION_WELCOME");

			case MessageTypes.BACKEND_FOR_PACKET_SEARCH:
				reply(createResponse(MessageTypes.PING));
				break;

			case MessageTypes.PING:
				if (lastMessage.hasReferences()) {
					assert lastMessage.getReferences() != 0;
					noResponse();
				} else {
					assert lastMessage.getId() != 0;
					MultiplexerMessage.Builder response = createResponse().setType(MessageTypes.PING).setMessage(
						lastMessage.getMessage());
					assert response.hasReferences() && response.getReferences() != 0;
					reply(response);
				}
				break;

			default:
				if (lastMessage.getType() <= MessageTypes.MAX_MULTIPLEXER_META_PACKET) {
					logger.warn("Unable to handle meta packet of type {}", lastMessage.getType());
				} else {
					handleOrdinaryMessage();
				}
				break;
			}
		} catch (Exception e) {
			handleException(e);
		} finally {
			lastIncomingRequest = null;
			lastMessage = null;
		}
	}

	private void handleOrdinaryMessage() throws Exception {
		assert lastMessage.getType() > MessageTypes.MAX_MULTIPLEXER_META_PACKET;
		boolean responseMissing;
		try {
			handleMessage(lastMessage);
			responseMissing = !currentContext.hasSentResponse() && currentContext.isResponseRequired();
		} catch (Exception e) {
			logger.warn("handleMessage threw: {}", e.getMessage());
			reportError(e);
			throw e;
		}
		if (responseMissing) {
			logger.warn("handleMessage finished without sending any response");
			reportError("handleMessage finished without sending any response");
		}
	}

	protected void reportError(Throwable e) throws NoPeerForTypeException {
		assert currentContext != null;
		currentContext.reportError(e);
	}

	protected void reportError(String explanation) throws NoPeerForTypeException {
		assert currentContext != null;
		currentContext.reportError(explanation);
	}

	protected void handleException(Exception e) throws Exception {
		throw e;
	}

	protected void noResponse() {
		currentContext.setResponseRequired(false);
	}

	protected MultiplexerMessage.Builder createResponse() {
		return currentContext.createResponse();
	}

	protected MultiplexerMessage.Builder createResponse(int packetType) {
		return currentContext.createResponse(packetType);
	}

	protected MultiplexerMessage.Builder createResponse(int packetType, ByteString message) {
		return currentContext.createResponse(packetType, message);
	}

	protected void reply(MultiplexerMessage.Builder message) {
		currentContext.reply(message);
	}

	protected void setResponseSent(boolean sent) {
		currentContext.setResponseSent(sent);
	}

	/**
	 * Cancels the execution of this {@code AbstractBackend}.
	 * 
	 * @param interrupt
	 *            if set to true, the thread running this
	 *            {@link AbstractBackend} will be interrupted.
	 * @return true if and only if the thread was interrupted
	 */
	public boolean cancel(boolean interrupt) {
		if (cancelled.getAndSet(true)) {
			// work already cancelled
			return false;
		}
		if (interrupt) {
			Thread workerThread = thread;
			if (workerThread != null) {
				workerThread.interrupt();
				return true;
			}
		}
		return false;
	}

	/**
	 * Cancels the execution of this {@code AbstractBackend} interrupting the
	 * worker thread. See {@link #cancel(boolean)} for details.
	 */
	public boolean cancel() {
		return cancel(true);
	}

	public boolean isCancelled() {
		return cancelled.get();
	}
}
