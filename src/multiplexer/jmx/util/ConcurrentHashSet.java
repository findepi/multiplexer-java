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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of concurrent Set that supports thread-safe
 * modifications and iteration.
 * 
 * Instantiating this class is roughly equivalent to
 * 
 * <pre>
 * Set&lt;E&gt; s = Collections.newSetFromMap(new ConcurrentHashMap&lt;E, Boolean&gt;());
 * </pre>
 * 
 * except that {@link ConcurrentHashSet} implements {@link ConcurrentSet}
 * interface in addition to plain {@link Set} interface.
 * 
 * @author Piotr Findeisen
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements
	ConcurrentSet<E> {

	private static final Object VALUE = new Object();
	private final ConcurrentHashMap<E, Object> elements = new ConcurrentHashMap<E, Object>();

	@Override
	public boolean add(E e) {
		return elements.put(e, VALUE) == null;
	}

	@Override
	public void clear() {
		elements.clear();
	}

	@Override
	public boolean contains(Object o) {
		return elements.containsKey(o);
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return elements.keySet().iterator();
	}

	@Override
	public boolean remove(Object o) {
		return elements.remove(o) == VALUE;
	}

	@Override
	public int size() {
		return elements.size();
	}
}
