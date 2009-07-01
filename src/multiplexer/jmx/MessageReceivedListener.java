package multiplexer.jmx;

import multiplexer.Multiplexer.MultiplexerMessage;

/**
 * @author Kasia Findeisen
 * 
 */
public interface MessageReceivedListener {

	public void onMessageReceived(MultiplexerMessage message,
		Connection connection);
}
