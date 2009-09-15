// Copyright 2009 Warsaw University, Faculty of Physics
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package multiplexer.jmx.server;

import multiplexer.jmx.internal.ByteCountingHandler;
import multiplexer.jmx.internal.MessageCountingHandler;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 * @author Piotr Findeisen
 */
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
