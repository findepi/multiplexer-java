package multiplexer.jmx;

import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import multiplexer.Multiplexer.BackendForPacketSearch;
import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.constants.Types;

import org.jboss.netty.channel.ChannelFuture;

import com.google.protobuf.ByteString;

/**
 * TODO
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
	 * TODO
	 * 
	 * @param clientType
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
	 * TODO
	 * 
	 * @param address
	 * @return
	 */
	public ChannelFuture asyncConnect(SocketAddress address) {
		return connectionsManager.asyncConnect(address);
	}

	/**
	 * TODO
	 * 
	 * @param address
	 */
	public void connect(SocketAddress address) {
		asyncConnect(address).awaitUninterruptibly();
	}

	/**
	 * TODO
	 * 
	 * @param message
	 * @param type
	 * @return
	 */
	public MultiplexerMessage createMessage(ByteString message, int type) {
		return connectionsManager.createMessage(message, type);
	}

	/**
	 * TODO
	 * 
	 * @param message
	 * @return
	 */
	public MultiplexerMessage createMessage(MultiplexerMessage.Builder message) {
		return connectionsManager.createMessage(message);
	}

	/**
	 * TODO
	 * 
	 * @param message
	 * @param sendingMethod
	 */
	public int send(MultiplexerMessage message, SendingMethod sendingMethod) {
		return connectionsManager.sendMessage(message, sendingMethod);
	}

	/**
	 * TODO
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public IncomingMessageData receive() throws InterruptedException {
		return connectionsManager.receiveMessage();
	}

	/**
	 * TODO
	 * 
	 * @param message
	 */
	public int event(MultiplexerMessage message) {
		return send(message, SendingMethod.THROUGH_ALL);
	}

	/**
	 * TODO
	 * 
	 * @param message
	 * @param type
	 * @throws InterruptedException
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
		Timer timer = new Timer();
		timer.setTimeoutInMillis(timeout);
		while (answer == null & count > 0) {

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
				answer = null;
				continue;
			}

			if (references == queryId) {
				return answer;
			}
			
			if (references == backendSearchMessageId) {
				answerFromId = answer.getMessage().getFrom();
				MultiplexerMessage backendQueryMessage = createMessage(MultiplexerMessage.newBuilder()
						.setMessage(message).setType(messageType).setTo(
								answerFromId));
				send(backendQueryMessage, SendingMethod.via(answer.getConnection()));
				long backendQueryId = backendQueryMessage.getId();
				queryResponses.put(backendQueryId, queryQueue);
				
				answer = null;
				timer = new Timer();
				timer.setTimeoutInMillis(timeout);
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
					
					if (type != Types.DELIVERY_ERROR & (references == queryId) || (references == backendQueryId)) {
						return answer;
					}
					
					if (references == queryId) {
						assert type == Types.DELIVERY_ERROR;
						if (phase3DeliveryError) {
							throw new RuntimeException("query phases 1 and 3 rejected");
						} else {
							phase1DeliveryError = true;
							answer = null;
							continue;
						}
					}
					
					if (references == backendQueryId) {
						assert type == Types.DELIVERY_ERROR;
						if (phase1DeliveryError) {
							throw new RuntimeException("query phases 1 and 3 rejected");
						} else {
							phase3DeliveryError = true;
							answer = null;
							continue;
						}
					}
				}
	
			}

		}
		throw new RuntimeException("query phase 2 timed out or rejected by all peers");
	}
}

class Timer {
	private final long startTime = System.currentTimeMillis();
	private long timeoutInMillis;

	public void setTimeoutInMillis(long timeout) {
		this.timeoutInMillis = timeout;
	}

	public long getElapsedMillis() {
		return (System.currentTimeMillis() - startTime);
	}

	public long getRemainingMillis() {
		return timeoutInMillis - getElapsedMillis();
	}
}
