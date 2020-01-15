package chord;

/**
 * Message used notify another node of its possible predecessor.
 */
public class NotificationMessage extends Message {
	/**
	 * Constructor of the message.
	 * 
	 * @param sender the sender of the message.
	 */
	public NotificationMessage(int sender) {
		super(sender);
	}
}
