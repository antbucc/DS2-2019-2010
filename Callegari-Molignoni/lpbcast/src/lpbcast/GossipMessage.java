package lpbcast;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Message sent at each round by a node to (a subset of) its view.
 */
public class GossipMessage extends Message {
	HashSet<Event> events = new HashSet<Event>();  // Event notifications
	LinkedList<String> eventIds = new LinkedList<String>();  // Event IDs
	HashSet<Integer> unSubs = new HashSet<Integer>();  // Unsubscriptions
	HashSet<Integer> subs = new HashSet<Integer>();  // Subscriptions

	
	/**
	 * Constructor of GossipMessage.
	 * 
	 * @param events   the events buffer of the node.
	 * @param eventIds the eventsIds buffer of the node.
	 * @param unSubs   the unSubs buffer of the node.
	 * @param subs     the subs buffer of the node.
	 * @param sender   the sender of the gossip message.
	 */
	public GossipMessage(HashSet<Event> events, LinkedList<String> eventIds, HashSet<Integer> unSubs,
			HashSet<Integer> subs, int sender) {
		this.events = events;
		this.eventIds = eventIds;
		this.unSubs = unSubs;
		this.subs = subs;
		this.sender = sender;
	}
}
