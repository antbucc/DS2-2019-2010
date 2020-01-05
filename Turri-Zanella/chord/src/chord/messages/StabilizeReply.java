package chord.messages;

import java.util.TreeSet;
import java.util.UUID;

/**
 * Represents a message sent by a node to inform its predecessor about 
 * its predecessor and successor list.
 * 
 * @author zanel
 * @author coffee
 * 
 */
public class StabilizeReply extends Message {
	
	/**
	 * The Chord identifier of the node's predecessor.
	 */
	private Long predecessor;
	
	/**
	 * The successor list of the node.
	 */
	private TreeSet<Long> successors;

	private UUID requestId;
	/**
	 * Instantiates a new StabilizeReply message.
	 * 
	 * @param sender the Chord identifier of the sender node.
	 * @param predecessor the identifier of the node's predecessor.
	 * @param successors the successor list of the node.
	 */
	public StabilizeReply(Long sender, Long predecessor, TreeSet<Long> successors, UUID requestId) {
		super(sender);
		this.requestId = requestId;
		this.predecessor = predecessor;
		this.successors = successors;
	}
	
	/**
	 * @return the predecessor
	 */
	public Long getPredecessor() {
		return predecessor;
	}

	/**
	 * @return the successors
	 */
	public TreeSet<Long> getSuccessors() {
		return successors;
	}

	/**
	 * @return the requestId
	 */
	public UUID getRequestId() {
		return requestId;
	}
	
	
}
