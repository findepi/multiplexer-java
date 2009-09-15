package multiplexer.jmx.internal;

import multiplexer.jmx.client.Connection;
import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * @author Kasia Findeisen
 */
public interface MessageReceivedListener {

	public void onMessageReceived(MultiplexerMessage message,
		Connection connection);
}
