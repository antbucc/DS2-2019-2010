/**
 * 
 */
package lpbcast;

/**
 * Represents a reply to a retrieve request message.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 */
public class RetrieveReply extends Message {

	/**
	 * The event which is needed by some process.
	 */
	public Event event;
	
	/**
	 * Instantiates a new retrieve reply.
	 * 
	 * @param sender the identifier of the process which sends this message
	 * @param event the event requested by some process
	 */
	public RetrieveReply(int sender, Event event) {
		super(Message.MessageType.RETRIEVE_REPLY, sender);
		this.event = event;
	}
}
