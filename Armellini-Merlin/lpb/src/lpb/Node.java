package lpb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import com.sun.org.apache.bcel.internal.generic.NEW;

import cern.colt.map.HashFunctions;
import lpb.Utils.*;
import net.sf.jasperreports.components.list.UnusedSpaceImageRenderer;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.Watchee;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

public class Node {
	static private int globalId = 0;
	private int nodeId;
	private ContinuousSpace<Object> space;
	private HashMap<Node, Integer> view;
	private HashMap<Node, Integer> subs;
	private HashMap<Node, Integer> unsubs;
	private HashMap<Event, Integer> events;
	private HashArrayList<EventId> eventIds;
	private HashMap<EventToFetch, Integer> eventsToFetch;
	private static int gossipSubset = 2;
	private static int unsubSetSize = 2;
	private static int subSetSize = 2;
	private static int maxViewSize = 3;
	private static int maxEventSize = 3;
	private static int maxEventIdSize = 5;
	private boolean debug;
	private boolean crashed = false;
	private Object tmpCrashedNode;
	private NdPoint location;
	private boolean unsubbing = false;
	private UnsubbingNode unsubbingNode;
	private int K = 1;
	private HashMap<EventToFetch, Integer> eventsasked;
	private HashMap<EventToFetch, Integer> eventsaskedtwice;
	private HashMap<Integer, Event> deliveredEvents;
	private int ROUNDS_REQ_TIMEOUT = 1;
	private int roundBorn;
	
	public Node(ContinuousSpace<Object> space) {
		Context<Object> context = ContextUtils.getContext(this);
		Parameters params = RunEnvironment.getInstance().getParameters();
		maxViewSize = (Integer)params.getValue("max_view_size");
		gossipSubset = (Integer)params.getValue("gossip_subset");
		unsubSetSize = (Integer)params.getValue("unsub_set_size");
		subSetSize = (Integer)params.getValue("sub_set_size");
		maxEventIdSize = (Integer)params.getValue("max_eventid_size");
		maxEventSize = (Integer)params.getValue("max_event_size");
		debug = params.getBoolean("DEBUG");
		nodeId = ++globalId;
		this.space = space;
		view = new HashMap<Node, Integer>();
		subs = new HashMap<Node, Integer>();
		unsubs = new HashMap<Node, Integer>();
		events = new HashMap<Event, Integer>();
		eventIds = new HashArrayList<EventId>();
		eventsToFetch = new HashMap<EventToFetch, Integer>();
		location = space.getLocation(this);
		unsubbingNode = null;
		eventsasked = new HashMap<EventToFetch, Integer>();
		eventsaskedtwice = new HashMap<EventToFetch, Integer>();
		deliveredEvents = new HashMap<Integer, Event>();
		roundBorn = GossipManager.getRound();
	}
	
	public void setLocation(NdPoint location) {
		this.location = location;
	}
	
	// Detects gossips in proximity and processes them
	
