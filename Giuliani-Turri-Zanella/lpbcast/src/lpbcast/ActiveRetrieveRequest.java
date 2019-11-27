/**
 * 
 */
package lpbcast;

/**
 * Represents a request which has been issued by a node to retrieve a missing event.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 * 
 */
public class ActiveRetrieveRequest {

	/**
	 * Types of processes that can receive a request to retrieve a missing event
	 * notification.
	 */
	public enum Destination {SENDER, RANDOM, ORIGINATOR};
	
	/**
	 * The identifier of the missing event notification.
	 */
	public EventId eventId;
	/**
	 * The tick during which the request to retrieve a missing event notification
	 * is sent to the destination.
	 */
	public double tick;
	/**
	 * The recipient of the request to retrieve a missing event notification.
	 */
	public Destination destination;
	
	/**
	 * Instantiates a new active retrieve request.
	 * 
	 * @param eventId the identifier of the missing event nofication
	 * @param tick the tick during which the request is sent to the destination
	 * @param destination the recipient of the request 
	 */
	public ActiveRetrieveRequest(EventId eventId, double tick, Destination destination) {
		this.eventId = eventId;
		this.tick = tick;
		this.destination = destination;
	}
}
