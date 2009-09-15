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

import static multiplexer.jmx.example.echo.JmxServerRunner.ECHO_RESPONSE;
import static multiplexer.jmx.example.echo.JmxServerRunner.ECHO_SERVER;
import static multiplexer.jmx.example.echo.JmxServerRunner.jmxServerAddress;
import multiplexer.jmx.backend.MessageContext;
import multiplexer.jmx.backend.MessageHandler;
import multiplexer.jmx.backend.SimpleBackend;
import multiplexer.jmx.server.JmxServer;
import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * Almost simplest possible Multiplexer backend that simply resends all messages
 * sent to it. To run the backend execute
 * 
 * <pre>
 * // Instantiate a backend; EchoRequestHandler will handle incoming messages. 
 * SimpleBackend backend = new SimpleBackend(ECHO_SERVER,
 * 	new EchoBackend.EchoRequestHandler());
 * 
 * // Connect to all the multiplexer daemons.
 * backend.asyncConnect(...);
 * backend.asyncConnect(...);
 * backend.asyncConnect(...);
 * 
 * // Run it.
 * backend.run();
 * </pre>
 * 
 * @author Piotr Findeisen
 */
public class EchoBackend {

	/**
	 * Handler to be used with {@link SimpleBackend} or similar.
	 * 
	 * @author Piotr Findeisen
	 */
	public static class EchoRequestHandler implements MessageHandler {

		public void handleMessage(MultiplexerMessage message, MessageContext ctx) {
			/*
			 * Send the same content directly to caller and with message type
			 * ECHO_RESPONSE.
			 */
			ctx.reply(ctx.createResponse(ECHO_RESPONSE, ctx.getMessage()
				.getMessage()));
		}
	}

	/**
	 * Run the Echo server connected to {@link JmxServer} run by
	 * {@link JmxServerRunner}.
	 */
	public static void main(String[] args) {
		SimpleBackend backend = new SimpleBackend(ECHO_SERVER,
			new EchoRequestHandler());
		backend.asyncConnect(jmxServerAddress());
		backend.run();
	}
}
