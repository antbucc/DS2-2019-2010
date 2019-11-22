package lpbcast.messages;

import communication.Message;
import lpbcast.Event;

public class EventResponse extends Message {
	
	private Event event;

	public EventResponse(int senderId, int destId, Event event) {
		super(senderId, destId);
		this.event = event;
	}
	
	public Event getEvent() {
		return event;
	}
}
