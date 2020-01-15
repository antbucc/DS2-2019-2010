package chord;

/**
 * Message used to obtain the successor through an existing node.
 */
public class JoinReplyMessage extends Message {
	/**
	 * The successor of the requester.
	 */
	int successor;
	
	/**
	 * Constructor of the message.
	 * 
	 * @param sender the sender of the message.
	 */
	public JoinReplyMessage(int sender, int successor) {
		super(sender);
		
		this.successor = successor;
	}
}
