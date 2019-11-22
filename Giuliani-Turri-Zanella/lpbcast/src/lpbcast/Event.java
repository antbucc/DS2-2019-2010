/**
 * 
 */
package lpbcast;

/**
 * Represents an event notification.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 * 
 */
public class Event {

	/**
	 * The identifier of an event notification. It also includes the identifier
	 * of the originator process.
	 */
	public EventId eventId;
	/**
	 * The number of rounds the message has spent in the system by the current
	 * moment.
	 */
	public int age;
	
	/**
	 * Instantiates a new event.
	 * 
	 * @param eventId the identifier of the event notification
	 * @param age the number of rounds the message has spent in the system
	 */
	public Event(EventId eventId,  int age) {
		this.eventId = eventId;
		this.age = age;
	}
	
	/**
	 * Instantiates a new event.
	 * 
	 * @param e the event itself.
	 */
	public Event(Event e) {
		this.eventId = e.eventId;
		this.age = e.age;
	}
	
	/**
	 * Returns a new object which is the copy of the event on which the method is
	 * called.
	 * 
	 * @return the copy of the event
	 */
	public Event clone() {
		return new Event(this);
	}
}
