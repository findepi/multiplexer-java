package multiplexer.jmx;

import multiplexer.Multiplexer.MultiplexerMessage;

import com.google.protobuf.ByteString;

/**
 * @author Piotr Findeisen
 */
// TODO javadoc
public interface MessageContext {

	MultiplexerMessage getMessage();

	JmxClient getJmxClient();

	MultiplexerMessage.Builder createResponse();

	MultiplexerMessage.Builder createResponse(int packetType);

	MultiplexerMessage.Builder createResponse(int packetType, ByteString message);

	void reply(MultiplexerMessage.Builder message);

	boolean hasSentResponse();

	void reportError(Throwable e);

	void reportError(String explanation);

	void setResponseRequired(boolean required);

	boolean isResponseRequired();
}
