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

package multiplexer.jmx.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;

import junit.framework.TestCase;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

/**
 * @author Piotr Findeisen
 */
public class TestByteBufferSerialization extends TestCase {

	@Test
	public void testByteBufferSerialization() {
		byte[] a = new byte[] { 'a', 'l', 'a' };
		CRC32 crc = new CRC32();
		crc.update(a);

		ByteBuffer b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(a.length);
		b.putInt((int) crc.getValue());

		assertTrue(Arrays.equals(b.array(), new byte[] { 3, 0, 0, 0, 96, 13,
			169 - 256, 69 }));
	}

	@Test
	public void testNettyBufferSimplest() {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(bb.order(), ByteOrder.LITTLE_ENDIAN);
		bb = bb.slice();
		assertEquals(bb.order(), ByteOrder.BIG_ENDIAN);

		ChannelBuffer b = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN,
			4);
		assertEquals("a bug in netty", b.order(), ByteOrder.LITTLE_ENDIAN);
		b.writeInt(1);
		assertEquals(b.readByte(), 1);
		assertEquals(b.readByte(), 0);
		assertEquals(b.readByte(), 0);
		assertEquals(b.readByte(), 0);
	}

	@Test
	public void testNettyBufferSerialization() {
		byte[] a = new byte[] { 'a', 'l', 'a' };
		CRC32 crc = new CRC32();
		crc.update(a);

		ChannelBuffer b = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, 8);
		b.writeInt(a.length);
		b.writeInt((int) crc.getValue());

		byte[] copy = new byte[8];
		b.getBytes(0, copy);
		assertTrue(Arrays.equals(copy, new byte[] { 3, 0, 0, 0, 96, 13,
			169 - 256, 69 }));
	}
}
