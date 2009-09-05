package multiplexer.jmx.client;

/**
 * @author Piotr Findeisen
 */
public class ConnectException extends Exception {

	private static final long serialVersionUID = 6706452969262144643L;

	public ConnectException() {
	}

	public ConnectException(String message) {
		super(message);
	}

	public ConnectException(Throwable cause) {
		super(cause);
	}

	public ConnectException(String message, Throwable cause) {
		super(message, cause);
	}
}
