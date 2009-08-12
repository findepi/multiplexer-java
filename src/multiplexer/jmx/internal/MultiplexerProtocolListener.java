package multiplexer.jmx.internal;

import multiplexer.protocol.Protocol.MultiplexerMessage;

import org.jboss.netty.channel.Channel;

/**
 * Listens to the messages and events returned by
 * {@link MultiplexerProtocolHandler}.
 * 
 * @author Piotr Findeisen
 */
public interface MultiplexerProtocolListener {

	/**
	 * Invoked when the {@link MultiplexerProtocolHandler} associated with this
	 * listener receives a message from the network.
	 * 
	 * @param message
	 *            message read from the network
	 * @param channel
	 *            from which the message was read
	 */
	public void messageReceived(MultiplexerMessage message, Channel channel);

	/**
	 * Invoked when the {@link MultiplexerProtocolHandler} receives the
	 * information about a {@link Channel} being disconnected.
	 * 
	 * @param channel
	 *            the closed channel
	 */
	public void channelDisconnected(Channel channel);

	/**
	 * Invoked when the {@link MultiplexerProtocolHandler} receives the
	 * information about a {@link Channel} being open.
	 * 
	 * @param channel
	 *            the opened channel
	 */
	public void channelOpen(Channel channel);
}
