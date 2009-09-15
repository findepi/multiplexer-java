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

package multiplexer.jmx.util;

/**
 * @author Piotr Findeisen
 */
public class LongDeltaCounter {
	private long state = 0;

	public LongDeltaCounter() {
	}
	
	public LongDeltaCounter(long initialState) {
		state = initialState;
	}
	
	/**
	 * Calculates and returns delta from last recorded state to {@code newState}
	 * . This is not thread safe.
	 * 
	 * @param newState
	 *            new state
	 * @return delta from last state to new state
	 */
	public long deltaTo(long newState) {
		long delta = newState - state;
		state = newState;
		return delta;
	}
}
