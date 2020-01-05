package chord.messages;

/**
 * Represents a request to find the successor sent by a joining node to its known node
 * 
 * @author coffee
 * @author zanel
 */
public class JoinRequest extends Message{
	
	/*
	 * The unique nodeId which the joining node know
	 */
	public Long knownNode;
	
	/**
	 * Instantiates a new JoinRequest
	 * 
	 * @param sender the Chord identifier of the message sender.
	 * @param knownNode the Chord identifier of the node which the joining node know
	 */
	public JoinRequest(Long sender) {
		super(sender);
	}
}
