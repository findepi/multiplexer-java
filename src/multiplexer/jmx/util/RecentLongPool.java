package multiplexer.jmx.util;

import gnu.trove.TLongHashSet;

import java.util.ArrayDeque;
import java.util.Deque;

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
	private Deque<Long> recent = new ArrayDeque<Long>();

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
