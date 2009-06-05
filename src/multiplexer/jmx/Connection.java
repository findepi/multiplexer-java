package multiplexer.jmx;

import org.jboss.netty.channel.socket.SocketChannel;

/**
 * Wrapper around {@link SocketChannel}.
 * @author Piotr Findeisen
 */
class Connection {

	private final SocketChannel channel;
	
	Connection(SocketChannel channel) {
		this.channel = channel; 
	}
	
	SocketChannel getChannel() {
		return channel;
	}
}
