package probabilisticBroadcast;

import java.util.HashMap;
import java.util.HashSet;

public class GossipMessage {
	
	private HashSet<String> subs, unSubs;
	private HashSet<Event> events;
	private String sender;
	private HashMap<String, String> eventIds;	// keys the event id, values the creator process id
	
	public GossipMessage(HashSet<String> subs, HashSet<String> unSubs, HashSet<Event> events, HashMap<String, String> eventIds, String sender) {
		this.subs = subs;
		this.unSubs = unSubs;
		this.eventIds = eventIds;
		this.events = events;
		this.sender = sender;
	}
	/**
	 * Create an empty gossip message
	 */
	public GossipMessage(String sender) {
		this(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>(), sender);
	}
	public HashSet<String> getSubs() {
		return subs;
	}
	public void setSubs(HashSet<String> subs) {
		this.subs = subs;
	}
	public HashSet<String> getUnSubs() {
		return unSubs;
	}
	public void setUnSubs(HashSet<String> unSubs) {
		this.unSubs = unSubs;
	}
	public HashMap<String, String> getEventIds() {
		return eventIds;
	}
	public void setEventIds(HashMap<String, String> eventIds) {
		this.eventIds = eventIds;
	}
	public HashSet<Event> getEvents() {
		return events;
	}
	public void setEvents(HashSet<Event> events) {
		this.events = events;
	}
	public String getSender() {
		return sender;
	}
	/**
	 * Add a subscription
	 * @param processId
	 */
	public void withSubscription(String processId) {
		this.subs.add(processId);
	}
	/**
	 * Add an unsubscription
	 * @param processId
	 */
	public void withUnsubscription(String processId) {
		this.unSubs.add(processId);
	}
	/**
	 * Add an event
	 * @param e
	 */
	public void withEvent(Event e) {
		this.events.add(e);
	}
	/**
	 * Add an event ID
	 * @param eventId
	 */
	public void withEventId(String eventId, String creator) {
		this.eventIds.put(eventId, creator);
	}
}
