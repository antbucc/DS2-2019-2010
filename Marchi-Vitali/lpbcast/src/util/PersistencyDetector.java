package util;

import java.util.HashMap;
import java.util.HashSet;

import lpbcast.Event;
import lpbcast.EventId;
import repast.simphony.engine.schedule.ScheduledMethod;

public class PersistencyDetector {
	
	private static PersistencyDetector instance = null;
	
	private HashMap<EventId, Integer> persistency = new HashMap<>();
	private HashMap<EventId, Integer> counter = new HashMap<>();
	private HashMap<EventId, Integer> latency = new HashMap<>();
	
	private PersistencyDetector() {}
	
	public static PersistencyDetector getInstance() {
		if (instance == null) {
			instance = new PersistencyDetector();
		} 
		return instance;
	}
	
	public void addEvent(EventId eventId) {
		// Initialize new event
		persistency.put(eventId, 0);
		counter.put(eventId, 0);
		latency.put(eventId, 1);
	}
	
	public void updatePersistency(HashMap<EventId, Event> events) {		
		for (EventId eventId : persistency.keySet()) {
			if (events.containsKey(eventId)) {
				persistency.replace(eventId, 2);
			} else {
				persistency.replace(eventId, 0, 1);
			}
		}
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 1000)
	public void startTick() {
		for (EventId eventId : persistency.keySet()) {
			persistency.replace(eventId, 0);
		}
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void endTick() {
		HashSet<EventId> toRemove = new HashSet<>();
		for (EventId eventId : persistency.keySet()) {
			if (persistency.get(eventId) == 2) {
				counter.replace(
						eventId, counter.get(eventId) + latency.get(eventId));
				latency.replace(eventId, 1);
			} else if (latency.get(eventId) < 8) {
				latency.replace(eventId, latency.get(eventId) + 1);
			} else {
				toRemove.add(eventId);
				Logger.getLogger().logPersistency(eventId.getId(), counter.get(eventId));
			}
		}
		for (EventId event : toRemove) {
			persistency.remove(event);
			counter.remove(event);
			latency.remove(event);
		}
	}
}
