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
