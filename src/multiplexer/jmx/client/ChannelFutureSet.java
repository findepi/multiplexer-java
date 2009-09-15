package multiplexer.jmx.client;

import java.util.Collection;
import java.util.Set;

import multiplexer.jmx.util.ConcurrentHashSet;
import multiplexer.jmx.util.ConcurrentSet;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.google.common.collect.ForwardingSet;

/**
 * A Set of {@link ChannelFuture}s which allows concurrent operations (addition,
 * removal) and iteration,
 * 
 * @author Kasia Findeisen
 */
public class ChannelFutureSet extends ForwardingSet<ChannelFuture> {

	private ConcurrentSet<ChannelFuture> channelFutures = new ConcurrentHashSet<ChannelFuture>();
	private ChannelFutureListener completionListener = new ChannelFutureListener() {

		public void operationComplete(ChannelFuture future) throws Exception {
			channelFutures.remove(future);
		}
	};

	@Override
	public boolean add(ChannelFuture cf) {
		cf.addListener(completionListener);
		return super.add(cf);
	}

	@Override
	public boolean addAll(Collection<? extends ChannelFuture> cfCollection) {
		boolean modified = false;
		for (ChannelFuture cf : cfCollection) {
			modified = add(cf) || modified;
		}
		return modified;
	}

	@Override
	protected Set<ChannelFuture> delegate() {
		return channelFutures;
	}
}
