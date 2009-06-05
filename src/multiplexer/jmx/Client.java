package multiplexer.jmx;

import java.net.SocketAddress;

import multiplexer.Multiplexer.MultiplexerMessage;

import org.jboss.netty.channel.ChannelFuture;

import com.google.protobuf.ByteString;

/**
 * TODO
 * @author Piotr Findeisen
 *
 */
public class Client {
	protected final ConnectionsManager connectionsManager;

	/**
	 * TODO
	 * @param clientType
	 */
	public Client(int clientType) {
		connectionsManager = new ConnectionsManager(clientType);
	}

	/**
	 * TODO
	 * @param address
	 * @return
	 */
	public ChannelFuture asyncConnect(SocketAddress address) {
		return connectionsManager.asyncConnect(address);
	}

	/**
	 * TODO
	 * @param address
	 */
	public void connect(SocketAddress address) {
		asyncConnect(address).awaitUninterruptibly();
	}

	/**
	 * TODO
	 * @param message
	 * @param type
	 * @return
	 */
	public MultiplexerMessage createMessage(ByteString message, int type) {
		return connectionsManager.createMessage(message, type);
	}

	/**
	 * TODO
	 * @param message
	 * @return
	 */
	public MultiplexerMessage createMessage(MultiplexerMessage.Builder message) {
		return connectionsManager.createMessage(message);
	}

	/**
	 * TODO
	 * @param message
	 * @param sendingMethod
	 */
	public void send(MultiplexerMessage message, SendingMethod sendingMethod) {
		throw new RuntimeException("not implemented");
	}

	/**
	 * TODO
	 * @return
	 */
	public ReceiveResult receive() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * TODO
	 * @param message
	 */
	public void event(MultiplexerMessage message) {
		send(message, SendingMethod.THROUGH_ALL);
	}
	
	/**
	 * TODO
	 * @param message
	 * @param type
	 */
	public void query(ByteString message, int type) {
		throw new RuntimeException("not implemented");
	}

	/**
	 * TODO
	 * @author Piotr Findeisen
	 *
	 */
	public class ReceiveResult {

	}
}
