package multiplexer.jmx;

import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import multiplexer.Multiplexer.BackendForPacketSearch;
import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.Multiplexer.MultiplexerMessage.Builder;
import multiplexer.constants.Peers;
import multiplexer.constants.Types;

import org.jboss.netty.channel.ChannelFuture;

import com.google.protobuf.ByteString;

/**
 * A Multiplexer server's client class. It provides methods for connecting with
 * a specified address, creating, sending and receiving Multiplexer messages.
 * {@code event} method sends a specified message to all connected Multiplexer
 * servers while {@code query} method is useful for getting a remote backend
 * solve a task.
 * 
 * @author Kasia Findeisen
 * @author Piotr Findeisen
 * 
 */
public class Client {
	protected final ConnectionsManager connectionsManager;

	private ConcurrentMap<Long, BlockingQueue<IncomingMessageData>> queryResponses = new ConcurrentHashMap<Long, BlockingQueue<IncomingMessageData>>();
	private BlockingQueue<IncomingMessageData> messageQueue = new LinkedBlockingQueue<IncomingMessageData>();

	/**
	 * Creates a new instance of a specified type ({@code clientType}). Sets the
	 * instance's {@link ConnectionsManager} and defines a callback method
	 * {@code onMessageReceived}. The method, invoked by the {@code
	 * ConnectionsManager}, puts the {@link IncomingMessageData}, accordingly to
	 * it's {@code referenceId} in one of the instance's {@link BlockingQueue}s,
	 * from where it might be read by method {@code receive()}.
	 * 
	 * @param clientType
	 *            client types are defined in {@link Peers}
	 */
	public Client(int clientType) {
		connectionsManager = new ConnectionsManager(clientType);
		connectionsManager
				.setMessageReceivedListener(new MessageReceivedListener() {

					@Override
					public void onMessageReceived(MultiplexerMessage message,
							Connection connection) {
						long Id = message.getReferences();
						IncomingMessageData msg = new IncomingMessageData(
								message, connection);
						BlockingQueue<IncomingMessageData> queryQueue = queryResponses
								.get(Id);
						if (queryQueue == null) {
							messageQueue.add(msg);
						} else {
							queryQueue.add(msg);
						}

					}
				});
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
		return connectionsManager.asyncConnect(address);
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
	 * Creates a new {@link MultiplexerMessage} with the specified
	 * {@link ByteString} as a {@code message} and a specified {@code type}.
	 * Also, fields {@code id}, {@code from} and {@code timestamp} are set.
	 * 
	 * @param message
	 * @param type
	 *            message types are defined in {@link Types}
	 */
	public MultiplexerMessage createMessage(ByteString message, int type) {
		return connectionsManager.createMessage(message, type);
	}

	/**
	 * Creates a new {@link MultiplexerMessage} using the specified
	 * {@link Builder}. Allows to set all the message's fields. However, fields
	 * {@code id}, {@code from} and {@code timestamp} are reset.
	 * 
	 * @param message
	 *            use {@code MultiplexerMessage.newBuilder()} to obtain a
	 *            builder. Use {@code builder.setAttributeName(name)} to set
	 *            {@code attributeName}. {@link MultiplexerMessage}'s attributes
	 *            are defined in {@link Multiplexer.proto} -> {@code
	 *            MultiplexerMessage}.
	 */
	public MultiplexerMessage createMessage(MultiplexerMessage.Builder message) {
		return connectionsManager.createMessage(message);
	}

	/**
	 * Sends one or more copies of {@code message}, accordingly to specified
	 * {@link SendingMethod}. {@code sendingMethod} might specify the receiver
	 * or just specify the type and quantity of receivers. In case the receiver
	 * should be any of a given type, it is chosen on a basis of round-robin
	 * algorithm.
	 * 
	 * @param message
	 * @param sendingMethod
	 * @return number of copies of {@code message} sent.
	 */
	public int send(MultiplexerMessage message, SendingMethod sendingMethod) {
		return connectionsManager.sendMessage(message, sendingMethod);
	}

	/**
	 * Tries to take one message from {@link BlockingQueue} of received
	 * messages. Blocks until any message is available.
	 * 
	 * @throws InterruptedException
	 * @return includes the received message and a connection
	 */
	public IncomingMessageData receive() throws InterruptedException {
		return connectionsManager.receiveMessage();
	}

	/**
	 * Sends a specified {@code message} through all connected Multiplexer
	 * servers.
	 * 
	 * @param message
	 * @return number of copies of {@code message} sent.
	 */
	public int event(MultiplexerMessage message) {
		return send(message, SendingMethod.THROUGH_ALL);
	}

	/**
	 * Attempts to send out a message with specified content ({@code message})
	 * and of specified type ({@code MessageType}) and get an answer from remote
	 * backend.
	 * 
	 * This is a 3-phase algorithm, however it may end at any stage on proper
	 * conditions. In phase 1, the message is sent through one Multiplexer. If
	 * the answer doesn't appear within specified amount of time ({@code
	 * timeout}), the algorithm enters phase 2. A special message, aimed to find
	 * a proper backend is sent through all connected Mulitplexer servers
	 * (method {@code event}). If an answer from the backend comes within a
	 * specified amount of time ({@code timeout}), the algorithm enters phase 3.
	 * The message is sent directly to the backend, and another {@code timeout}
	 * is given to receive the answer.
	 * 
	 * The algorithm only reads it's own messages. Other messages,
	 * simultaneously received by the {@code Client}, are not affected.
	 * 
	 * @param message
	 *            the request to be sent over Multiplexer connection
	 * @param messageType
	 *            type of the request, from which a Multiplexer can deduce the
	 *            right backend type
	 * @param timeout
	 *            each of the 3 phases of the algorithm has the same time limit
	 * @throws InterruptedException
	 * @return an answer message
	 */
	public IncomingMessageData query(ByteString message, int messageType,
			long timeout) throws InterruptedException {

		boolean phase1DeliveryError = false;
		boolean phase3DeliveryError = false;

		MultiplexerMessage queryMessage = createMessage(message, messageType);
		long queryId = queryMessage.getId();
		LinkedBlockingQueue<IncomingMessageData> queryQueue = new LinkedBlockingQueue<IncomingMessageData>();
		queryResponses.put(queryId, queryQueue);
		send(queryMessage, SendingMethod.THROUGH_ONE);

		IncomingMessageData answer = queryQueue.poll(timeout,
				TimeUnit.MILLISECONDS);
		if (answer != null) {
			if (answer.getMessage().getType() != Types.DELIVERY_ERROR) {
				return answer;
			} else
				phase1DeliveryError = true;
		}

		BackendForPacketSearch backendSearch = BackendForPacketSearch
				.newBuilder().setPacketType(messageType).build();
		MultiplexerMessage backendSearchMessage = createMessage(backendSearch
				.toByteString(), Types.BACKEND_FOR_PACKET_SEARCH);
		long backendSearchMessageId = backendSearchMessage.getId();
		queryResponses.put(backendSearchMessageId, queryQueue);
		int count = event(backendSearchMessage);

		answer = null;
		long answerFromId;
		TimeoutCounter timer = new TimeoutCounter(timeout);
		while (answer == null) {

			answer = queryQueue.poll(timer.getRemainingMillis(),
					TimeUnit.MILLISECONDS);

			if (answer == null) {
				throw new RuntimeException("query phase 2 timed out");
			}

			long references = answer.getMessage().getReferences();
			int type = answer.getMessage().getType();

			if (type == Types.DELIVERY_ERROR & references == queryId) {
				phase1DeliveryError = true;
				answer = null;
				continue;
			}

			if (type == Types.DELIVERY_ERROR) {
				assert type == backendSearchMessageId;
				count--;
				if (count == 0) {
					throw new RuntimeException(
							"query phase 2 rejected by all peers");
				}
				answer = null;
				continue;
			}

			if (references == queryId) {
				return answer;
			}

			if (references == backendSearchMessageId) {
				answerFromId = answer.getMessage().getFrom();
				MultiplexerMessage backendQueryMessage = createMessage(MultiplexerMessage
						.newBuilder().setMessage(message).setType(messageType)
						.setTo(answerFromId));
				long backendQueryId = backendQueryMessage.getId();
				queryResponses.put(backendQueryId, queryQueue);
				send(backendQueryMessage, SendingMethod.via(answer
						.getConnection()));

				answer = null;
				timer = new TimeoutCounter(timeout);
				while (answer == null) {

					answer = queryQueue.poll(timer.getRemainingMillis(),
							TimeUnit.MILLISECONDS);

					if (answer == null) {
						throw new RuntimeException("query phase 3 timed out");
					}

					references = answer.getMessage().getReferences();
					type = answer.getMessage().getType();

					if (references == backendSearchMessageId) {
						answer = null;
						continue;
					}

					if (type != Types.DELIVERY_ERROR
							& ((references == queryId) || (references == backendQueryId))) {
						return answer;
					}

					if (references == queryId) {
						assert type == Types.DELIVERY_ERROR;
						if (phase3DeliveryError) {
							throw new RuntimeException(
									"query phases 1 and 3 rejected");
						} else {
							phase1DeliveryError = true;
							answer = null;
							continue;
						}
					}

					if (references == backendQueryId) {
						assert type == Types.DELIVERY_ERROR;
						if (phase1DeliveryError) {
							throw new RuntimeException(
									"query phases 1 and 3 rejected");
						} else {
							phase3DeliveryError = true;
							answer = null;
							continue;
						}
					}
				}

			}

		}
		throw new AssertionError("Should not reach here.");
	}
}

/**
 * Helper class for timeout management. Used time unit is milliseconds. Each
 * instance gets it's creation time and specified timeout. It might check, at
 * any point, whether the timeout has passed.
 * 
 * @author Kasia Findeisen
 * 
 */
class TimeoutCounter {
	private final long startTime = System.currentTimeMillis();
	private long timeoutInMillis;

	/**
	 * Creates a new instance which might be asked if a specific timeout (
	 * {@code timeoutInMillis}) has passed since it's creation time.
	 * 
	 * @param timeoutInMillis
	 *            timeout in milliseconds
	 */
	public TimeoutCounter(long timeoutInMillis) {
		this.timeoutInMillis = timeoutInMillis;
	}

	/**
	 * Returns the amount of time in milliseconds that has passed since the
	 * instance's creation time.
	 * 
	 * @return
	 */
	public long getElapsedMillis() {
		return (System.currentTimeMillis() - startTime);
	}

	/**
	 * Returns the amount of time remaining until {@code timeoutInMillis} passes
	 * or {@code 0} if it has happened already.
	 * 
	 * @return
	 */
	public long getRemainingMillis() {
		long remainingMillis = timeoutInMillis - getElapsedMillis();
		if (remainingMillis >= 0) {
			return remainingMillis;
		}
		return 0;
	}
}
