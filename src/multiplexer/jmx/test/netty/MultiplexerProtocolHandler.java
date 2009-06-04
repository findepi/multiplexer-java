package multiplexer.jmx.test.netty;

import multiplexer.Multiplexer.MultiplexerMessage;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

@ChannelPipelineCoverage("all")
class MultiplexerProtocolHandler extends SimpleChannelHandler {

	private SimpleNettyConnection connection;

	public MultiplexerProtocolHandler(SimpleNettyConnection connection) {
		this.connection = connection;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		System.err.println("messageReceived");
		assert e.getMessage() instanceof MultiplexerMessage : e.getMessage()
				+ " is not a MultiplexerMessage";
		System.err.println(e.getMessage());
		boolean offered = connection.queue.offer((MultiplexerMessage) e.getMessage());
		assert offered : "sorry not offered, offiaro";
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {

		assert e.getMessage() instanceof MultiplexerMessage : "You should feed the channel with MultiplexerMessages, not "
				+ e.getMessage();
		super.writeRequested(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}