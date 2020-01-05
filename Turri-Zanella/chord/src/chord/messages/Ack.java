package chord.messages;

import java.util.UUID;

/**
 * Represents a signal sent by the destination node to the sending node after the receipt of a message.
 * 
 * @author zanel
 * @author coffee
 *
 */
public class Ack extends Message {
	
	public enum AckType{FINDSUCCESSOR, CHECKPREDECESSOR};
	
	/**
	 *  The identifier of the message referred to by the ack.
	 */
	private UUID requestId;
	
	/**
	 * The type of the message referred to by the ack.
	 */
	private AckType ackType;
	
	/**
	 * Instantiates a new Ack message.
	 * 
	 * @param sender the Chord identifier of the message sender.
	 * @param tick the tick during which the message has to be processed.
	 * @param requestId the identifier of the message referred to by the ack.
	 * @param ackType the type of the message referred to by the ack.
	 */
	public Ack(Long sender, UUID requestId, AckType ackType) {
		super(sender);
		this.requestId = requestId;
		this.ackType = ackType;
	}

	/**
	 * @return the requestId
	 */
	public UUID getRequestId() {
		return requestId;
	}

	/**
	 * @return the ackType
	 */
	public AckType getAckType() {
		return ackType;
	}
	
}
