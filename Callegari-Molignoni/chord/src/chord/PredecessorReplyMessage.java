package chord;

/**
 * Message used to return the predecessor to another node.
 */
public class PredecessorReplyMessage extends Message {
	/**
	 * The predecessor of the current node.
	 */
	int predecessor;
	
	/**
	 * Constructor of the message.
	 * 
	 * @param sender the sender of the message.
	 */
	public PredecessorReplyMessage(int sender, int predecessor) {
		super(sender);
		
		this.predecessor = predecessor;
	}

}
