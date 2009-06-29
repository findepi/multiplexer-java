/**
 * 
 */
package multiplexer.jmx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kasia Findeisen
 * 
 */
public class RecentIdPool {
	private Set<Long> ids = new HashSet<Long>();
	private Deque<Long> recent = new ArrayDeque<Long>();
	public static final int CAPACITY = 20000;

	public boolean add(long id) {
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
