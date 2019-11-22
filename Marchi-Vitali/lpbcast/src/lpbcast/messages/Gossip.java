package lpbcast.messages;

import java.util.HashMap;
import java.util.HashSet;

import communication.Message;
import lpbcast.Event;
import lpbcast.EventId;

public class Gossip extends Message {
	
	private HashSet<Integer> subs = new HashSet<>();
	private HashSet<Integer> unsubs = new HashSet<>();
	private HashMap<EventId, Event> events = new HashMap<>();
	private HashSet<EventId> eventIds = new HashSet<>();

	public Gossip(
			int senderId, int destId, 
			HashSet<Integer> subs,
			HashSet<Integer> unsubs,
			HashMap<EventId, Event> events,
			HashSet<EventId> eventIds
    ) {
		super(senderId, destId);
		this.subs = subs;
		this.subs.add(senderId);
		this.unsubs = unsubs;
		this.events = events;
		this.eventIds = eventIds;
	}

	public HashSet<Integer> getSubs() {
		return subs;
	}

	public HashSet<Integer> getUnsubs() {
		return unsubs;
	}

	public HashMap<EventId, Event> getEvents() {
		return events;
	}

	public HashSet<EventId> getEventIds() {
		return eventIds;
	}
}
