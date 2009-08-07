package multiplexer.jmx.backend;

import multiplexer.jmx.client.Connection;
import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * @author Piotr Findeisen
 * 
 */
public abstract class AbstractHandlingBackend extends AbstractBackend {

	private MessageHandler messageHandler;

	public AbstractHandlingBackend(int peerType) {
		super(peerType);
	}

	protected MessageHandler getMessageHandler() {
		return messageHandler;
	}

	protected void setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * multiplexer.jmx.AbstractBackend#handleMessage(multiplexer.Multiplexer
	 * .MultiplexerMessage)
	 */
	@Override
	protected void handleMessage(MultiplexerMessage message) throws Exception {
		MessageHandler messageHandler = getMessageHandler();
		if (messageHandler == null)
			throw new NullPointerException("messageHandler");
		MessageContext ctx = createContext(message, lastIncomingRequest
			.getConnection());
		messageHandler.handleMessage(message, ctx);
		if (!ctx.isResponseRequired()) {
			this.noResponse();
		}
		this.setResponseSent(ctx.hasSentResponse());
	}

	/**
	 * Create new {@link MessageContext} for handling this {@code message}. All
	 * replies sent through this new {@link MessageContext} will be sent using
	 * passed {@link Connection}.
	 * 
	 * @param message
	 *            message that a new context will point to
	 * @return new {@code MessageContext} for the {@code message}
	 */
	protected MessageContext createContext(MultiplexerMessage message,
		Connection conn) {

		return new MessageContextImpl(message, conn);
	}

	/**
	 * IncomingMessageData {@link MessageContext} is used as a context
	 * descriptor, when forwarding message handling to a {@link MessageHandler}.
	 * This is an implementation of {@link MessageContext} to be used with
	 * {@link AbstractHandlingBackend} .
	 * <p>
	 * You should use {@link AbstractHandlingBackend#createContext} instead of
	 * explicitly calling the constructor.
	 * 
	 * @author Piotr Findeisen
	 */
	protected class MessageContextImpl extends DefaultMessageContext {
		public MessageContextImpl(MultiplexerMessage message, Connection conn) {
			super(message, AbstractHandlingBackend.this.connection, conn);
		}
	}
}
