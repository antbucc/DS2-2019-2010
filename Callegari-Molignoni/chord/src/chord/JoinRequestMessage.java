package chord;

/**
 * Message used to join the Chord ring through an existing node.
 */
public class JoinRequestMessage extends Message {
	/**
	 * Constructor of the message.
	 * 
	 * @param sender the sender of the message.
	 */
	public JoinRequestMessage(int sender) {
		super(sender);
	}
}
