package chord.messages;

import java.util.TreeSet;

/**
 * Represents a message sent by a node to notify its successor and predecessor
 * about its voluntarily leaving of the network.
 * 
 * @author zanel
 * @author coffee
 * 
 */
public class VoluntaryLeave extends Message {

	/**
	 * The Chord identifier of the node's predecessor.
	 */
	private Long predecessor;
	
	/**
	 * The successor list of the node.
	 */
	private TreeSet<Long> successors;
	
	/**
	 * Instantiates a new VoluntaryLeave message.
	 * 
	 * @param sender the Chord identifier of the message sender.
	 * @param predecessor the Chord identifier of the node's predecessor.
	 * @param successors the successor list of the node.
	 */
	public VoluntaryLeave(Long sender, Long predecessor, TreeSet<Long> successors) {
		super(sender);
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
	
}
