package multiplexer.jmx.test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import multiplexer.jmx.client.ConnectException;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.test.TestConstants.PeerTypes;
import multiplexer.jmx.test.util.JmxServerRunner;

import com.google.protobuf.ByteString;

/**
 * @author Piotr Findeisen
 */
public class TestMultiplexerPassword extends TestCase {

	public void testNoPasswords() throws Exception {
		// should work
		testMultiplexerPassword("", "");
	}

	public void testBothHasPassword() throws Exception {
		final String password = "some password";
		testMultiplexerPassword(password, password);
	}

	public void testOnlyServerHasPassword() throws Exception {
		// server should forbid
		testMultiplexerPassword("some password", "");
	}

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
