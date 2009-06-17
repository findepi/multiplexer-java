package multiplexer.jmx;

import multiplexer.Multiplexer.MultiplexerMessage;

/**
 * @author Piotr Findeisen
 */
public class IncomingMessageData {
	private final MultiplexerMessage message;
	private final Connection connection;

	public IncomingMessageData(MultiplexerMessage message, Connection connection) {
		this.message = message;
		this.connection = connection;
	}

	public MultiplexerMessage getMessage() {
		return message;
	}

	public Connection getConnection() {
		return connection;
	}
}