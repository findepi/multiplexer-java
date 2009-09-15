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

import java.util.concurrent.TimeUnit;

/**
 * Helper class for timeout management. Used time unit is milliseconds. Each
 * instance gets it's creation time and specified timeout. It might check, at
 * any point, whether the timeout has passed.
 * 
 * @author Kasia Findeisen
 */
public class TimeoutCounter {
	private final long startTime = System.currentTimeMillis();
	private long timeoutInMillis;

	/**
	 * Creates a new instance which might be asked if a specific timeout (
	 * {@code timeoutInMillis}) has passed since it's creation time.
	 * 
	 * @param timeoutInMillis
	 *            timeout in milliseconds
	 */
	public TimeoutCounter(long timeoutInMillis) {
		this.timeoutInMillis = timeoutInMillis;
	}

	/**
	 * Creates a new instance which might be asked if a specific timeout (
	 * {@code timeout}) in a specified time unit ({@code TimeUnit}) has passed
	 * since it's creation time.
	 * 
	 * @param timeout
	 *            timeout in time unit
	 * @param unit
	 *            time unit
	 */
	public TimeoutCounter(long timeout, TimeUnit unit) {
		this(unit.toMillis(timeout));
	}

	/**
	 * Returns the amount of time in milliseconds that has passed since the
	 * instance's creation time.
	 * 
	 * @return elapsed time in milliseconds
	 */
	public long getElapsedMillis() {
		return (System.currentTimeMillis() - startTime);
	}

	/**
	 * Returns the amount of time remaining until {@code timeoutInMillis} passes
	 * or {@code 0} if it has happened already.
	 * 
	 * @return remaining time in milliseconds
	 */
	public long getRemainingMillis() {
		long remainingMillis = timeoutInMillis - getElapsedMillis();
		if (remainingMillis >= 0) {
			return remainingMillis;
		}
		return 0;
	}
}
