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
