package multiplexer.jmx.internal;

import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Piotr Findeisen
 */
@ChannelPipelineCoverage("all")
public class ChannelBufferFactorySettingHandler implements
	ChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory
		.getLogger(ChannelBufferFactorySettingHandler.class);

	/**
	 * Internal.
	 */
	public static final ChannelBufferFactorySettingHandler LITTLE_ENDIAN_BUFFER_FACTORY_SETTER = new ChannelBufferFactorySettingHandler(
		HeapChannelBufferFactory.getInstance(ByteOrder.LITTLE_ENDIAN));

	private final ChannelBufferFactory factory;

	public ChannelBufferFactorySettingHandler(ChannelBufferFactory factory) {
		this.factory = factory;
	}

	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {
		logger.debug("CBFSH welcomes {}", ctx.getChannel());
		e.getChannel().getConfig().setBufferFactory(factory);
		ctx.getPipeline().remove(this);
		ctx.sendUpstream(e);
	}
}
