package chord.messages;

/**
 * Represents a message sent by a node to its predecessor to verify whether it has failed or not.
 * 
 * @author zanel
 * @author coffee
 *
 */
public class CheckPredecessor extends Message {
	
	/**
	 * Instantiates a new CheckPredecessor message.
	 * 
	 * @param sender the Chord identifier of the message sender.
	 */
	public CheckPredecessor(Long sender){
		super(sender);
	}
	
}
