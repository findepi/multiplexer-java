package multiplexer.jmx.exceptions;

/**
 * @author Piotr Findeisen
 */
public class JmxException extends Exception {

	private static final long serialVersionUID = -7869186798714620555L;

	public JmxException() {
		super();
	}

	public JmxException(String message, Throwable cause) {
		super(message, cause);
	}

	public JmxException(String message) {
		super(message);
	}

	public JmxException(Throwable cause) {
		super(cause);
	}

}
