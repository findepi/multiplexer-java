package multiplexer.jmx.tools;

import java.io.FileNotFoundException;
import java.io.IOException;

import multiplexer.jmx.tools.rulesconsts.ConstantsFromRulesCompiler;
import multiplexer.jmx.tools.rulesconsts.InvalidRulesFileException;
import multiplexer.jmx.tools.rulesconsts.Options;
import multiplexer.jmx.tools.rulesconsts.SystemConstantsCompiler;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * @author Piotr Findeisen
 * 
 */
public class CompileConstants {

	public static void main(String[] args) throws ClassNotFoundException,
		InstantiationException, IllegalAccessException, FileNotFoundException,
		IOException, InvalidRulesFileException {

		Options options = new Options();
		CmdLineParser optionsParser = new CmdLineParser(options);

		try {
			optionsParser.parseArgument(args);
		} catch (CmdLineException e) {
			usage(e.getMessage(), optionsParser);
			System.exit(1);
		}

		if (options.rulesFile == null) {
			usage("Input file not specified", optionsParser);
			System.exit(1);
		}

		// CompileConstants compiler;
		Class<?> compilerClass = getCompilerClass(options);
		ConstantsFromRulesCompiler compiler = (ConstantsFromRulesCompiler) compilerClass
			.newInstance();
		compiler.compile(options);
	}

	public static void usage(String error, CmdLineParser optionsParser) {
		System.err.println(error);
		System.err.println("java " + CompileConstants.class.getName()
			+ " [options...] <multiplexer rules file>");
		System.err.println();
		System.err
			.println("Available options are listed below. You must use either -output or -class.");
		optionsParser.printUsage(System.err);
	}

	private static Class<?> getCompilerClass(Options options)
		throws ClassNotFoundException {
		if (options.system) {
			// This class must be always available, e.g. its compilation must
			// not require previous build of any constants.
			return SystemConstantsCompiler.class;
		} else {
			// This class may require constants created by
			// SystemConstantsCompiler, so it must be loaded dynamically.
			return Class
				.forName("multiplexer.jmx.tools.rulesconsts.UserspaceConstantsCompiler");
		}
	}
}
