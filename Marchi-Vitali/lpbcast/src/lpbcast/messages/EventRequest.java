package lpbcast.messages;

import communication.Message;
import lpbcast.EventId;

public class EventRequest extends Message {
	
	private EventId eventId;

	public EventRequest(int senderId, int destId, EventId eventId) {
		super(senderId, destId);
		this.eventId = eventId;
	}
	
	public EventId getEventId() {
		return eventId;
	}
}
