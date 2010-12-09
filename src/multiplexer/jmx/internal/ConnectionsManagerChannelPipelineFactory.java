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

package multiplexer.jmx.internal;

import java.util.concurrent.TimeUnit;

import multiplexer.protocol.Protocol;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;

/**
 * @author Piotr Findeisen
 */
public class ConnectionsManagerChannelPipelineFactory implements
	ChannelPipelineFactory {

	// helpers
	private final Timer timer;

	// Encoders
	private static final RawMessageFrameEncoder rawMessageEncoder = new RawMessageFrameEncoder();
	private static final ProtobufEncoder multiplexerMessageEncoder = new ProtobufEncoder();
	// Decoders
	private static final ProtobufDecoder multiplexerMessageDecoder = new ProtobufDecoder(
		Protocol.MultiplexerMessage.getDefaultInstance());
	// Heartbits
	private static final HeartbitHandler heartbitHandler = new HeartbitHandler();
	// Protocol handler
	private final MultiplexerProtocolHandler multiplexerProtocolHandler;

	ConnectionsManagerChannelPipelineFactory(Timer timer,
		MultiplexerProtocolListener protocolListener) {
		this.timer = timer;
		multiplexerProtocolHandler = new MultiplexerProtocolHandler(
			protocolListener);
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();

		// Configuration
		pipeline
			.addFirst(
				"littleEndianEndiannessSetter",
				ChannelBufferFactorySettingHandler.LITTLE_ENDIAN_BUFFER_FACTORY_SETTER);

		// Encoders
		pipeline.addLast("rawMessageEncoder", rawMessageEncoder);
		pipeline
			.addLast("multiplexerMessageEncoder", multiplexerMessageEncoder);

		// Decoders
		pipeline.addLast("rawMessageDecoder", new RawMessageFrameDecoder());
		pipeline
			.addLast("multiplexerMessageDecoder", multiplexerMessageDecoder);

		// Heartbits
		pipeline.addLast("idleHandler", new IdleStateHandler(timer,
			Config.INITIAL_READ_IDLE_TIME, Config.INITIAL_WRITE_IDLE_TIME,
			Long.MAX_VALUE, TimeUnit.SECONDS));

		pipeline.addLast("heartbitHandler", heartbitHandler);

		// Protocol handler
		pipeline.addLast("multiplexerProtocolHandler",
			multiplexerProtocolHandler);

		return pipeline;
	}

}
