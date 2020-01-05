package chord.messages;

/**
 * Represents a message sent by a node to ask its successor for the
 * successor's predecessor and successor list.
 * 
 * @author zanel
 * @author coffee
 * 
 */
public class StabilizeRequest extends Message {

	/**
	 * The Chord identifier of the message recipient.
	 */
	private Long destinationId;
	
	/**
	 * Instantiates a new StabilizeRequest message.
	 * 
	 * @param sender the Chord identifier of the sender node.
	 */
	public StabilizeRequest(Long sender, Long destinationId) {
		super(sender);
		this.destinationId = destinationId;
	}
	
	/**
	 * @return the destinationId
	 */
	public Long getDestinationId() {
		return destinationId;
	}
}
