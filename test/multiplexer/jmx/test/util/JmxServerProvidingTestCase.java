package multiplexer.jmx.test.util;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

/**
 * @author Piotr Findeisen
 */
public class JmxServerProvidingTestCase extends TestCase {

	private JmxServerRunner jmxServerRunner;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		jmxServerRunner = new JmxServerRunner();
		jmxServerRunner.start();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		jmxServerRunner.stop();
		jmxServerRunner = null;
	}

	protected int getLocalServerPort() {
		return jmxServerRunner.getLocalServerPort();
	}

	protected InetSocketAddress getLocalServerAddress()
		throws UnknownHostException {
		return jmxServerRunner.getLocalServerAddress();
	}
}
