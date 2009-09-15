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
