package multiplexer.jmx;

import java.util.Collection;
import java.util.Set;

import multiplexer.jmx.util.ConcurrentHashSet;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.google.common.collect.ForwardingSet;

/**
 * A Set of {@link ChannelFuture}s which allows concurrent operations (addition,
 * removal) and iteration,
 * 
 * @author Kasia Findeisen
 * 
 */
public class ChannelFutureSet<E extends ChannelFuture> extends ForwardingSet<E> {

	private Set<E> channelFutures = new ConcurrentHashSet<E>();
	private ChannelFutureListener completionListener = new ChannelFutureListener() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			channelFutures.remove(future);
		}
	};

	@Override
	public boolean add(E cf) {
		cf.addListener(completionListener);
		return super.add(cf);
	}

	@Override
	public boolean addAll(Collection<? extends E> cfCollection) {
		boolean modified = false;
		for (E cf : cfCollection) {
			modified = add(cf) || modified;
		}
		return modified;
	}

	@Override
	protected Set<E> delegate() {
		return channelFutures;
	}
}
