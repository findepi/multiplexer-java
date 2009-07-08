package multiplexer.jmx.backend;

import multiplexer.jmx.client.JmxClient;
import multiplexer.protocol.Classes.MultiplexerMessage;

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
	
	void setResponseSent(boolean sent);

	boolean hasSentResponse();

	void reportError(Throwable e);

	void reportError(String explanation);

	void setResponseRequired(boolean required);

	boolean isResponseRequired();
}
