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

package multiplexer.jmx.client;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import multiplexer.jmx.util.TimeoutCounter;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link ChannelFuture} implementation for dealing with groups of
 * {@code ChannelFuture} objects. The semantic of {@code ChannelFutureGroup} is
 * aimed do be similar to other known {@code ChannelFuture} implementations'.
 * The properties of {@code ChannelFutureGroup}, such as {@code isDone,
 * isSuccess, isCancelled} are intuitively extended over a set of {@code
 * ChannelFuture} elements. However, as {@code ChannelFuture} objects may be
 * added to the {@code ChannelFutureGroup} at any point, it is possible that
 * {@code isDone} method returns {@code true} at some points and {@code false}
 * in the meantime (when another uncompleted {@code ChannelFuture} was added).
 * Thus, there might be multiple points of notifying listeners added to the
 * {@code ChannelFutureGroup}.
 * 
 * @author Kasia Findeisen
 */
public final class ChannelFutureGroup implements ChannelFuture {

	private static final Logger logger = LoggerFactory
		.getLogger(ChannelFutureGroup.class);

	private Set<ChannelFuture> channelFutures = new HashSet<ChannelFuture>();
	private List<ChannelFutureListener> listeners = new LinkedList<ChannelFutureListener>();
	/**
	 * A counter for uncompleted {@code ChannelFuture}s. It is used only when a
	 * {@code Listener} was added to the {@code ChannelFutureGroup}. In other
	 * case, it remains set to {@code 0}.
	 */
	private int tracedNotCompleted = 0;
	private ChannelFutureListener completionListener = new ChannelFutureListener() {
		public void operationComplete(ChannelFuture future) throws Exception {
			tracedNotCompleted--;
			if (tracedNotCompleted == 0) {
				notifyListeners();
			}
		}

	};

	/**
	 * Creates a new instance with an empty set of {@code ChannelFuture}
	 * elements.
	 */
	public ChannelFutureGroup() {
	}

	/**
	 * Creates a new instance with one {@code ChannelFuture} element.
	 */
	public ChannelFutureGroup(ChannelFuture cf) {
		add(cf);
	}

	/**
	 * Creates a new instance with a given set of {@code ChannelFuture}
	 * elements.
	 */
	public ChannelFutureGroup(Set<ChannelFuture> cfSet) {
		for (ChannelFuture cf : cfSet) {
			add(cf);
		}
	}

	/**
	 * Adds a new {@code ChannelFuture} element.
	 * 
	 * @param cf
	 *            a new element
	 */
	public void add(ChannelFuture cf) {
		channelFutures.add(cf);
		if (!listeners.isEmpty()) {
			tracedNotCompleted++;
			cf.addListener(completionListener);
		}
	}

	/**
	 * Number of {@code ChannelFuture} elements in the {@code
	 * ChannelFutureGroup}.
	 */
	public int size() {
		return channelFutures.size();
	}

	/**
	 * Adds a new {@code Listener} to the {@code ChannelFutureGroup}. The
	 * {@code Listener} will be notified when all {@code ChannelFuture}s are
	 * completed (method {@code isDone()} returns {@code true}). As {@code
	 * ChannelFuture} objects may be added to the {@code ChannelFutureGroup} at
	 * any point, it is possible that {@code isDone} method returns {@code true}
	 * at some points and {@code false} in the meantime (when another
	 * uncompleted {@code ChannelFuture} was added). Thus, there might be
	 * multiple points of notifying listeners.
	 * 
	 */
	public void addListener(ChannelFutureListener listener) {
		assert (listener != null) : "addListener";
		boolean notifyNow = false;
		synchronized (this) {
			if (isDone()) {
				notifyNow = true;
			} else {
				if (listeners.isEmpty()) {
					for (ChannelFuture cf : channelFutures) {
						tracedNotCompleted++;
						cf.addListener(completionListener);
					}
				}
				listeners.add(listener);
			}
		}

		if (notifyNow) {
			notifyListener(listener);
		}

	}

	private void notifyListener(ChannelFutureListener listener) {
		try {
			listener.operationComplete(this);
		} catch (Exception e) {
			logger.warn("An exception was thrown by "
				+ ChannelFutureListener.class.getSimpleName(), e);
		}
	}

