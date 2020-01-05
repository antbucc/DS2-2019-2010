/**
 * 
 */
package chord.messages;

import java.util.UUID;

/**
 * Represents a lookup message sent by either the initiator of the lookup
 * or an intermediate node (in the recursive version of the protocol only).
 * 
 * @author zanel
 * @author coffee
 *
 */
public class FindSuccessorRequest extends Message {

	public enum ReqType{JOIN, FIXFINGER, LOOKUP};
	
	/**
	 * The Chord identifier of the node which initiated the lookup.
	 */
	private Long initiatorId;
	
	/**
	 * The Chord identifier of the node/key which the node is looking for.
	 */
	private Long targetId;
	
	/**
	 * The identifier of the FindSuccessorRequest message initiated
	 * by the initiator node.
	 */
	private UUID lookupId;
	
	/**
	 * The type of the request.
	 */
	private ReqType reqType;
	
	/**
	 * The index of the finger table entry which has to be refreshed if reqType is equal to FIXFINGER, null otherwise.
	 */
	private Integer fingerToFix;
	
	/**
	 * The Chord identifier of the message recipient.
	 */
	private Long destinationId;

	/**
	 * Instantiates a new FindSuccessorRequest message.
	 * 
	 * @param sender the Chord identifier of the message sender.
	 * @param initiatorId the Chord identifier of the node which initiated the lookup.
	 * @param targetId the Chord identifier of the node/key that the node is looking for.
	 * @param lookupId the unique identifier of the FindSuccessorRequest message initiated
	 * by the initiator node.
	 * @param reqType the type of the request.
	 * @param fingerToFix the index of the finger table entry which has to be refreshed if reqType
	 * is equal to FIXFINGER, null otherwise
	 * @param destinationId the Chord identifier of the message recipient.
	 */
	public FindSuccessorRequest(Long sender, Long initiatorId, Long targetId, UUID lookupId, ReqType reqType, Integer fingerToFix, Long destinationId) {
		super(sender);
		this.initiatorId = initiatorId;
		this.targetId = targetId;
		this.lookupId = lookupId;
		this.reqType = reqType;
		this.fingerToFix = fingerToFix;
		this.destinationId = destinationId;
	}
	
	/**
	 * @return the initiatorId
	 */
	public Long getInitiatorId() {
		return initiatorId;
	}

	/**
	 * @return the targetId
	 */
	public Long getTargetId() {
		return targetId;
	}

	/**
	 * @return the lookupId
	 */
	public UUID getLookupId() {
		return lookupId;
	}

	/**
	 * @return the reqType
	 */
	public ReqType getReqType() {
		return reqType;
	}
	
	/**
	 * @return the fingerToFix
	 */
	public Integer getFingerToFix() {
		return fingerToFix;
	}
	
	/**
	 * @return the destinationId
	 */
	public Long getDestinationId() {
		return destinationId;
	}
	
	/**
	 * @param lookupId the lookupId to set
	 */
	public void setLookupId(UUID lookupId) {
		this.lookupId = lookupId;
	}
	
}
