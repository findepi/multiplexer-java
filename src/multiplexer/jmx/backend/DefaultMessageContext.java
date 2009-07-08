package multiplexer.jmx.backend;

import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.client.SendingMethod;
import multiplexer.jmx.internal.Connection;
import multiplexer.protocol.Classes.MultiplexerMessage;
import multiplexer.protocol.Classes.MultiplexerMessage.Builder;

/**
 * Default full implementation of {@link MessageContext}.
 * 
 * @author Piotr Findeisen
 */
public class DefaultMessageContext extends AbstractMessageContext {

	private final JmxClient client;
	private final Connection conn;

	public DefaultMessageContext(MultiplexerMessage message, JmxClient client,
		Connection conn) {
		super(message);
		if (client == null)
			throw new NullPointerException("client");
		if (conn == null)
			throw new NullPointerException("conn");
		this.client = client;
		this.conn = conn;
	}

	public JmxClient getJmxClient() {
		return client;
	}

	public void reply(Builder message) {
		assert message.hasType() || message.hasTo();
		assert message.hasId();
		getJmxClient().send(message.build(), SendingMethod.via(conn));
		setResponseSent(true);
	}
}