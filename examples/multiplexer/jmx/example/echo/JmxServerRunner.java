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

package multiplexer.jmx.example.echo;

import java.net.InetSocketAddress;

import multiplexer.jmx.server.JmxServer;
import multiplexer.protocol.Protocol.MultiplexerMessageDescription;
import multiplexer.protocol.Protocol.MultiplexerPeerDescription;
import multiplexer.protocol.Protocol.MultiplexerRules;
import multiplexer.protocol.Protocol.MultiplexerMessageDescription.RoutingRule;

/**
 * @author Piotr Findeisen
 */
public class JmxServerRunner {

	public static final String MULTIPLEXER_HOST = "127.0.0.1";
	public static final int MULTIPLEXER_PORT = 31889;

	/*
	 * Normally those settings go to a .rules file and appropriate constant
	 * definitions are generated from it.
	 */
	public static final int ECHO_SERVER = 1001;
	public static final int ECHO_CLIENT = 1002;
	public static final int ECHO_REQUEST = 1201;
	public static final int ECHO_RESPONSE = 1202;

	/* Normally read from a .rules file. */
	public static final MultiplexerRules MULTIPLEXER_RULES = MultiplexerRules
		.newBuilder()
		/* define ECHO_SERVER peer type (required by ECHO_REQUEST message type) */
		.addPeer(
			MultiplexerPeerDescription.newBuilder().setName("ECHO_SERVER")
				.setType(ECHO_SERVER))
		/* define ECHO_CLIENT peer type (for completeness, not really required) */
		.addPeer(
			MultiplexerPeerDescription.newBuilder().setName("ECHO_CLIENT")
				.setType(ECHO_CLIENT))
		/*
		 * define a message type ECHO_REQUEST that should be routed to any from
		 * connected ECHO_SERVERs. Notice that we don't need to define
		 * ECHO_RESPONSE here.
		 */
		.addType(
			MultiplexerMessageDescription.newBuilder().setName("ECHO_REQUEST")
				.setType(ECHO_REQUEST).addTo(
					RoutingRule.newBuilder().setPeerType(ECHO_SERVER).setWhom(
						RoutingRule.Whom.ANY)))
		/* Build the rules object with the above definitions. */
		.build();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JmxServer jmxServer = new JmxServer(jmxServerAddress());
		jmxServer.loadMessageDefinitions(MULTIPLEXER_RULES);
		jmxServer.run();
	}

	public static InetSocketAddress jmxServerAddress() {
		return new InetSocketAddress(MULTIPLEXER_HOST, MULTIPLEXER_PORT);
	}
}
