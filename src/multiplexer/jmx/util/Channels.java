package multiplexer.jmx.util;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroupFuture;

public class Channels {
	private Channels() {
	}

	public static void awaitSemiInterruptibly(ChannelFuture future,
		int ignoredInterruptsNumber) throws InterruptedException {
		
		while (ignoredInterruptsNumber > 0) {
			try {
				future.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				ignoredInterruptsNumber --;
			}
		}
		future.await();
	}
	
	public static void awaitSemiInterruptibly(ChannelGroupFuture future,
		int ignoredInterruptsNumber) throws InterruptedException {
		
		while (ignoredInterruptsNumber > 0) {
			try {
				future.await();
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				ignoredInterruptsNumber --;
			}
		}
		future.await();
	}
	
}
