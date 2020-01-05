package chord.messages;
/**
 * Represents a join-reply message sent by the node who previously has received a joinRequest message
 * to the related joining node, the message contains the successor of the joining node
 * 
 * @author coffee
 *
 */
public class JoinReply extends Message{
	
	/**
	 * the successor of the joining node
	 */
	private Long successor;
	
	/**
	 * Instantiates a new JoinReply message
	 * 
	 * @param sender the Chord identifier of the message sender.
	 * @param successor the Chord identifier of the successor of the joining node
	 */
	public JoinReply(Long sender, Long successor) {
		super(sender);
		this.successor = successor;
	}
	
	/**
	 * @return the successor
	 */
	public Long getSuccessor() {
		return successor;
	}
}
