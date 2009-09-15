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

import gnu.trove.TLongHashSet;

import java.util.LinkedList;

/**
 * A set-like structure of longs that stores only {@code CAPACITY} recent
 * values.
 * 
 * @author Kasia Findeisen
 * @author Piotr Findeisen
 */
public class RecentLongPool {

	public static final int CAPACITY = 20000;

	private TLongHashSet ids = new TLongHashSet();
	private LinkedList<Long> recent = new LinkedList<Long>();

	public synchronized boolean add(long id) {
		if (ids.add(id)) {
			recent.addFirst(id);
			if (ids.size() > CAPACITY) {
				long removed = recent.removeLast();
				ids.remove(removed);
			}
			return true;
		}
		return false;
	}
}
