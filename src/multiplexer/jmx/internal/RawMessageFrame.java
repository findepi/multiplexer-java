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

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author Piotr Findeisen
 */
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
