/**
 * 
 */
package lpbcast;

/**
 * Represents a missing event notification.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 */
public class MissingEvent {
	
	/**
	 * The identifier of the missing event notification.
	 */
	public EventId eventId;
	/**
	 * The tick during which the process discovered the missing event notification. 
	 */
	public double tick;
	/**
	 * The identifier of the process which sent the gossip in which is contained 
	 * the missing event notification.
	 */
	public int sender;
	
	/**
	 * Instantiates a new missing event notification.
	 * 
	 * @param eventId the identifier of the missing event notification
	 * @param tick the tick during which the process discovered it lost the event
	 * @param sender the identifier of the process which sent the gossip in which is
	 * contained the missing event
	 */
	public MissingEvent(EventId eventId, double tick, int sender) {
		this.eventId = eventId;
		this.tick = tick;
		this.sender = sender;
	}
}
