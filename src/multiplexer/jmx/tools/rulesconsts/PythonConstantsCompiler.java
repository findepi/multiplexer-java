package multiplexer.jmx.tools.rulesconsts;

public class PythonConstantsCompiler extends UserspaceConstantsCompiler {
	
	private final boolean validate;
	
	public PythonConstantsCompiler() {
		this(true);
	}
	
	public PythonConstantsCompiler(boolean validate) {
		this.validate = validate;
	}

	@Override
	protected String getConstantsTemplateResourceName() {
		return "PythonConstants.template";
	}

	@Override
	protected void prepareOutputConfiguration(Options options) {
		if (!options.python) {
			throw new RuntimeException("-python option should be selected");
		}

		if (options.outputFile == null) {
			throw new RuntimeException("No output file chosen.");
		}
	}
	
	@Override
	protected void validateValues(ConstantsGroup group) throws InvalidRulesFileException {
		if (validate)
			super.validateValues(group);
	}
}
