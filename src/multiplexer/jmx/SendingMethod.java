package multiplexer.jmx;

/**
 * TODO javadoc
 * @author Piotr Findeisen
 *
 */
public final class SendingMethod {
	
	private final Connection connection;

	public static final SendingMethod THROUGH_ONE = new SendingMethod();
	public static final SendingMethod THROUGH_ALL = new SendingMethod();
	
	public static SendingMethod via(Connection connection) {
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		return new SendingMethod(connection);
	}

	private SendingMethod() {
		connection = null;
	}

	private SendingMethod(Connection connection) {
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return connection;
	}
}
