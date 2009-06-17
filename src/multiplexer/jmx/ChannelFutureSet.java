/**
 * 
 */
package multiplexer.jmx;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.google.common.collect.ForwardingSet;

/**
 * @author Kasia Findeisen
 *
 */
public class ChannelFutureSet extends ForwardingSet<ChannelFuture> {
	
	private Set<ChannelFuture> channelFutures = new HashSet<ChannelFuture>();
	private ChannelFutureListener completionListener = new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					channelFutures.remove(future);
				}

			};
			
	public boolean add(ChannelFuture cf) {
		cf.addListener(completionListener);
		return super.add(cf);
	}
	
	public boolean addAll(Collection<? extends ChannelFuture> cfCollection) {
		for (ChannelFuture cf: cfCollection) {
			cf.addListener(completionListener);
		}
		return super.addAll(cfCollection);
	}

	@Override
	protected Set<ChannelFuture> delegate() {
		return channelFutures;
	}

}
