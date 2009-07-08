package multiplexer.jmx.backend;

import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * @author Piotr Findeisen
 *
 */
public interface MessageHandler {

	void handleMessage(MultiplexerMessage message, MessageContext ctx);
}
