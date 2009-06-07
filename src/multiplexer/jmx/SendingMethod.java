package multiplexer.jmx;

public class SendingMethod {
	
	private final Connection connection;

	public static final SendingMethod THROUGH_ONE = new SendingMethod();
	public static final SendingMethod THROUGH_ALL = new SendingMethod();

	private SendingMethod() {
		connection = null;
	}

	public SendingMethod(Connection connection) {
		// TODO
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return connection;
	}
}
