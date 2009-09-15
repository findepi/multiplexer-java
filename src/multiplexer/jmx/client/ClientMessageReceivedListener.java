package multiplexer.jmx.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;

import multiplexer.jmx.internal.MessageReceivedListener;
import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * @author Piotr Findeisen
 */
public class ClientMessageReceivedListener implements MessageReceivedListener {
	
	final private ConcurrentMap<Long, BlockingQueue<IncomingMessageData>> queryResponses;
	final private BlockingQueue<IncomingMessageData> messageQueue;
	
	public ClientMessageReceivedListener(
		ConcurrentMap<Long, BlockingQueue<IncomingMessageData>> queryResponses,
		BlockingQueue<IncomingMessageData> messageQueue) {
		super();
		this.queryResponses = queryResponses;
		this.messageQueue = messageQueue;
	}

	public void onMessageReceived(MultiplexerMessage message,
		Connection connection) {
		long id = message.getReferences();
		IncomingMessageData msg = new IncomingMessageData(message,
			connection);
		BlockingQueue<IncomingMessageData> queryQueue = queryResponses
			.get(id);
		if (queryQueue == null) {
			messageQueue.add(msg);
		} else {
			queryQueue.add(msg);
		}
	}
}
