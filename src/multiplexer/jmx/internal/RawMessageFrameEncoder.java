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
