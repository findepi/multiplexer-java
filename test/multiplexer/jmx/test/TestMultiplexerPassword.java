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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import multiplexer.jmx.client.ConnectException;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.test.TestConstants.PeerTypes;
import multiplexer.jmx.test.util.JmxServerRunner;

import org.junit.Test;

import com.google.protobuf.ByteString;

/**
 * @author Piotr Findeisen
 */
public class TestMultiplexerPassword extends TestCase {

	@Test
	public void testNoPasswords() throws Exception {
		// should work
		testMultiplexerPassword("", "");
	}

	@Test
	public void testBothHasPassword() throws Exception {
		final String password = "some password";
		testMultiplexerPassword(password, password);
	}

	@Test
	public void testOnlyServerHasPassword() throws Exception {
		// server should forbid
		testMultiplexerPassword("some password", "");
	}

	@Test
	public void testOnlyClientHasPassword() throws Exception {
		// client should disconnect
		testMultiplexerPassword("", "some password");
	}

	private void testMultiplexerPassword(String serverPassword,
		String clientPassword) throws Exception {
		testMultiplexerPassword(serverPassword == null ? null : ByteString
			.copyFromUtf8(serverPassword), clientPassword == null ? null
			: ByteString.copyFromUtf8(clientPassword));
	}

	private void testMultiplexerPassword(ByteString serverPassword,
		ByteString clientPassword) throws Exception {

		JmxServerRunner runner = new JmxServerRunner();
		try {
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("multiplexerPassword", serverPassword);
			runner.start(options);

			JmxClient client = new JmxClient(PeerTypes.TEST_CLIENT);
			try {
				client.setMultiplexerPassword(clientPassword);
				assertEquals(clientPassword, client.getMultiplexerPassword());

				try {
					client.connect(runner.getLocalServerAddress());
					assertEquals(serverPassword, clientPassword);
				} catch (ConnectException e) {
					assertTrue(!serverPassword.equals(clientPassword));
				}
				assertEquals(clientPassword, client.getMultiplexerPassword());

			} finally {
				client.shutdown();
			}

		} finally {
			runner.stop(false);
		}
	}
}
