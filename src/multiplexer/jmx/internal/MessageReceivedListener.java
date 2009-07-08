package multiplexer.jmx.internal;

import multiplexer.protocol.Classes.MultiplexerMessage;

/**
 * @author Kasia Findeisen
 * 
 */
public interface MessageReceivedListener {

	public void onMessageReceived(MultiplexerMessage message,
		Connection connection);
}
