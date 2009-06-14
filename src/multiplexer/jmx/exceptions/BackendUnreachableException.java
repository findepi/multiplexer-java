package multiplexer.jmx.exceptions;

/**
 * @author Piotr Findeisen
 */
public class BackendUnreachableException extends OperationFailedException {

	private static final long serialVersionUID = -3512977545317977998L;

	public BackendUnreachableException() {
		super();
	}

	public BackendUnreachableException(String message, Throwable cause) {
		super(message, cause);
	}

	public BackendUnreachableException(String message) {
		super(message);
	}

	public BackendUnreachableException(Throwable cause) {
		super(cause);
	}
}
