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
