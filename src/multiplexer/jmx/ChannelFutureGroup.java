package multiplexer.jmx;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * 
 * @author Kasia Findeisen
 * 
 */
public final class ChannelFutureGroup implements ChannelFuture {
	private static final Logger logger = Logger
			.getLogger(ChannelFutureGroup.class.getName());
	private Set<ChannelFuture> channelFutures = new HashSet<ChannelFuture>();
	private List<ChannelFutureListener> listeners = new LinkedList<ChannelFutureListener>();
	private int notCompleted = 0;
	private ChannelFutureListener completionListener = new ChannelFutureListener() {
//TODO dodawać completionListenery tylko, gdy są listenery na Groupie.
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			notCompleted--;
			if (notCompleted == 0) {
				notifyListeners();
			}
		}

	};

	public ChannelFutureGroup() {
	}

	public ChannelFutureGroup(ChannelFuture cf) {
		add(cf);
	}

	public ChannelFutureGroup(Set<ChannelFuture> cfSet) {
		for (ChannelFuture cf : cfSet) {
			add(cf);
		}
	}

	public void add(ChannelFuture cf) {
		channelFutures.add(cf);
		notCompleted++;
		cf.addListener(completionListener);
	}

	public int size() {
		return channelFutures.size();
	}

	@Override
	public void addListener(ChannelFutureListener listener) {
		assert (listener != null) : "addListener";

		boolean notifyNow = false;
		synchronized (this) {
			if (isDone()) {
				notifyNow = true;
			} else {
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
			logger.log(Level.WARNING, "An exception was thrown by "
					+ ChannelFutureListener.class.getSimpleName() + ".", e);
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

	@Override
	public ChannelFuture await() throws InterruptedException {
		for (ChannelFuture cf : channelFutures) {
			cf.await();
		}
		return this;
	}

	@Override
	public ChannelFuture awaitUninterruptibly() {
		for (ChannelFuture cf : channelFutures) {
			cf.awaitUninterruptibly();
		}
		return this;
	}

	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		return await(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	@Override
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

	@Override
	public boolean awaitUninterruptibly(long timeoutMillis) {
		return awaitUninterruptibly(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		TimeoutCounter timeoutCounter = new TimeoutCounter(timeout, unit);
		for (ChannelFuture cf : channelFutures) {
			if (!cf.awaitUninterruptibly(timeoutCounter.getRemainingMillis())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean cancel() {
		boolean ret = true;
		for (ChannelFuture cf : channelFutures) {
			ret &= cf.cancel();
		}
		return ret;
	}

	@Override
	public Throwable getCause() {
		for (ChannelFuture cf : channelFutures) {
			if (cf.getCause() != null) {
				return cf.getCause();
			}
		}
		return null;
	}

	@Override
	public Channel getChannel() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public boolean isCancelled() {
		for (ChannelFuture cf : channelFutures) {
			if (!cf.isCancelled()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isDone() {
		if (notCompleted == 0) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isSuccess() {
		for (ChannelFuture cf : channelFutures) {
			if (!cf.isSuccess()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void removeListener(ChannelFutureListener listener) {
		assert (listener != null) : "removeListener";
		synchronized (this) {
			listeners.remove(listener);
		}
	}

	@Override
	public boolean setFailure(Throwable cause) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public boolean setSuccess() {
		throw new RuntimeException("Not implemented.");
	}

}
