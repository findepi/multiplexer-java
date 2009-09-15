package multiplexer.jmx.server;

import multiplexer.jmx.internal.ByteCountingHandler;
import multiplexer.jmx.internal.MessageCountingHandler;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

class ServerChannelPipelineFactory implements ChannelPipelineFactory {

	private final ChannelPipelineFactory connectionsManagerPipelineFactory;
	private final ByteCountingHandler byteCountingHandler = new ByteCountingHandler();
	private final MessageCountingHandler messageCountingHandler = new MessageCountingHandler();

	ServerChannelPipelineFactory(
		ChannelPipelineFactory connectionsManagerPipelineFactory) {
		this.connectionsManagerPipelineFactory = connectionsManagerPipelineFactory;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = connectionsManagerPipelineFactory
			.getPipeline();

		pipeline.addFirst("byteCounter", byteCountingHandler);
		pipeline.addBefore("multiplexerProtocolHandler", "messageCounter",
			messageCountingHandler);

		return pipeline;
	}

	public ByteCountingHandler getByteCountingHandler() {
		return byteCountingHandler;
	}

	public MessageCountingHandler getMessageCountingHandler() {
		return messageCountingHandler;
	}
}
