/**
 * 
 */
package lpbcast;

/**
 * Represents a retrieve request message.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 */
public class RetrieveRequest extends Message {

	/**
	 * The identifier of the event needed by the process which sends this message.
	 */
	public EventId eventId;
	
	/**
	 * Instantiates a new retrieve request.
	 * 
	 * @param sender the identifier of the process which sends this message
	 * @param eventId the identifier of the event needed the process which sends
	 * this message
	 */
	public RetrieveRequest(int sender, EventId eventId) {
		super(Message.MessageType.RETRIEVE_REQUEST, sender);
		this.eventId = eventId;
	}
}
