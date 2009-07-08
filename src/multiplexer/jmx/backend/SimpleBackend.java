package multiplexer.jmx.backend;


/**
 * @author Piotr Findeisen
 * 
 */
public class SimpleBackend extends AbstractHandlingBackend {

	SimpleBackend(int peerType) {
		super(peerType);
	}

	public SimpleBackend(int peerType, MessageHandler messageHandler) {
		this(peerType);
		setMessageHandler(messageHandler);
	}
	
	@Override
	public MessageHandler getMessageHandler() {
		return super.getMessageHandler();
	}

	@Override
	public void setMessageHandler(MessageHandler messageHandler) {
		super.setMessageHandler(messageHandler);
	}
}
