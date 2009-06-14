package multiplexer.jmx.exceptions;

/**
 * @author Piotr Findeisen
 */
public class OperationFailedException extends JmxException {

	private static final long serialVersionUID = 4986460719353186716L;

	public OperationFailedException() {
		super();
	}

	public OperationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public OperationFailedException(String message) {
		super(message);
	}

	public OperationFailedException(Throwable cause) {
		super(cause);
	}
}
