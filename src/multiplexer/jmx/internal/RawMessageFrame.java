package multiplexer.jmx.internal;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import org.jboss.netty.buffer.ChannelBuffer;

public final class RawMessageFrame {

	private RawMessageFrame() {
	}

	public static final int HEADER_LENGTH = 8;
	public static final int MAX_MESSAGE_SIZE = 128 * 1024 * 1024;

	/**
	 * Calculate a crc32 checksum of the given buffer.
	 * 
	 * @param buffer
	 *            which checksum should be calculated
	 * @return calculated checksum
	 */
	public static long getCrc32(ChannelBuffer buffer) {
		CRC32 crc = new CRC32();
		for (ByteBuffer bb : buffer.toByteBuffers())
			crc.update(bb.array());
		return crc.getValue();
	}
}
