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

import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import static multiplexer.jmx.internal.RawMessageFrame.getCrc32;

/**
 * RawMessageFrameEncoder is responsible for creating frames in the format
 * 
 * <pre>
 * [ length ][ crc ][ message... ]
 * </pre>
 * 
 * (thus, as expected by RawMessageFrameDecoder)
 *
 * @author Piotr Findeisen
 */
@ChannelPipelineCoverage("all")
public class RawMessageFrameEncoder extends OneToOneEncoder {

	private static final int HEADER_LENGTH = 8;

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel,
		Object msg) throws Exception {

		ChannelBuffer serialized = (ChannelBuffer) msg;
		int length = serialized.readableBytes();
		long crc = getCrc32(serialized);

		ChannelBuffer rawMessage = ChannelBuffers.buffer(
			ByteOrder.LITTLE_ENDIAN, HEADER_LENGTH + length);
		rawMessage.writeInt(length);
		rawMessage.writeInt((int) crc);
		rawMessage.writeBytes(serialized);

		return rawMessage;
	}
}
