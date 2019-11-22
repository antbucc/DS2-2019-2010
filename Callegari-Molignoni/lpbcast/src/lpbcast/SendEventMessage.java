package lpbcast;

/**
 * Message used as reply of a "AskEventMessage".
 * 
 * This message is sent back to a node which required a specific event.
 */
public class SendEventMessage extends Message {
	Event event;
	

	/**
	 * Constructor of SendEventMessage.
	 * 
	 * @param event  event which was requested
	 * @param sender sender of the message
	 */
	public SendEventMessage(Event event, int sender) {
		this.event = event;
		this.sender = sender;
	}
}
