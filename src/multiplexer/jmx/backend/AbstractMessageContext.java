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

import static com.google.common.base.Preconditions.checkNotNull;
import static multiplexer.jmx.util.Stacks.stackTraceToByteString;

import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Protocol.MultiplexerMessage;
import multiplexer.protocol.Protocol.MultiplexerMessage.Builder;

import com.google.protobuf.ByteString;

/**
 * Partial implementation of {@link MessageContext}.
 * 
 * @author Piotr Findeisen
 */
public abstract class AbstractMessageContext implements MessageContext {

	private final MultiplexerMessage message;
	private boolean responseSent = false;
	private boolean responseRequired = true;

	public AbstractMessageContext(MultiplexerMessage message) {
		if (message == null)
			throw new NullPointerException("message");
		this.message = message;
	}

	protected MultiplexerMessage.Builder createMessageBuilder() {
		return getJmxClient().createMessageBuilder();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#createResponse()
	 */
	public Builder createResponse() {
		assert message != null;
		MultiplexerMessage.Builder builder = createMessageBuilder().setTo(
			message.getFrom()).setReferences(message.getId());
		if (message.hasWorkflow())
			builder.setWorkflow(message.getWorkflow());
		return builder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#createResponse(int)
	 */
	public Builder createResponse(int packetType) {
		return createResponse().setType(packetType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#createResponse(int,
	 * com.google.protobuf.ByteString)
	 */
	public Builder createResponse(int packetType, ByteString message) {
		checkNotNull(message);
		return createResponse(packetType).setMessage(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#getMessage()
	 */
	public MultiplexerMessage getMessage() {
		return message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#hasSentResponse()
	 */
	public boolean hasSentResponse() {
		return responseSent;
	}

	public void setResponseSent(boolean sent) {
		this.responseSent = sent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#isResponseRequired()
	 */
	public boolean isResponseRequired() {
		return responseRequired;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#reportError(java.lang.Throwable)
	 */
	public void reportError(Throwable e) {
		reply(createResponse(MessageTypes.BACKEND_ERROR,
			stackTraceToByteString(e)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#reportError(java.lang.String)
	 */
	public void reportError(String explanation) {
		reply(createResponse(MessageTypes.BACKEND_ERROR, ByteString
			.copyFromUtf8(explanation)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multiplexer.jmx.MessageContext#setResponseRequired(boolean)
	 */
	public void setResponseRequired(boolean required) {
		this.responseRequired = required;
	}

}
