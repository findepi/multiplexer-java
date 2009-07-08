package multiplexer.jmx;

import multiplexer.Multiplexer.MultiplexerMessage;
import multiplexer.Multiplexer.MultiplexerMessage.Builder;

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
