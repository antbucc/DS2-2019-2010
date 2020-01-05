package chord.messages;

/**
 * Represents a message sent by a node to notify its old predecessor that
 * its predecessor has changed.
 * 
 * @author zanel
 * @author coffee
 * 
 */
public class NotifyPredecessor extends Message {
	
	/**
	 * The Chord identifier of the node's new predecessor.
	 */
	private Long newPredecessor;
	
	/**
	 * Instantiates a new NotifyPredecessor message.
	 * 
	 * @param sender the Chord identifier of the sender node.
	 * @param newPredecessor the Chord identifier of the node's new predecessor.
	 */
	public NotifyPredecessor(Long sender, Long newPredecessor) {
		super(sender);
		this.newPredecessor = newPredecessor;
	}
	
	/**
	 * @return the newPredecessor
	 */
	public Long getNewPredecessor() {
		return newPredecessor;
	}
	
}
