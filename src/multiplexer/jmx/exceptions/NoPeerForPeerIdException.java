package multiplexer.jmx.exceptions;

/**
 * @author Piotr Findeisen
 */
public class NoPeerForPeerIdException extends JmxException {

	private static final long serialVersionUID = 7696681340845807200L;

	public NoPeerForPeerIdException() {
		super();
	}

	public NoPeerForPeerIdException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoPeerForPeerIdException(String message) {
		super(message);
	}

	public NoPeerForPeerIdException(Throwable cause) {
		super(cause);
	}

}
