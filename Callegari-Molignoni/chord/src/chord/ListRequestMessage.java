package chord;

/**
 * Message used to ask another node for its successor list.
 */
public class ListRequestMessage extends Message {
	/**
	 * Constructor of the message.
	 * 
	 * @param sender the sender of the message.
	 */
	public ListRequestMessage(int sender) {
		super(sender);		
	}
}
