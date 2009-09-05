package multiplexer.jmx.internal;

import java.lang.ref.WeakReference;
import java.nio.channels.ClosedChannelException;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * @author Piotr Findeisen
 */
public class WeakFailureSettingListener implements ChannelFutureListener {

	private final WeakReference<ChannelFuture> target;

	public WeakFailureSettingListener(ChannelFuture target) {
		this.target = new WeakReference<ChannelFuture>(target);
	}

	public void operationComplete(ChannelFuture future) throws Exception {
		ChannelFuture target = this.target.get();
		if (target != null) {
			target.setFailure(new ClosedChannelException());
		}
	}

}
