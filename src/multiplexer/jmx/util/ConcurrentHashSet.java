package multiplexer.jmx.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of concurrent Set that supports thread-safe
 * modifications and iteration.
 * 
 * @author Piotr Findeisen
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> {
	
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
