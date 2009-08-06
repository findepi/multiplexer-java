package multiplexer.jmx.internal;

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

/**
 * @author Piotr Findeisen
 * 
 */

@ChannelPipelineCoverage("all")
public class MessageCountingHandler implements ChannelUpstreamHandler,
	ChannelDownstreamHandler {

	private AtomicLong messagesOut = new AtomicLong();
	private AtomicLong messagesIn = new AtomicLong();

	public MessageCountingHandler() {
	}

	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateMessageCounter(e, messagesIn);
		ctx.sendUpstream(e);
	}

	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateMessageCounter(e, messagesOut);
		ctx.sendDownstream(e);
	}

	private void updateMessageCounter(ChannelEvent e, AtomicLong counter) {
		if (e instanceof MessageEvent) {
				counter.incrementAndGet();
		}
	}
	
	public long getMessagesOutCount() {
		return messagesOut.get();
	}
	
	public long getMessagesInCount() {
		return messagesIn.get();
	}

}
