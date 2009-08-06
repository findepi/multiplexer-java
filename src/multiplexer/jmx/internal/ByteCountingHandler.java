package multiplexer.jmx.internal;

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Piotr Findeisen
 * 
 */

@ChannelPipelineCoverage("all")
public class ByteCountingHandler implements ChannelUpstreamHandler,
	ChannelDownstreamHandler {

	private static final Logger logger = LoggerFactory
		.getLogger(ByteCountingHandler.class);

	private AtomicLong bytesOut = new AtomicLong();
	private AtomicLong bytesIn = new AtomicLong();

	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateByteCounter(e, bytesIn, "upstream");
		ctx.sendUpstream(e);
	}

	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateByteCounter(e, bytesOut, "downstream");
		ctx.sendDownstream(e);
	}

	private void updateByteCounter(ChannelEvent e, AtomicLong counter,
		String direction) {
		if (e instanceof MessageEvent) {
			Object message = ((MessageEvent) e).getMessage();
			if (!(message instanceof ChannelBuffer)) {
				logger.warn("ByteCountingHandler got {} message of type {}",
					direction, message.getClass().getName());
			} else {
				counter.addAndGet(((ChannelBuffer) message).readableBytes());
			}
		}
	}

	public long getBytesOutCount() {
		return bytesOut.get();
	}
	
	public long getBytesInCount() {
		return bytesIn.get();
	}
}
