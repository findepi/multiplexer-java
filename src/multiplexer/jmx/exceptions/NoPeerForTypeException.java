package multiplexer.jmx.exceptions;

/**
 * @author Piotr Findeisen
 */
public class NoPeerForTypeException extends JmxException {

	private static final long serialVersionUID = -4107987618122058520L;

	public NoPeerForTypeException() {
		super();
	}

	public NoPeerForTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoPeerForTypeException(String message) {
		super(message);
	}

	public NoPeerForTypeException(Throwable cause) {
		super(cause);
	}

}
