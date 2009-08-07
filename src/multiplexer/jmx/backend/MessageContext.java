package multiplexer.jmx.backend;

import multiplexer.jmx.client.Connection;
import multiplexer.jmx.client.JmxClient;
import multiplexer.protocol.Protocol.MultiplexerMessage;

import com.google.protobuf.ByteString;

/**
 * Context of a {@link MultiplexerMessage} being handled by
 * {@link MessageHandler}. Provides a bridge between a handler and a backend
 * (for example {@link AbstractHandlingBackend}) that invokes the handler.
 * Enables handler to construct and send responses, report errors etc.
 * 
 * @see DefaultMessageContext
 * @see AbstractMessageContext
 * @author Piotr Findeisen
 */
public interface MessageContext {

	/**
	 * A message being handled.
	 */
	MultiplexerMessage getMessage();

	/**
	 * A {@link JmxClient} that invoking backend is attached to.
	 */
	JmxClient getJmxClient();

	/**
	 * Construct a response to the message being handled with preset fields:
	 * {@code to}, {@code from}, {@code references}, {@code timestamp} and
	 * {@code workflow}.
	 * 
	 * @return a builder of {@link MultiplexerMessage} that can be further
	 *         altered
	 */
	MultiplexerMessage.Builder createResponse();

	/**
	 * Same as {@link #createResponse()} but also sets {@code type} field.
	 */
	MultiplexerMessage.Builder createResponse(int packetType);

	/**
	 * Same as {@link #createResponse()} but also sets {@code type} and {@code
	 * message} fields.
	 */
	MultiplexerMessage.Builder createResponse(int packetType, ByteString message);

	/**
	 * Send a {@link MultiplexerMessage} over a {@link JmxClient} using the same
	 * {@link Connection} that was used to read the handled message. Also notes
	 * that a response has been sent. Not sending a response is normally
	 * considered an error, see {@link #setResponseSent(boolean)} tough.
	 * 
	 * @param message
	 *            a {@link MultiplexerMessage} to be sent
	 */
	void reply(MultiplexerMessage message);

	/**
	 * Shortcut for {@code reply(message.build())}. See
	 * {@link MessageContext#reply(multiplexer.protocol.Protocol.MultiplexerMessage)}
	 * for details.
	 * 
	 * @param message
	 *            a builder of a {@link MultiplexerMessage} to be sent
	 */
	void reply(MultiplexerMessage.Builder message);

	/**
	 * Manually set the {@code responseSent} flag either allowing for no
	 * response to be sent or notifying that still one or more responses should
	 * be sent during message handling.
	 * 
	 * To inform the invoking backend that this message does not require any
	 * response use {@link #setResponseRequired(boolean)}. Use this function
	 * generally in the case when the message requires two or more responses to
	 * reset the {@code responseSent} flag back to {@code false} until the last
	 * response is sent out.
	 */
	void setResponseSent(boolean sent);

	/**
	 * Get the value of the {@code responseSent} flag.
	 */
	boolean hasSentResponse();

	/**
	 * Set the {@code responseRequired} flag (default is true). If the response
	 * is required and there is no response sent during message handling, the
	 * backend that invoked a {link {@link MessageHandler} should reply to the
	 * querying peer with a {@code BACKEND_ERROR} message.
	 * 
	 * Use this function when handling events (or other messages such that the
	 * sender does not expect response to).
	 */
	void setResponseRequired(boolean required);

	/**
	 * Get the value of the {@code responseRequired} flag.
	 */
	boolean isResponseRequired();

	/**
	 * Report an exception that happened during message handling. This creates
	 * {@code MessageTypes.BACKEND_ERROR} message with traceback as a {@code
	 * message} in human readable form (UTF-8 encoded).
	 * 
	 * @param e
	 *            caught exception
	 */
	void reportError(Throwable e);

	/**
	 * Similiar to {@link #reportError(String)} but sets {@code explanation}
	 * instead of exception's traceback as a message body.
	 * 
	 * @param explanation
	 */
	void reportError(String explanation);
}
