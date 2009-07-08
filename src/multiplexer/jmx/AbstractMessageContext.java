package multiplexer.jmx;

import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.Multiplexer.MultiplexerMessage.Builder;
import multiplexer.protocol.Constants.MessageTypes;

import com.google.protobuf.ByteString;
import static multiplexer.jmx.internal.Stacks.stackTraceToByteString;

/**
 * Partial implementation of {@link MessageContext}.
 * 
 * @author Piotr Findeisen
 * 
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
		assert message != null;
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

	protected void setResponseSent(boolean sent) {
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
