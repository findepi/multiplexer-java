// Copyright 2009 Warsaw University, Faculty of Physics
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package multiplexer.jmx.tools.rulesconsts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Piotr Findeisen
 */
public class UserspaceConstantsCompiler extends AbstractRulesCompiler {

	@Override
	protected void validateValues(ConstantsGroup group)
		throws InvalidRulesFileException {

		List<String> checkConstants = new ArrayList<String>();
		checkConstants.add(multiplexer.protocol.Constants.class.getName());
		if (this.options.checkConstants != null)
			checkConstants.addAll(this.options.checkConstants);

		for (String className : checkConstants) {
			ConstantsPack otherConstants;
			try {
				otherConstants = loadConstantsPack(className);

			} catch (ClassNotFoundException e) {
				throw new RuntimeException(
					"Unable to load resources for checking.", e);
			} catch (InstantiationException e) {
				throw new RuntimeException(
					"Unable to load resources for checking.", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(
					"Unable to load resources for checking.", e);
			}

			validateConstants(group.getPeerTypes(), otherConstants
				.getPeerTypes(), "PeerTypes", className);
			validateConstants(group.getMessageTypes(), otherConstants
				.getMessageTypes(), "MessageTypes", className);
		}
	}

	private ConstantsPack loadConstantsPack(String className)
		throws InstantiationException, IllegalAccessException,
		ClassNotFoundException {

		Class<?> otherConstantsClass = Class.forName(className);
		return (ConstantsPack) otherConstantsClass.newInstance();
	}

	private <T> void validateConstants(List<Constant<T>> targetConstants,
		Constants againstConstants, String groupName, String otherSource) {

		final Map<Integer, String> otherConstantsNames = againstConstants
			.getConstantsNames();
		for (Constant<T> c : targetConstants) {
			if (otherConstantsNames.containsKey(c.getValue())) {
				throw new RuntimeException("Constant " + c.getValue() + " ("
					+ c.getName() + ") in group " + groupName
					+ " is also specified in " + otherSource + " as "
					+ otherConstantsNames.get(c.getValue()));
			}

		}
	}
}