	@Watch(watcheeClassName = "lpb.GossipMsg",
			watcheeFieldNames = "moved",
			triggerCondition = "$watchee.getTarget() == $watcher && $watchee.distanceFromTarget() < 1.0 && !$watcher.getUnsubbing()",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void receiveGossip() {
		Context<Object> context = ContextUtils.getContext(this);
		HashSet<GossipMsg> toReceive = new HashSet<GossipMsg>();
		for(Object msg : context.getObjects(GossipMsg.class)) {
			GossipMsg tmp = (GossipMsg)msg;
			if(tmp.getTarget() == this && tmp.distanceFromTarget() < 1.0) {
				toReceive.add(tmp);
			}
		}
		if(!crashed) {
			consumeGossip(toReceive);
			GossipManager.refreshViewNetwork();
		}
	}
	
	// Detects retrieval messages in proximity and processes them
	
	@Watch(watcheeClassName = "lpb.EventFetchMsg",
			watcheeFieldNames = "moved",
			triggerCondition = "$watchee.getReceiverId() == $watcher.getNodeId() && $watchee.distanceFromTarget() < 1.0 && !$watcher.getUnsubbing()",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void reiceveToFetch() {
		Context<Object> context = ContextUtils.getContext(this);
		HashSet<EventFetchMsg> toReceive = new HashSet<EventFetchMsg>();
		for(Object msg : context.getObjects(EventFetchMsg.class)) {
			EventFetchMsg tmp = (EventFetchMsg)msg;
			if(tmp.getReceiverId() == this.nodeId && tmp.distanceFromTarget() < 1.0) {
				toReceive.add(tmp);
			}
		}
		if(!crashed) {
			for(EventFetchMsg toFetch: toReceive) {
				consumeEventFetchMsg(toFetch);
			}
			context.removeAll(toReceive);
			GossipManager.refreshViewNetwork();
		}
	}
	
	// Handles retrieval requests
	
	public void consumeEventFetchMsg(EventFetchMsg msg) {
		Context<Object> context = ContextUtils.getContext(this);
		if(msg.isReply()) {
			Event e = msg.getReply();
			if(!deliveredEvents.containsValue(e)) {
				events.put(e, 1);
				lpbDeliver(e);
				eventIds.add(e.getEventIdRef());
				eventsToFetch.remove(e);
			}
		}else {
			if(deliveredEvents.containsKey(msg.getToFetch().getEventId())) {
				Event e = deliveredEvents.get(msg.getToFetch().getEventId());
				if(e.getEventIdRef().getEventId() == msg.getToFetch().getEventId()) {
					EventFetchMsg reply = msg.createReply(e);
					context.add(reply);
					space.moveTo(reply, location.getX(), location.getY());
				}
			}
		}
	}
	
	// Handles incoming gossips
	
	private void consumeGossip(HashSet<GossipMsg> toReceive) {
		Context<Object> context = ContextUtils.getContext(this);
		
		for(GossipMsg msg : toReceive) {
			final ArrayList<String> debugArr = new ArrayList<String>();
			debugArr.add("\tSubs:");
			msg.getSubs().keySet().forEach( s ->{debugArr.add("\t\t"+s.nodeId);});
			debugArr.add("\tUnsubs:");
			msg.getUnsubs().keySet().forEach( s ->{debugArr.add("\t\t"+s.nodeId);});
			
			HashMap<Node, Integer> subs_ = msg.getSubs(); // Process subs
			subs_.remove(this);
			for(Node s : subs_.keySet()) {
				if(view.containsKey(s)) {
					view.replace(s, view.get(s)+1);
				}else {
					view.put(s, subs_.get(s)+1);
				}
				
				if(subs.containsKey(s)) {
					subs.replace(s, subs.get(s)+1);
				}else {
					subs.put(s, subs_.get(s)+1);
				}
			}
			HashMap<Node, Integer> unsubs = msg.getUnsubs(); // Process unsubs
			for(Node unsub : unsubs.keySet()) {
				this.unsubs.put(unsub, unsubs.get(unsub));
			}
			for(Node unsub : this.unsubs.keySet()) {
				view.remove(unsub);
				subs.remove(unsub);
			}
			purgeUnsubs();

			debugArr.add("\tView:");
			view.keySet().forEach( a ->{debugArr.add("\t\t"+a.nodeId);});
			purgeView();

			debugArr.add("\tPURGED View:");
			view.keySet().forEach( a ->{debugArr.add("\t\t"+a.nodeId);});
			purgeSubs();
			for(Event event : msg.getEvents()) { // Process events
				if(!deliveredEvents.containsKey(event.getEventIdRef().getEventId())) {
					events.put(event, msg.getEventAge(event));
					lpbDeliver(event);
					eventIds.add(event.getEventIdRef());
				} else if (events.containsKey(event) && events.get(event) < msg.getEventAge(event)) {
					events.put(event, msg.getEventAge(event));
				}
			}
			for(EventId eventId : msg.getEventIds()) { // Process event ids
				if(!deliveredEvents.containsKey(eventId.getEventId()) && eventId.getTimestamp() >= roundBorn) {
					EventToFetch toFetch = new EventToFetch(eventId,GossipManager.getRound());
					eventsToFetch.put(toFetch, msg.getSenderId());
				}
			}
			purgeEventIds();
			purgeEvents();
			context.remove(msg);

			if(debug && nodeId==((Node)context.getObjects(Node.class).get(0)).nodeId)debugArr.forEach( s->{System.out.println(s);});
		}
	}
	
	// Function simulating event delivery to the application layer
	
	private void lpbDeliver(Event event) {
		deliveredEvents.put(event.getEventIdRef().getEventId(), event);
	}
	
	// Age based unsub purging
	
	private void purgeUnsubs() {
		while(unsubs.size() > unsubSetSize) {
			Node n = select_process(unsubs);
			unsubs.remove(n);
		}
	}
	
	// Age based event purging
	
	private void purgeEvents() {
		int diff = events.size() - maxEventSize;
		if(diff > 0) {
			for(int i = 0; i < diff; i++) {
				int max = -1;
				Event oldestEvent = null;
				for(Event e : events.keySet()) {
					if(events.get(e) > max) {
						max = events.get(e);
						oldestEvent = e;
					}
				}
				events.remove(oldestEvent);
			}


		}
	}
	
	// Event ids purging (random)
	
	private void purgeEventIds() {
		while(eventIds.size() > maxEventIdSize) {
			eventIds.remove(0);
		}
	}
	
	// Age based subs purging
	
	private void purgeSubs() {
		while(subs.size() > subSetSize) {
			Node n = select_process(subs);
			subs.remove(n);
		}
	}
	
	// Purging auxiliary function
	
	private Node select_process(HashMap<Node,Integer> map) {
		Node toRtn = null;
		int max = -1;
		for(Node n : map.keySet()) {
			if(map.get(n) > max) {
				max = map.get(n);
				toRtn = n;
			}
		}
		return toRtn;
	}
	
	
	// Age based view purging
	
	private void purgeView() {
		while(view.size() > maxViewSize) {
			Node n = select_process(view);
			subs.put(n,view.remove(n));
		}
	}
	
	public NdPoint getLocation() {
		return location;
	}
	
	public UnsubbingNode getUnsubbingNode() {
		return unsubbingNode;
	}
	
	public HashMap<Node, Integer> getUnsubs() {
		return unsubs;
	}
	
	// Gossiping function, sends gossips to subset in view
	
	public void spawnGossip() {
		if(!crashed) {
			HashSet<Node> targets = new HashSet<Node>(view.keySet());
			while(targets.size() > gossipSubset) {
				Node tmp = null;
				int item = RandomHelper.nextIntFromTo(0, targets.size()-1);
				int i = 0;
				for(Node selectedNode : targets)
				{
				    if (i == item)
				        tmp = selectedNode;
				    i++;
				}
				targets.remove(tmp);
			}
			Context<Object> context = ContextUtils.getContext(this);
			for(Event e : events.keySet()) {
				events.put(e, events.get(e) + 1);
			}
			for(Node unsub : unsubs.keySet()) {
				unsubs.put(unsub, unsubs.get(unsub) + 1);
			}

			NdPoint spacePt = space.getLocation(this);
			for(Node target : targets) {
				if(!unsubbing)
					subs.put(this, 0);
				GossipMsg toSend = new GossipMsg(nodeId, target, space, subs, unsubs, events, eventIds);
				if(!unsubbing)
					subs.remove(this);
				context.add(toSend);
				space.moveTo(toSend, spacePt.getX(), spacePt.getY());
			}
			events = new HashMap<Event, Integer>();
			if(unsubbing) {
				context.remove(this);
				context.add(unsubbingNode);
				space.moveTo(unsubbingNode, location.getX(), location.getY());
			} else {
				askEvents(); 
			}
		}
	}
	
	// Event retrieval routine
	
	public void askEvents() {
		ArrayList<EventToFetch> toRmv = new ArrayList<Utils.EventToFetch>();
		Context<Object> context = ContextUtils.getContext(this);
		if(!eventsToFetch.isEmpty()) {
			for(EventToFetch e : eventsToFetch.keySet()) {
				if(GossipManager.getRound()-e.getRounds()>=K) {
					if(!deliveredEvents.containsKey(e.getEventIdRef().getEventId())){
						NdPoint sender = GossipManager.getNodePosition(space,eventsToFetch.get(e));
						if(!eventsasked.containsKey(e) && sender != null) {
							EventFetchMsg msg = new EventFetchMsg(space, location, nodeId, GossipManager.getNodePosition(space,eventsToFetch.get(e)), eventsToFetch.get(e), e.getEventIdRef(), null);
							context.add(msg);
							space.moveTo(msg, location.getX(), location.getY());
							eventsasked.put(e, GossipManager.getRound());
						} else {
							NdPoint origin = GossipManager.getNodePosition(space, e.getEventIdRef().getOriginId());
							if((eventsasked.containsKey(e) && eventsasked.get(e) >= ROUNDS_REQ_TIMEOUT) || origin == null) {
								int receiver = GossipManager.getRandomActiveNode(nodeId).getNodeId();
								EventFetchMsg msg = new EventFetchMsg(space, location, nodeId, GossipManager.getNodePosition(space,receiver), receiver , e.getEventIdRef(), null);
								context.add(msg);
								space.moveTo(msg, location.getX(), location.getY());
								eventsaskedtwice.put(e, GossipManager.getRound());
							}else {
								int receiver = e.getEventIdRef().getOriginId();
								EventFetchMsg msg = new EventFetchMsg(space, location, nodeId, GossipManager.getNodePosition(space,receiver), receiver, e.getEventIdRef(), null);
								context.add(msg);
								space.moveTo(msg, location.getX(), location.getY());
							}
						}
					}else {
						toRmv.add(e);
					}
				}
			}
			
		}
		for(EventToFetch rm : toRmv) {
			eventsToFetch.remove(rm);
		}
	}
	
	public int deliveredEventsCount() {
		return deliveredEvents.size();
	}
	
	public void addToView(Node toAdd, Integer age) {
		view.put(toAdd, age);
	}
	
	public void addSub(Node toAdd, Integer age) {
		subs.put(toAdd, age);
	}
	
	public void removeFromView(Node toRemove) {
		view.remove(toRemove);
	}
	public int getViewSize() {
		return view.size();
	}
	public int getSubSize() {
		return subs.size();
	}
	public HashMap<Node, Integer> getView() {
		return view;
	}
	public HashMap<Node, Integer> getSubs() {
		return subs;
	}
	
	public boolean getUnsubbing() {
		return unsubbing;
	}
	
	@Override
	public int hashCode() {
		return nodeId;
	}
	
	// Crash function
	
	public void crash() {
		if(!crashed) {
			Context<Object> context = ContextUtils.getContext(this);
			crashed = true;
			tmpCrashedNode = new CrashedNode();
			context.add(tmpCrashedNode);
			NdPoint myPoint = space.getLocation(this);
			space.moveTo(tmpCrashedNode, myPoint.getX(),myPoint.getY());
		}
	}

	// Crash recovery function
	
	public void recover() {
		if(crashed){
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(tmpCrashedNode);
			tmpCrashedNode = null;
			crashed = false;
		}
	}
	
	// Event generation function
	
	public int createEvent() {
		int eventid = -1;
		if(!crashed) {
			int hash = Objects.hash(nodeId, RandomHelper.nextIntFromTo(1,1000), RandomHelper.nextIntFromTo(1,1000));
			Event e = new Event(""+hash, nodeId);
			events.put(e, 0);
			eventIds.add(e.getEventIdRef());
			eventid = e.getEventIdRef().getEventId();
			lpbDeliver(e);
		}
		return eventid;
	}
	
	public int getNodeId() {
		return nodeId;
	}
	
	// Unsubscribing function
	
	public void unsub() {
		this.unsubs.put(this, 0);
		unsubbingNode = new UnsubbingNode(nodeId);
		unsubbing = true;
	}
	
	// Place holder class for crashed nodes used for "CRASHED" label underneath nodes
	
	public class CrashedNode {}
	
	// Place holder class for unsubscribed nodes which are not completely forgotten yet
	
	public class UnsubbingNode {
		private int nodeId;
		
		public UnsubbingNode(int nodeId) {
			this.nodeId = nodeId;
		}
		
		public int getNodeId() {
			return nodeId;
		}
		
		@Watch(watcheeClassName = "lpb.GossipMsg",
				watcheeFieldNames = "moved",
				triggerCondition = "$watchee.getTargetId() == $watcher.getNodeId() && $watchee.distanceFromTarget() < 1.0",
				whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
		public void receiveGossip() {
			Context<Object> context = ContextUtils.getContext(this);
			HashSet<GossipMsg> toReceive = new HashSet<GossipMsg>();
			for(Object msg : context.getObjects(GossipMsg.class)) {
				GossipMsg tmp = (GossipMsg)msg;
				if(tmp.getTarget().getNodeId() == this.nodeId && tmp.distanceFromTarget() < 1.0) {
					toReceive.add(tmp);
				}
			}
			for(GossipMsg msg: toReceive) {
				context.remove(msg);
			}
		}
	
		@Watch(watcheeClassName = "lpb.EventFetchMsg",
				watcheeFieldNames = "moved",
				triggerCondition = "$watchee.getReceiverId() == $watcher.getNodeId() && $watchee.distanceFromTarget() < 1.0",
				whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
		public void reiceveToFetch() {
			Context<Object> context = ContextUtils.getContext(this);
			HashSet<EventFetchMsg> toReceive = new HashSet<EventFetchMsg>();
			for(Object msg : context.getObjects(EventFetchMsg.class)) {
				EventFetchMsg tmp = (EventFetchMsg)msg;
				if(tmp.getReceiverId() == this.nodeId && tmp.distanceFromTarget() < 1.0) {
					toReceive.add(tmp);
				}
			}
			for(EventFetchMsg msg: toReceive) {
				context.remove(msg);
			}
		}
		
	}
}
