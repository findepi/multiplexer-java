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

/**
 * Indicates that rules file used to generate constants is invalid.
 *
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
