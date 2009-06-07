package multiplexer.jmx;

public class SendingMethod {

	public static final SendingMethod THROUGH_ONE = new SendingMethod();
	public static final SendingMethod THROUGH_ALL = new SendingMethod();

	private SendingMethod() {
	}

	public SendingMethod(Connection connection) {
		// TODO
		if (connection == null) {
			throw new NullPointerException("connection");
		}
		throw new RuntimeException("not implemented");
	}
}
