/**
 * 
 */
package multiplexer.jmx;

import multiplexer.Multiplexer.MultiplexerMessage;

/**
 * @author Kasia Findeisen
 *
 */
public interface MessageReceivedListener {
	/**
	 * TODO
	 * @param message
	 * @param connection
	 */
	public void onMessageReceived(MultiplexerMessage message, Connection connection);
}
