package multiplexer.jmx.internal;

import static multiplexer.jmx.internal.RawMessageFrame.getCrc32;

import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RawMessageFrameDecoder is responsible for reading messages in chunks in the
 * format: [ length ][ crc ][ message... ] and validating that length has
 * acceptable value and that the crc32 checksum matches.
 * 
 * You may want to prepend it with {@link ChannelBufferFactorySettingHandler}.
 * {@code LITTLE_ENDIAN_BUFFER_FACTORY_SETTER} to simplify header decoding.
 */
public class RawMessageFrameDecoder extends
	ReplayingDecoder<RawMessageDecoderState> {

	private static final Logger logger = LoggerFactory
		.getLogger(RawMessageFrameDecoder.class);

	private static final int MAX_MESSAGE_SIZE = 128 * 1024 * 1024;

	private int length;
	private int crc;

	public RawMessageFrameDecoder() {
		super(RawMessageDecoderState.READ_LENGTH);
	}

	/**
	 * Decode uses {@link ReplayingDecoder} magic to incrementally read the
	 * whole chunk (frame).
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
		ChannelBuffer buffer, RawMessageDecoderState state) throws Exception {

		logger.trace("RawMessageFrameDecoder.decode with state {}", state);

		switch (state) {
		case READ_LENGTH:
			length = buffer.readInt();
			if (buffer.order() == ByteOrder.BIG_ENDIAN) {
				length = Integer.reverseBytes(length);
			}
			checkpoint(RawMessageDecoderState.READ_CRC32);
		case READ_CRC32:
			crc = buffer.readInt();
			if (buffer.order() == ByteOrder.BIG_ENDIAN) {
				crc = Integer.reverseBytes(crc);
			}
			logger.trace("next message length = {}, crc = {}", length, crc);
			if (length < 0) {
				Channels.close(channel);
				throw new Exception("length must be positive, not " + length);
			}
			if (length > MAX_MESSAGE_SIZE) {
				Channels.close(channel);
				throw new Exception("length must be less than "
					+ MAX_MESSAGE_SIZE + ", not " + length);
			}
			checkpoint(RawMessageDecoderState.READ_MESSAGE);
		case READ_MESSAGE:
			ChannelBuffer message = buffer.readBytes(length);
			assert message.readableBytes() == length;
			checkpoint(RawMessageDecoderState.READ_LENGTH);
			if (!checkCrc(message)) {
				logger
					.warn(
						"message of length {} with invalid checksum received over {}",
						length, channel);
				Channels.close(channel);
			}
			return message;
		default:
			throw new Error("Shouldn't reach here.");
		}
	}

	@Override
	protected Object decodeLast(ChannelHandlerContext ctx, Channel channel,
		ChannelBuffer buffer, RawMessageDecoderState state) throws Exception {

		logger
			.trace(
				"RawMessageFrameDecoder.decodeLast with state {} buffer.readableBytes = {}",
				state, buffer.readableBytes());

		if (state == RawMessageDecoderState.READ_LENGTH
			&& buffer.readableBytes() == 0) {
			// OK, Channel closed at the frame boundary.
			return null;
		}

		logger.warn(
			"Channel closed when decoder state is {}, readableBytes = {}",
			state, buffer.readableBytes());
		return null;
	}

	/**
	 * validate crc32 checksum of the incoming message
	 * 
	 * @param message
	 *            the message read from the channel (not including headers)
	 * @throws Exception
	 *             throw if the checksum does not mach
	 */
	private boolean checkCrc(ChannelBuffer message) throws Exception {
		if (this.crc != (int) getCrc32(message))
			return false;
		return true;
	}
}

/**
 * RawMessageDecoderState is internally used by RawMessageFrameDecoder
 */
enum RawMessageDecoderState {
	READ_LENGTH, READ_CRC32, READ_MESSAGE,
}