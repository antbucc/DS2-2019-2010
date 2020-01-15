package chord;

/**
 * Message used to ask another node for its predecessor.
 */
public class PredecessorRequestMessage extends Message {
	/**
	 * Constructor of the message.
	 * 
	 * @param sender the sender of the message.
	 */
	public PredecessorRequestMessage(int sender) {
		super(sender);
	}
}
