package chord.messages;

import java.util.UUID;

/**
 * Represents a message sent by an intermediate node to the initiator of the lookup, 
 * carrying either an intermediate node (in the iterative version of the protocol only)
 * or the successor of the identifier the initiator is looking for.
 * 
 * @author zanel
 * @author coffee
 * 
 */
public class FindSuccessorReply extends Message {
	
	public enum NodeType {INTERMEDIARY, SUCCESSOR}
	
	/**
	 * The type of the node carried by this message:
	 * - INTERMEDIARY if the node is an intermediate node.
	 * - SUCCESSOR if the node is the successor of the identifier the initiator is looking for.
	 */
	private NodeType nodeType;
	
	/**
	 * The identifier of the FindSuccessorRequest message referred to by this message.
	 */
	private UUID requestId;

	/**
	 * The Chord identifier of the node carried by this message.
	 */
	Long nodeId;
	
	/**
	 * Instantiates a new FindSuccessorReply message.
	 * 
	 * @param sender the Chord identifier of the message sender.
	 * @param requestId the identifier of the FindSuccessorRequest message referred to by this message. 
	 * @param nodeId the Chord identifier of the node carried by this message.
	 * @param nodeType the type of the node carried by this message.
	 */
	public FindSuccessorReply(Long sender, UUID requestId, Long nodeId, NodeType nodeType) {
		super(sender);
		this.nodeType = nodeType;
		this.requestId = requestId;
		this.nodeId = nodeId;
		
	}
	/**
	 * @return the nodeType
	 */
	public NodeType getNodeType() {
		return nodeType;
	}

	/**
	 * @return the requestId
	 */
	public UUID getRequestId() {
		return requestId;
	}

	/**
	 * @return nodeId
	 */
	public Long getNodeId() {
		return nodeId;
	}
}
