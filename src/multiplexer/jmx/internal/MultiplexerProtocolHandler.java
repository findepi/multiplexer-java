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

import com.google.protobuf.ByteString;

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

		if (!(e.getMessage() instanceof MultiplexerMessage)) {
			ctx.sendUpstream(e);
			return;
		}

		MultiplexerMessage message = (MultiplexerMessage) e.getMessage();
		if (message.getType() != MessageTypes.HEARTBIT) {
			if (logger.isDebugEnabled()) {
				logger.debug("Received\n{}", makeShortDebugMessage(message));
			}
			connectionsManager.messageReceived(message, ctx.getChannel());
		}
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
		throws Exception {
		
		if (!(e.getMessage() instanceof MultiplexerMessage)) {
			ctx.sendDownstream(e);
			return;
		}

		if (logger.isDebugEnabled()) {
			MultiplexerMessage message = (MultiplexerMessage) e.getMessage();

			if (message.getType() != MessageTypes.HEARTBIT) {
				logger.debug("Writing\n{}", makeShortDebugMessage(message));
			}
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

	private static MultiplexerMessage makeShortDebugMessage(
		MultiplexerMessage message) {
		if (message.getMessage().size() < 256) {
			return message;
		} else {
			return message.toBuilder().setMessage(
				ByteString.copyFromUtf8("<message length="
					+ message.getMessage().size() + ">")).build();
		}
	}
}
