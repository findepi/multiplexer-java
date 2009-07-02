package multiplexer.jmx.tools.rulesconsts;

import java.util.List;

/**
 * @author Piotr Findeisen
 */
public class ConstantsGroup {

	private List<Constant<Integer>> peerTypes;
	private List<Constant<Integer>> messageTypes;

	public ConstantsGroup(List<Constant<Integer>> peerTypes,
		List<Constant<Integer>> messageTypes) {
		super();
		this.peerTypes = peerTypes;
		this.messageTypes = messageTypes;
	}

	public List<Constant<Integer>> getPeerTypes() {
		return peerTypes;
	}

	public List<Constant<Integer>> getMessageTypes() {
		return messageTypes;
	}
}
