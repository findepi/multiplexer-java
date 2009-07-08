package multiplexer.jmx.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RawMessageCodecs {

	private static final Logger logger = LoggerFactory
		.getLogger(RawMessageCodecs.class);

	private RawMessageCodecs() {
	}

	public static final int HEADER_LENGTH = 8;
	public static final int MAX_MESSAGE_SIZE = 128 * 1024 * 1024;

	/**
	 * RawMessageDecoderState is internally used by RawMessageFrameDecoder
	 */
	private enum RawMessageDecoderState {
		READ_HEADER, READ_MESSAGE,
	}

	/**
	 * RawMessageFrameDecoder is responsible for reading messages in chunks in
	 * the format: [ length ][ crc ][ message... ] and validating that length
	 * has acceptable value and that the crc32 checksum matches.
	 */
	public static class RawMessageFrameDecoder extends
		ReplayingDecoder<RawMessageDecoderState> {

		private static final int HEADER_LENGTH = 8;
		private static final int MAX_MESSAGE_SIZE = 128 * 1024 * 1024;

		private byte[] header = new byte[HEADER_LENGTH];
		private int length;
		private int crc;

		public RawMessageFrameDecoder() {
			super(RawMessageDecoderState.READ_HEADER);
		}

		/**
		 * decode uses ReplayingDecoder magic to incrementally read the whole
		 * chunk (frame)
		 */
		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buffer, RawMessageDecoderState state)
			throws Exception {

			logger.trace("RawMessageFrameDecoder.decode with state {}", state);

			switch (state) {
			case READ_HEADER:
				buffer.readBytes(this.header);
				ByteBuffer header = ByteBuffer.wrap(this.header);
				header.order(ByteOrder.LITTLE_ENDIAN);
				length = header.getInt();
				crc = header.getInt();
				logger.debug("next message length = {}", length);
				if (length < 0) {
					channel.close();
					throw new Exception("length must be positive, not "
						+ length);
				}
				if (length > MAX_MESSAGE_SIZE) {
					channel.close();
					throw new Exception("length must be less than "
						+ MAX_MESSAGE_SIZE + ", not " + length);
				}
				checkpoint(RawMessageDecoderState.READ_MESSAGE);
			case READ_MESSAGE:
				ChannelBuffer message = buffer.readBytes(length);
				assert message.readableBytes() == length;
				checkpoint(RawMessageDecoderState.READ_HEADER);
				checkCrc(message);
				return message;
			default:
				throw new Error("Shouldn't reach here.");
			}
		}

		/**
		 * validate crc32 checksum of the incoming message
		 * 
		 * @param message
		 *            the message read from the channel (not including headers)
		 * @throws Exception
		 *             throw if the checksum does not mach
		 */
		private void checkCrc(ChannelBuffer message) throws Exception {
			if (this.crc != (int) getCrc32(message))
				throw new Exception("Crc checksum does not match.");
			logger.debug("validated a checksum of message, length {}", message
				.readableBytes());
		}
	}

	/**
	 * RawMessageEncoder is responsible for creating frames in the format
	 * 
	 * <pre>
	 * [ length ][ crc ][ message... ]
	 * </pre>
	 * 
	 * (thus, as expected by RawMessageFrameDecoder)
	 */
	@ChannelPipelineCoverage("all")
	public static class RawMessageEncoder extends OneToOneEncoder {

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

	/**
	 * Calculate a crc32 checksum of the given buffer.
	 * 
	 * @param buffer
	 *            which checksum should be calculated
	 * @return calculated checksum
	 */
	static long getCrc32(ChannelBuffer buffer) {
		CRC32 crc = new CRC32();
		for (ByteBuffer bb : buffer.toByteBuffers())
			crc.update(bb.array());
		return crc.getValue();
	}
}
