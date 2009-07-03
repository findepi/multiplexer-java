package multiplexer.jmx;

import java.io.PrintWriter;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.protocol.Constants.MessageTypes;

import com.google.protobuf.ByteString;

/**
 * Abstract base class for backends providing services through Multiplexer
 * connections. A subclass needs to define only
 * {@link AbstractBackend#handleMessage} method in order to have fully working
 * Multiplexer backend that supports {@code PING} and {@code
 * BACKEND_FOR_PACKET_SEARCH} messages and sends {@code BACKEND_ERROR} on
 * errors.
 * 
 * A simple example of Echo service may look like this:
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

// TODO mam wrażenie, że AbstractBackend nie zmienia timestampu, gdy odpowiada
// backend_errorem...
public abstract class AbstractBackend implements Runnable {

	private static final Logger logger = LoggerFactory
		.getLogger(AbstractBackend.class);

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
	private boolean responseSent;
	private boolean responseRequired;

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
	 */
	public void connect(SocketAddress address) {
		asyncConnect(address).awaitUninterruptibly();
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
	abstract protected void handleMessage(MultiplexerMessage message)
		throws Exception;

	public void run() {
		assert thread == null;
		thread = Thread.currentThread();
		try {
			try {
				while (!isCancelled()) {
					runOne();
				}
			} catch (InterruptedException e) {
				if (!isCancelled()) {
					throw e;
				} else {
					logger
						.warn(
							"interruption ignored, use AbstractBackend.cancel to stop the backend",
							e);
				}
			}
		} catch (Exception e) {
			logger.warn("Unhandled exception", e);
		} finally {
			thread = null;
		}
		// TODO cleanup of JmxClient
	}

	private void runOne() throws Exception {
		lastIncomingRequest = connection.receive();
		if (lastIncomingRequest == null) {
			throw new NullPointerException("lastIncomingRequest");
		}
		lastMessage = lastIncomingRequest.getMessage();
		if (lastMessage == null) {
			throw new NullPointerException("lastMessage");
		}
		responseSent = false;
		responseRequired = true;

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
					MultiplexerMessage.Builder response = createResponse()
						.setType(MessageTypes.PING).setMessage(
							lastMessage.getMessage());
					assert response.hasReferences()
						&& response.getReferences() != 0;
					reply(response);
				}
				break;

			default:
				if (lastMessage.getType() <= MessageTypes.MAX_MULTIPLEXER_META_PACKET) {
					logger.warn("Unable to handle meta packet of type {}",
						lastMessage.getType());
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
			responseMissing = !responseSent && responseRequired;
		} catch (Exception e) {
			logger.warn("handleMessage threw", e);
			reportError(e);
			throw e;
		}
		if (responseMissing) {
			logger.warn("handleMessage finished without sending any response");
			reportError("handleMessage finished without sending any response");
		}
	}

	protected void reportError(Throwable e) throws NoPeerForTypeException {
		reply(createResponse(MessageTypes.BACKEND_ERROR, serializeStackTrace(e)));
	}

	protected void reportError(String explanation)
		throws NoPeerForTypeException {
		reply(createResponse(MessageTypes.BACKEND_ERROR, ByteString
			.copyFromUtf8(explanation)));
	}

	protected void handleException(Exception e) throws Exception {
		throw e;
	}

	protected static ByteString serializeStackTrace(Throwable e) {
		ByteString.Output output = ByteString.newOutput();
		PrintWriter writer = new PrintWriter(output);
		e.printStackTrace(writer);
		writer.close(); // force flush
		return output.toByteString();
	}

	protected void noResponse() {
		responseRequired = false;
	}

	protected MultiplexerMessage.Builder createResponse() {
		assert lastMessage != null;
		MultiplexerMessage.Builder builder = connection.createMessageBuilder()
			.setTo(lastMessage.getFrom()).setReferences(lastMessage.getId());
		if (lastMessage.hasWorkflow())
			builder.setWorkflow(lastMessage.getWorkflow());
		return builder;
	}

	protected MultiplexerMessage.Builder createResponse(int packetType) {
		return createResponse().setType(packetType);
	}

	protected MultiplexerMessage.Builder createResponse(int packetType,
		ByteString message) {
		assert message != null;
		return createResponse(packetType).setMessage(message);
	}

	protected void reply(MultiplexerMessage.Builder message)
		throws NoPeerForTypeException {
		Connection conn = lastIncomingRequest.getConnection();
		assert conn != null;
		assert message.hasType() || message.hasTo();
		assert message.hasId();
		connection.send(message.build(), SendingMethod.via(conn));
		responseSent = true;
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
