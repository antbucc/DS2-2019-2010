package lpbcast;

/**
 * Message that a node wants to share with others.
 * 
 * Each event has a unique identifier.
 */
public class Event {
	String eventId;
	int sender;
	int age;

	/**
	 * Constructor of Event.
	 * 
	 * @param sender  the generator of the event.
	 * @param eventId the id of the event. It is assumed to be in the form
	 *                "nodeId_messageId".
	 */
	public Event(int sender, String eventId) {
		this.sender = sender;
		this.eventId = eventId;
		this.age = 0;
	}

	/**
	 * Constructor of Event.
	 * 
	 * @param e the event to copy.
	 */
	public Event(Event e) {
		this.eventId = e.eventId;
		this.sender = e.sender;
		this.age = e.age;
	}

	/**
	 * Increments by one the age of the event.
	 */
	public void incrementAge() {
		age += 1;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Event) {
			Event e = (Event) o;
			if (e.eventId == this.eventId) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return eventId.hashCode();
	}
}
