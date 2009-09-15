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

package multiplexer.jmx.backend;

import multiplexer.jmx.client.Connection;
import multiplexer.jmx.client.JmxClient;
import multiplexer.jmx.client.SendingMethod;
import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * Default full implementation of {@link MessageContext}.
 * 
 * @author Piotr Findeisen
 */
public class DefaultMessageContext extends AbstractMessageContext {

	private final JmxClient client;
	private final Connection conn;

	public DefaultMessageContext(MultiplexerMessage message, JmxClient client,
		Connection conn) {
		super(message);
		if (client == null)
			throw new NullPointerException("client");
		if (conn == null)
			throw new NullPointerException("conn");
		this.client = client;
		this.conn = conn;
	}

	public JmxClient getJmxClient() {
		return client;
	}

	public void reply(MultiplexerMessage message) {
		assert message.hasType() || message.hasTo();
		assert message.hasId();
		getJmxClient().send(message, SendingMethod.via(conn));
		setResponseSent(true);
	}

	public void reply(MultiplexerMessage.Builder message) {
		reply(message.build());
	}
}
