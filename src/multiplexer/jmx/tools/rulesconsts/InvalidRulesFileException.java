package multiplexer.jmx.tools.rulesconsts;

/**
 * Indicates that rules file used to generate constants is invalid.
 * @author Piotr Findeisen
 */
public class InvalidRulesFileException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidRulesFileException() {
		super();
	}

	public InvalidRulesFileException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public InvalidRulesFileException(String arg0) {
		super(arg0);
	}

	public InvalidRulesFileException(Throwable arg0) {
		super(arg0);
	}
}
