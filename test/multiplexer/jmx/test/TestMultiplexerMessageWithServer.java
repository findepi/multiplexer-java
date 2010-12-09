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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Random;
import java.util.zip.CRC32;

import multiplexer.jmx.test.util.JmxServerProvidingTestCase;
import multiplexer.protocol.Constants;
import multiplexer.protocol.Protocol.MultiplexerMessage;
import multiplexer.protocol.Protocol.WelcomeMessage;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;

/**
 * @author Piotr Findeisen
 */
public class TestMultiplexerMessageWithServer extends
	JmxServerProvidingTestCase {

	private static final Logger logger = LoggerFactory
		.getLogger(TestMultiplexerMessageWithServer.class);

	@Test
	public void testSimpleConnection() throws Exception {

		final int PYTHON_TEST_SERVER = TestConstants.PeerTypes.TEST_SERVER;
		final int CONNECTION_WELCOME = Constants.MessageTypes.CONNECTION_WELCOME;
		final int MULTIPLEXER = Constants.PeerTypes.MULTIPLEXER;
		final int PYTHON_TEST_REQUEST = TestConstants.MessageTypes.TEST_REQUEST;

		SimpleConnection c = new SimpleConnection(getLocalServerAddress());

		// send out invitation
		logger.info("sending welcome message");
		ByteString message = WelcomeMessage.newBuilder().setType(
			PYTHON_TEST_SERVER).setId(c.getInstanceId()).build().toByteString();
		c.send_message(message, CONNECTION_WELCOME);

		// receive the invitation
		logger.info("waiting for welcome message");
		MultiplexerMessage mxmsg = c.receive_message();
		logger.info("validating welcome message");
		assert mxmsg.getType() == CONNECTION_WELCOME;
		WelcomeMessage peer = WelcomeMessage.parseFrom(mxmsg.getMessage());
		assert peer.getType() == MULTIPLEXER;
		peer.getId();

		// send a stupid search_query
		ArrayList<Byte> sq = new ArrayList<Byte>();
		for (byte d : "this is a search query with null (\\x00) bytes and other "
			.getBytes())
			sq.add(d);
		for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++)
			sq.add((byte) i);

		Output sqo = ByteString.newOutput();
		for (byte d : sq)
			sqo.write(d);

		logger.info("sending sample search query");
		long id = c.send_message(sqo.toByteString(), PYTHON_TEST_REQUEST);
		logger.info("waiting for sample search query");
		mxmsg = c.receive_message();
		logger.info("validating sample search query");
		assert mxmsg.getId() == id;
		assert mxmsg.getType() == PYTHON_TEST_REQUEST;
		assert mxmsg.getMessage().equals(sqo.toByteString());

		// # send a large search_query
		// query = open("/dev/urandom", "r").read(1024 * 1024)
		// print "sending large search query"
		// id = c.send_message(query, types.SEARCH_QUERY)
		// print "waiting for large search query"
		// msg = c.receive_message()
		// print "validating large search query"
		// assert msg.id == id
		// assert msg.type == types.SEARCH_QUERY
		// assert msg.message == query
	}

	static class SimpleConnection {
		private final Socket socket;
		private final long instance_id;
		private final InputStream input_stream;
		private final OutputStream output_stream;
		private static final int header_length = 8;

		public long getInstanceId() {
			return instance_id;
		}

		public SimpleConnection(SocketAddress address) throws IOException {
			instance_id = new Random().nextLong();
			socket = new Socket();
			socket.connect(address);
			output_stream = socket.getOutputStream();
			input_stream = socket.getInputStream();
		}

		public long send_message(ByteString message, int type)
			throws IOException {
			MultiplexerMessage mxmsg = MultiplexerMessage.newBuilder().setId(
				new Random().nextLong()).setFrom(instance_id).setType(type)
				.setMessage(message).build();

			byte[] body = mxmsg.toByteArray();
			logger.info("sending " + body.length + " bytes ("
				+ message.size() + " meaningful)");
			ByteBuffer buffer = ByteBuffer
				.allocate(header_length + body.length);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			CRC32 body_sum = new CRC32();
			body_sum.update(body);
			buffer.putInt(body.length).putInt((int) body_sum.getValue()).put(
				body);
			output_stream.write(buffer.array());
			return mxmsg.getId();
		}

		private byte[] read_all(int len) throws IOException {
			byte[] buffer = new byte[len];
			int off = 0;
			while (off < len) {
				off += input_stream.read(buffer, off, len - off);
			}
			return buffer;
		}

		public MultiplexerMessage receive_message() throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(read_all(header_length));
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int length = buffer.getInt();
			int crc32 = buffer.getInt();
			byte[] body = read_all(length);
			CRC32 body_sum = new CRC32();
			body_sum.update(body);
			assert (int) body_sum.getValue() == crc32;
			MultiplexerMessage mxmsg = MultiplexerMessage.parseFrom(body);
			logger.info("received " + body.length + " bytes ("
				+ mxmsg.getMessage().size() + " meaningful)");
			return mxmsg;
		}
	}
}