	private void notifyListeners() {
		List<ChannelFutureListener> listenersCopy = new LinkedList<ChannelFutureListener>();
		synchronized (this) {
			listenersCopy.addAll(listeners);
			listeners.clear();
		}
		for (ChannelFutureListener listener : listenersCopy) {
			notifyListener(listener);
		}
	}

	/**
	 * Blocks until the {@code ChannelFutureGroup} {@code isDone}. Calls {@code
	 * await()} on all the {@code ChannelFutureGroup}'s elements.
	 */
	public ChannelFuture await() throws InterruptedException {
		for (ChannelFuture cf : channelFutures) {
			cf.await();
		}
		return this;
	}

	/**
	 * Blocks uninterruptibly until the {@code ChannelFutureGroup} {@code
	 * isDone}. Calls {@code awaitUninterruptibly()} on all the {@code
	 * ChannelFutureGroup}'s elements.
	 */
	public ChannelFuture awaitUninterruptibly() {
		for (ChannelFuture cf : channelFutures) {
			cf.awaitUninterruptibly();
		}
		return this;
	}

	/**
	 * Blocks until the {@code ChannelFutureGroup} {@code isDone} or {@code
	 * timeoutMillis} elapses.
	 */
	public boolean await(long timeoutMillis) throws InterruptedException {
		return await(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * Blocks until the {@code ChannelFutureGroup} {@code isDone} or {@code
	 * timeout} given in specified {@code TimeUnit} elapses.
	 */
	public boolean await(long timeout, TimeUnit unit)
		throws InterruptedException {
		TimeoutCounter timeoutCounter = new TimeoutCounter(timeout, unit);
		for (ChannelFuture cf : channelFutures) {
			if (!cf.await(timeoutCounter.getRemainingMillis())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Blocks uninterruptibly until the {@code ChannelFutureGroup} {@code
	 * isDone} or {@code timeoutMillis} elapses.
	 */
	public boolean awaitUninterruptibly(long timeoutMillis) {
		return awaitUninterruptibly(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * Blocks uninterruptibly until the {@code ChannelFutureGroup} {@code
	 * isDone} or {@code timeout} given in specified {@code TimeUnit} elapses.
	 */
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		TimeoutCounter timeoutCounter = new TimeoutCounter(timeout, unit);
		for (ChannelFuture cf : channelFutures) {
			if (!cf.awaitUninterruptibly(timeoutCounter.getRemainingMillis())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Cancels all {@code ChannelFuture}s of the {@code ChannelFutureGroup}.
	 * 
	 */
	public boolean cancel() {
		boolean ret = true;
		for (ChannelFuture cf : channelFutures) {
			ret &= cf.cancel();
		}
		return ret;
	}

	/**
	 * Returns any not-null {@code Cause} of one of the {@code
	 * ChannelFutureGroup}'s {@code ChannelFuture}s or null, if {@code Cause} is
	 * unavailable.
	 */
	public Throwable getCause() {
		for (ChannelFuture cf : channelFutures) {
			if (cf.getCause() != null) {
				return cf.getCause();
			}
		}
		return null;
	}

	public Channel getChannel() {
		throw new RuntimeException("Not implemented.");
	}

	/**
	 * True if and only if every {@code ChannelFuture} of the {@code
	 * ChannelFutureGroup} {@code isCancelled}.
	 */
	public boolean isCancelled() {
		for (ChannelFuture cf : channelFutures) {
			if (!cf.isCancelled()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * True if and only if every {@code ChannelFuture} of the {@code
	 * ChannelFutureGroup} {@code isDone}.
	 */
	public boolean isDone() {
		for (ChannelFuture cf : channelFutures) {
			if (!cf.isDone()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * True if and only if every {@code ChannelFuture} of the {@code
	 * ChannelFutureGroup} {@code isSuccess}.
	 */
	public boolean isSuccess() {
		for (ChannelFuture cf : channelFutures) {
			if (!cf.isSuccess()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes specified {@code Listener} from the {@code ChannelFutureGroup}.
	 */
	public void removeListener(ChannelFutureListener listener) {
		assert (listener != null) : "removeListener";
		synchronized (this) {
			listeners.remove(listener);
		}
	}

	public boolean setFailure(Throwable cause) {
		throw new RuntimeException("Not implemented.");
	}

	public boolean setSuccess() {
		throw new RuntimeException("Not implemented.");
	}

	public boolean setProgress(long amount, long current, long total) {
		return false;
	}
}
