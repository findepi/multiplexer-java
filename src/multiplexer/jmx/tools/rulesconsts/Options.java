package multiplexer.jmx.tools.rulesconsts;

import org.kohsuke.args4j.Option;

public class Options {
	@Option(name = "-system", usage = "indicate to generate constants for internal use by library")
	public boolean system = false;

	@Option(name = "-input", usage = "input file with text-encoded MultiplexerRules protobuf")
	public String rulesFile;

	@Option(name = "-outdir", usage = "where to put generated files")
	public String outputRoot;

	@Option(name = "-output", usage = "path to generate file within output dir")
	public String outputFile;

	@Option(name = "-class", usage = "name of the generated class")
	public String outputClass;

	// inferred
	public String outputClassName;

	@Option(name = "-package", usage = "name of the package for generated class; use '' for default")
	public String packageName;
}