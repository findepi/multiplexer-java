package multiplexer.jmx.exceptions;

/**
 * @author Piotr Findeisen
 */
public class OperationTimeoutException extends OperationFailedException {

	private static final long serialVersionUID = 9027875978659121784L;

	public OperationTimeoutException() {
		super();
	}

	public OperationTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public OperationTimeoutException(String message) {
		super(message);
	}

	public OperationTimeoutException(Throwable cause) {
		super(cause);
	}
}
