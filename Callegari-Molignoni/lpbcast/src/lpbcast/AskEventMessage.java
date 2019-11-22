package lpbcast;

/**
 * Message sent by a node which wants to ask for a specific event.
 */
public class AskEventMessage extends Message {
	String id;

	
	/**
	 * Constructor of AskEventMessage.
	 * 
	 * @param id     the ID of the message to retrieve.
	 * @param sender the sender of the message.
	 */
	public AskEventMessage(String id, int sender) {
		this.id = id;
		this.sender = sender;
	}

}
