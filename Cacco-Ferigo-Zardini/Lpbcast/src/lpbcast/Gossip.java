package lpbcast;

import java.util.ArrayList;
import java.util.TreeMap;

public class Gossip {
	private ArrayList<Event> events = new ArrayList<>();
	private ArrayList<String> eventIds = new ArrayList<>();
	private ArrayList<Node> subs = new ArrayList<>();
	private TreeMap<Node, Integer> unsubs = new TreeMap<>();
	private Node sender;

	public Gossip(Node sender, ArrayList<Event> events, ArrayList<String> eventIds, TreeMap<Node, Integer> subs,
			TreeMap<Node, Integer> unsubs) {

		this.sender = sender;

		for (Event e : events) {
			this.events.add(e.cloneEvent());
		}

		for (String id : eventIds) {
			this.eventIds.add(new String(id));
		}

		for (Node node : subs.keySet()) {
			this.subs.add(node);
		}
		
		this.subs.add(sender);
		
		
		for (Node node : unsubs.keySet()) {
			this.unsubs.put(node, Integer.valueOf(unsubs.get(node).intValue()));
		}
	}

	public ArrayList<Event> getEvents() {
		return events;
	}

	public ArrayList<String> getEventIds() {
		return eventIds;
	}

	public ArrayList<Node> getSubs() {
		return subs;
	}

	public TreeMap<Node, Integer> getUnsubs() {
		return unsubs;
	}

	public Node getSender() {
		return sender;
	}
}
