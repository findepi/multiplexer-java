package multiplexer.jmx;

/**
 * TODO javadoc
 * @author Piotr Findeisen
 *
 */
public final class SendingMethod {
	
	public static final SendingMethod THROUGH_ONE = new SendingMethod();
	public static final SendingMethod THROUGH_ALL = new SendingMethod();
	
	public static ViaConnection via(Connection connection) {
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		return new ViaConnection(connection);
	}

	private SendingMethod() {
	}

	public static final class ViaConnection {
		private final Connection connection;

		private ViaConnection(Connection connection) {
			super();
			this.connection = connection;
		}

		public Connection getConnection() {
			return connection;
		}
	}
}
