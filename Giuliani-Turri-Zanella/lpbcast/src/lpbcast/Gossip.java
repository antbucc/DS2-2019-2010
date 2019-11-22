/**
 * 
 */
package lpbcast;

import java.util.HashSet;

/**
 * Represents a gossip message.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 */
public class Gossip extends Message {
	
	/**
	 * The piggybacked event notifications.
	 */
	public HashSet<Event> events;
	/**
	 * The piggybacked subscriptions.
	 */
	public HashSet<Integer> subs;
	/**
	 * The piggybacked unsubscriptions.
	 */
	public HashSet<Integer> unsubs;
	/**
	 * The piggybacked event notifications identifiers.
	 */
	public HashSet<EventId> eventIds;
	
	/**
	 * Instantiates a new gossip.
	 * 
	 * @param tick the tick during which the gossip is sent
	 * @param type the type of the message
	 * @param sender the sender of the gossip
	 * @param events the piggybacked event notifications
	 * @param subs the piggybacked subscriptions
	 * @param unsubs the piggybacked unsubscriptions
	 * @param eventIds the piggybacked event notifications identifiers
	 */
	public Gossip(double tick, MessageType type, int sender, HashSet<Event> events, HashSet<Integer> subs, HashSet<Integer> unsubs, HashSet<EventId> eventIds) {
		super(tick, type, sender);
		this.events = events;
		this.subs = subs;
		this.unsubs = unsubs;
		this.eventIds = eventIds;
	}
	
	/**
	 * Instantiates a new gossip.
	 * 
	 * @param sender the sender of the gossip
	 * @param events the piggybacked event notifications
	 * @param subs the piggybacked subscriptions
	 * @param unsubs the piggybacked unsubscriptions
	 * @param eventIds the piggybacked event notifications identifiers
	 */
	public Gossip(int sender, HashSet<Event> events, HashSet<Integer> subs, HashSet<Integer> unsubs, HashSet<EventId> eventIds) {
		super(Message.MessageType.GOSSIP, sender);
		this.events = events;
		this.subs = subs;
		this.unsubs = unsubs;
		this.eventIds = eventIds;
	}
}
