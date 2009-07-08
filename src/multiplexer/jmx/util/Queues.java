package multiplexer.jmx.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * @author Piotr Findeisen
 */
public class Queues {
	private Queues() {
	}

	public static <E> E pollUninterruptibly(BlockingQueue<E> queue, TimeoutCounter timer) {
		while (true) {
			long remainingMillis = timer.getRemainingMillis();
			if (remainingMillis <= 0)
				break;
			try {
				return queue.poll(remainingMillis, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// no-op
			}
		}
		return null;
	}

	public static <E> E pollUninterruptibly(BlockingQueue<E> queue, long timeoutMillis) {
		return pollUninterruptibly(queue, new TimeoutCounter(timeoutMillis));
	}
}
