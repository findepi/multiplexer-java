package multiplexer.jmx.client;

import org.jboss.netty.channel.Channel;

/**
 * Wrapper around {@link Channel}.
 * @author Piotr Findeisen
 */
public class Connection {

	private final Channel channel;
	
	public Connection(Channel channel) {
		this.channel = channel; 
	}
	
	/**
	 * Internal.
	 */
	public Channel getChannel() {
		return channel;
	}
}
