package multiplexer.jmx;

import multiplexer.Multiplexer.MultiplexerMessage;

/**
 * @author Piotr Findeisen
 *
 */
public interface MessageHandler {

	void handleMessage(MultiplexerMessage message, MessageContext ctx);
}
