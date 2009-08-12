package multiplexer.jmx.internal;

import multiplexer.protocol.Constants.MessageTypes;
import multiplexer.protocol.Protocol.MultiplexerMessage;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelPipelineCoverage("all")
public class MultiplexerProtocolHandler extends SimpleChannelHandler {

	private static final Logger logger = LoggerFactory
		.getLogger(MultiplexerProtocolHandler.class);

	private MultiplexerProtocolListener connectionsManager;

	public MultiplexerProtocolHandler(
		MultiplexerProtocolListener connectionsManager) {
		this.connectionsManager = connectionsManager;
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
		ChannelStateEvent e) throws Exception {

		connectionsManager.channelDisconnected(e.getChannel());
		ctx.sendUpstream(e);
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
		throws Exception {

		connectionsManager.channelOpen(e.getChannel());
		ctx.sendUpstream(e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

		assert e.getMessage() instanceof MultiplexerMessage : e.getMessage()
			+ " is not a MultiplexerMessage";

		if (((MultiplexerMessage) e.getMessage()).getType() != MessageTypes.HEARTBIT) {
			logger.debug("MessageReceived\n{}", e.getMessage());
			connectionsManager.messageReceived((MultiplexerMessage) e
				.getMessage(), ctx.getChannel());
		}
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
		throws Exception {

		assert e.getMessage() instanceof MultiplexerMessage : "You should feed the channel with MultiplexerMessages, not "
			+ e.getMessage();

		if (((MultiplexerMessage) e.getMessage()).getType() != MessageTypes.HEARTBIT) {
			logger.debug("Writing\n{}", e.getMessage());
		}

		ctx.sendDownstream(e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		if (logger.isWarnEnabled()) {
			logger
				.warn("Unhandled exception on "
					+ ctx.getChannel()
					+ ", open="
					+ (ctx.getChannel() != null ? ctx.getChannel().isOpen()
						: "null") + ", manager=" + connectionsManager, e
					.getCause());
		}
		Channels.close(e.getChannel());
	}
}
