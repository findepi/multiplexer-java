// Copyright 2009 Warsaw University, Faculty of Physics
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package multiplexer.jmx.backend;

import multiplexer.jmx.client.Connection;
import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * @author Piotr Findeisen
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
