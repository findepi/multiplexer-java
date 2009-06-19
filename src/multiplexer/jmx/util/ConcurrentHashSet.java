package multiplexer.jmx.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of concurrent Set that supports thread-safe
 * modifications and iteration.
 * 
 * @author Piotr Findeisen
 * 
 * @param <E>
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> {
	
	private final ConcurrentHashMap<E, Integer> elements = new ConcurrentHashMap<E, Integer>();

	@Override
	public boolean add(E e) {
		Integer previous = elements.put(e, 1);
		return previous == null || previous == 0;
	}

	@Override
	public void clear() {
		elements.clear();
	}

	@Override
	public boolean contains(Object o) {
		Integer count = elements.get(o);
		return count != null && count > 0;
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
		Integer previous = elements.remove(o);
		return previous != null && previous > 0;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		for (E e : this)
			if (!c.contains(e))
				if (remove(e))
					modified = true;
		return modified;
	}

	@Override
	public int size() {
		return elements.size();
	}
}
