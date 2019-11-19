package probabilisticBroadcast;

import java.util.HashMap;
import java.util.UUID;

public class Event {
	private String eventId, creator, payload;
	private HashMap<String, Integer> knownBy;	// key: process id, value round when event is received
	private int startRound;
	
	public Event(String eventId, String creator, String payload, int startRound) {
		this.eventId = eventId;
		this.creator = creator;
		this.payload = payload;
		this.knownBy = new HashMap<>();
		this.startRound = startRound;
	}
	
	public void seen(String processId, int round) {
		if (!this.knownBy.containsKey(processId)) {
			this.knownBy.put(processId, round);
		}
	}
	
	/**
	 * Computes the number of rounds needed by the event to be known around
	 * @return the number of rounds took for spreading around
	 */
	public int roundsNeeded() {
		int largest = 0;
		for (int x: this.knownBy.values()) {
			if (x > largest)
				largest = x;
		}
		return largest - this.startRound;
	}
	public int knownBy() {
		return this.knownBy.size();
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
	/**
	 * Generate a random Event, with random id and payload
	 * @param creator the id of the process that generates the event
	 */
	public Event (String creator, int startRound) {
		this(UUID.randomUUID().toString(), creator, UUID.randomUUID().toString(), startRound);
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Event)) 
			return false;
		Event event = (Event) obj;
		return this.creator.equals(event.getCreator()) && this.eventId.equals(event.getEventId()) && this.payload.equals(event.getPayload());
	}
	@Override
	public int hashCode() {
		final int prime = 31;
	    int result = 1;
	    result = prime * result + ((this.creator == null) ? 0 : this.creator.hashCode());
	    result = prime * result + ((this.eventId == null) ? 0 : this.eventId.hashCode());
	    result = prime * result + ((this.payload == null) ? 0 : this.payload.hashCode());
	    return result;
	}
}
