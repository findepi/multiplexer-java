package multiplexer.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;

import junit.framework.TestCase;

public class TestByteBufferSerialization extends TestCase {

	public void testByteBufferSerialization() {
		byte[] a = new byte[] { 'a', 'l', 'a' };
		ByteBuffer b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(a.length);
		CRC32 crc = new CRC32();
		crc.update(a);
		b.putInt((int) crc.getValue());

		assertTrue(Arrays.equals(b.array(),
			new byte[] { 3, 0, 0, 0, 96, 13, 169 - 256, 69 }));
	}
}
