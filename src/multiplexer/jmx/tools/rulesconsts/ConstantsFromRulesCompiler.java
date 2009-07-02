package multiplexer.jmx.tools.rulesconsts;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * @author Piotr Findeisen
 */
public interface ConstantsFromRulesCompiler {

	public void compile(Options options) throws FileNotFoundException, IOException, InvalidRulesFileException;
}
