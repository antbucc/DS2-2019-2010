/**
 * 
 */
package lpbcast;

import java.util.UUID;

/**
 * Represents the unique identifier of an event notification.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 *
 */
public class EventId {

	/**
	 * The identifier of an event notification
	 */
	public UUID id;
	/**
	 * The identifier of the process which created this event notification.
	 */
	public int origin;
	
	/**
	 * Instantiates a new event notification identifier.
	 * 
	 * @param id the identifier of the event notification
	 * @param origin the identifier of the originator process
	 */
	public EventId(UUID id, int origin) {
		this.id = id;
		this.origin = origin;
	}
	
	/**
	 * Instantiates a new event notification identifier.
	 * 
	 * @param origin the identifier of the originator process
	 */
	public EventId(int origin) {
		this.id = UUID.randomUUID(); //generate random id for event
		this.origin = origin;
	}
	
	/**
	 * Instantiates a new event notification identifier.
	 * 
	 * @param eId the identifier of the event notification itself
	 */
	public EventId(EventId eId) {
		this.id = eId.id;
		this.origin = eId.origin;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventId other = (EventId) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (origin != other.origin)
			return false;
		return true;
	}
	
	/**
	 * Returns a new object which is the copy of the event identifier on which 
	 * the method is called.
	 * 
	 * @return the copy of the event identifier
	 */
	public EventId clone() {
		return new EventId(this);
	}
	
}
