package multiplexer.jmx.tools.rulesconsts;

/**
 * @author Piotr Findeisen
 */
public class UserspaceConstantsCompiler extends AbstractRulesCompiler {

	@Override
	protected void validateValues(ConstantsGroup group)
		throws InvalidRulesFileException {
		// TODO validate group against system constants 
	
//		checkNoDuplicates(group.m)
	}
}
