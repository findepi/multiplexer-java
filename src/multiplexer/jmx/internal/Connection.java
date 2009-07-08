package multiplexer.jmx.internal;

import org.jboss.netty.channel.Channel;

/**
 * Wrapper around {@link Channel}.
 * @author Piotr Findeisen
 */
public class Connection {

	private final Channel channel;
	
	Connection(Channel channel) {
		this.channel = channel; 
	}
	
	Channel getChannel() {
		return channel;
	}
}
