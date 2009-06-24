/**
 * 
 */
package multiplexer.jmx.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;
import multiplexer.constants.Peers;
import multiplexer.jmx.JmxClient;

/**
 * @author Kasia Findeisen
 *
 */
public class TestConnectivity extends TestCase {
	public void testConnect() throws UnknownHostException {
		JmxClient client = new JmxClient(Peers.PYTHON_TEST_CLIENT);
		client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1980));
	}

}
