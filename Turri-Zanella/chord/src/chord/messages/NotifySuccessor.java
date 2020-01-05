package chord.messages;

/**
 * Represents a message sent by a node to notify its successor of its
 * existence.
 * 
 * @author zanel
 * @author coffee
 * 
 */
public class NotifySuccessor extends Message {
	
	/**
	 * Instantiate a new NotifySuccessor message.
	 * 
	 * @param sender the Chord identifier of the message sender.
	 */
	public NotifySuccessor(Long sender) {
		super(sender);
	}
	
}
