package multiplexer.jmx.backend;

import multiplexer.protocol.Classes.MultiplexerMessage;

/**
 * @author Piotr Findeisen
 *
 */
public interface MessageHandler {

	void handleMessage(MultiplexerMessage message, MessageContext ctx);
}
