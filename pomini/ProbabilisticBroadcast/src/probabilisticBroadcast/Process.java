package probabilisticBroadcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;

public class Process {
	public static int MAX_VIEW_SIZE = 10, MAX_SUBS = 10, MAX_EVENT_SIZE = 10, MAX_EVENT_IDS = 10, MAX_UNSUBS = 10, GOSSIP_SIZE = 10, K_ROUNDS = 4;
	
	private Set<Event> events, generatedEvents;	// events in memory and events generated by this process
	private Set<String> subs, unSubs;	// list of known processes
	private List<String> eventIds, eventCreators; // inserted by arrival time, parallel lists
	private Map<String, Process> view, allProcesses;	// current view, all nodes in read only
	private Set<EventElement> elementBuffer;	// buffer of elements to retrieve
	private String processId;
	
	private ReentrantLock functionsMutualExclusionLock;
	
	private RepastEdge<Object> lastEdge;
	
	public Process(Set<String> subs, Set<String> unSubs, Map<String, Process> view, Set<Event> events, List<String> eventIds, List<String> eventCreators, Set<EventElement> elementBuffer, Set<Event> generatedEvents) {
		this.eventIds = eventIds;
		this.events = events;
		this.subs = subs;
		this.unSubs = unSubs;
		this.view = view;
		this.elementBuffer = elementBuffer;
		this.processId = UUID.randomUUID().toString();
		this.generatedEvents = generatedEvents;
		this.eventCreators = eventCreators;
		this.lastEdge = null;
		this.functionsMutualExclusionLock = new ReentrantLock();
	}
	
	public Process() {
		this(new HashSet<>(), new HashSet<>(), new HashMap<>(), new HashSet<>(), new ArrayList<>(), new ArrayList<>(), new HashSet<>(), new HashSet<>());
	}
	
	public Process(Map<String, Process> view) {
		this(new HashSet<>(), new HashSet<>(), view, new HashSet<>(), new ArrayList<>(), new ArrayList<>(), new HashSet<>(), new HashSet<>());
	}
	
	public static void setParameters(int maxViewSize, int maxSubs, int maxUnsubs, int maxEvents, int maxEventIds, int gossipSize, int kRounds) {
		MAX_VIEW_SIZE = maxViewSize;
		MAX_SUBS = maxSubs;
		MAX_UNSUBS = maxUnsubs;
		MAX_EVENT_SIZE = maxEvents;
		MAX_EVENT_IDS = maxEventIds;
		GOSSIP_SIZE = gossipSize;
		K_ROUNDS = kRounds;
		System.out.println("Process parameter set correctly");
	}
	
	public void receive(GossipMessage gossip) {
		this.functionsMutualExclusionLock.lock();
		// handle unsubscriptions
		for (String unsub: gossip.getUnSubs()) {
			if (this.view.containsKey(unsub))
				this.view.remove(unsub);
		}
		this.unSubs.addAll(gossip.getUnSubs());
		this.subs.removeAll(gossip.getUnSubs());
		// handle subscriptions
		for (String sub: gossip.getSubs()) {
			//this.viewLock.lock();
			if (!this.view.containsKey(sub)) 
				this.view.put(sub, this.allProcesses.get(sub));
		}
		this.subs.addAll(gossip.getSubs());
		// handle events
		for (Event e: gossip.getEvents()) {
			if (!this.eventIds.contains(e.getEventId())) {
				e.seen(this.processId, (int)RepastEssentials.GetTickCount());
				this.events.add(e);
				// deliver to application
				this.eventIds.add(e.getEventId());
				this.eventCreators.add(e.getCreator());
			}
		}
		for (String id: gossip.getEventIds().keySet()) {
			if (!this.eventIds.contains(id)) {
				EventElement element = new EventElement(id, gossip.getSender(), gossip.getEventIds().get(id), (int)RepastEssentials.GetTickCount());
				this.elementBuffer.add(element);
			}
		}
		this.truncateSetsAndView();
		this.functionsMutualExclusionLock.unlock();
	}
	
	@ScheduledMethod ( start = 1 , interval = 1)
	public void emitGossip() {
		this.functionsMutualExclusionLock.lock();
		GossipMessage gossip = new GossipMessage(this.processId);
		gossip.getSubs().addAll(this.subs);
		gossip.getSubs().add(this.processId);
		for (int i = 0; i < this.eventIds.size(); i++) {
			gossip.withEventId(this.eventIds.get(i), this.eventCreators.get(i));
		}
		gossip.getEvents().addAll(this.events);
		gossip.getUnSubs().addAll(this.unSubs);
		// send
		List<Process> toSend;
		if (this.view.size() <= GOSSIP_SIZE)
			toSend = new ArrayList<Process>(this.view.values());
		else {
			// if too many processes in the view, choose some randomly
			List<Process> processes = new ArrayList<Process>(this.view.values());
			Collections.shuffle(processes);
			toSend = new ArrayList<Process>(GOSSIP_SIZE);
			for (int i = 0; i < GOSSIP_SIZE; i++)
				toSend.add(processes.get(i));				
		}
		for (Process process: toSend) {
			if (process != null)
				this.send(process, gossip);
		}
		this.events.clear(); 
		this.functionsMutualExclusionLock.unlock();
	}
	
	public void send(Process process, GossipMessage gossip) {
		// creo un link
		if (this.allProcesses.containsKey(process.getProcessId())) { // if the process still exists
			Context<Object> context = ContextUtils.getContext(process);
			Network<Object> net = (Network<Object>)context.getProjection("gossip network");
			if (this.lastEdge != null)
				net.removeEdge(this.lastEdge);
			this.lastEdge = net.addEdge(this, process);
			process.receive(gossip); 
		} else {	// otherwise remove the process
			this.view.remove(process.getProcessId());
			this.unSubs.add(process.getProcessId());
		}
	}
	
	@ScheduledMethod ( start = 1 , interval = 1)
	public void retrieveEvents() {
		this.functionsMutualExclusionLock.lock();
		int currentRound = (int) RepastEssentials.GetTickCount();
		List<EventElement> toRemove = new ArrayList<Process.EventElement>();
		for (EventElement element: this.elementBuffer) {
			if (currentRound - element.round > K_ROUNDS) {
				if (!this.eventIds.contains(element.eventId)) {
					// ask to the gossip sender
					Event event = null;
					if (this.view.containsKey(element.getSender())) {
						Process toAsk = this.view.get(element.getSender());
						if (toAsk != null)
							event = toAsk.askEvent(element.getEventId());
					}
					// in case the event has not arrived, or the sender process is not yet in the view, try with a random one
					if (event == null) {
						List<Process> allKnownProcesses = new ArrayList<>(this.view.values());
						Process selectedProcess = allKnownProcesses.get(RandomHelper.nextIntFromTo(0, allKnownProcesses.size() - 1));
						if (selectedProcess != null)
							event = selectedProcess.askEvent(element.getEventId());
					}
					// in case not yet arrived, ask to the creator (if known)
					if (event == null && this.allProcesses.containsKey(element.getCreator())) {
						event = this.allProcesses.get(element.getCreator()).askEvent(element.getEventId());
					}
					// if the event has arrived, store it
					if (event != null) {
						this.events.add(event);
						event.seen(this.processId, currentRound);
						// deliver to application
						this.eventIds.add(event.getEventId());
						this.eventCreators.add(event.getCreator());
					} 
					// otherwise nothing is done, waiting for the gossip protocol to let the process know someone else
				} else {
					toRemove.add(element);
				}
			}
		}
		this.elementBuffer.removeAll(toRemove);
		this.functionsMutualExclusionLock.unlock();
	}
	/**
	 * Ask a process to return an event
	 * @param eventId the id of the event to be returned
	 * @return the Event, if the process has it. Otherwise null
	 */
	public Event askEvent(String eventId) {
		for (Event e: this.generatedEvents) {
			if (e.getEventId().equals(eventId))
				return e;
		}
		for (Event e: this.events) {
			if (e.getEventId().equals(eventId))
				return e;
		}
		return null;
	}
	
	private void truncateSetsAndView() {
		// view
		if (this.view.size() > MAX_VIEW_SIZE) {
			List<String> keys = new ArrayList<>(this.view.keySet());
			while (this.view.size() > MAX_VIEW_SIZE) {
				int victim = RandomHelper.nextIntFromTo(0, keys.size() - 1);
				this.view.remove(keys.get(victim));
				keys.remove(victim);
			}
		}
		// unsubs
		if (this.unSubs.size() > MAX_UNSUBS) {
			List<String> items = new ArrayList<>(this.unSubs);
			while (this.unSubs.size() > MAX_UNSUBS) {
				int victim = RandomHelper.nextIntFromTo(0, items.size() - 1);
				this.unSubs.remove(items.get(victim));
				items.remove(victim);
			}
		}
		// subs
		if (this.subs.size() > MAX_SUBS) {
			List<String> items = new ArrayList<>(this.subs);
			while (this.subs.size() > MAX_SUBS) {
				int victim = RandomHelper.nextIntFromTo(0, items.size() - 1);
				this.subs.remove(items.get(victim));
				items.remove(victim);
			}
		}
		// events
		if (this.events.size() > MAX_EVENT_SIZE) {
			List<Event> items = new ArrayList<>(this.events);
			while (this.events.size() > MAX_EVENT_SIZE) {
				int victim = RandomHelper.nextIntFromTo(0, items.size() - 1);
				this.events.remove(items.get(victim));
				items.remove(victim);
			}
		}
		// eventIDs, removing the oldest (the first k ones)
		if (this.eventIds.size() > MAX_EVENT_IDS) {
			int to_delete = this.eventIds.size() - MAX_EVENT_IDS;
			for (int i = 0; i < to_delete; i++) {
				this.eventIds.remove(0);
				this.eventCreators.remove(0);
			}
		}
	}
	
	public void suicide() {
		this.functionsMutualExclusionLock.lock();
		GossipMessage gossip = new GossipMessage(this.processId);
		gossip.getUnSubs().add(this.processId);
		List<Process> currentProcesses = new ArrayList<>(this.view.values());
		for (Process p: currentProcesses) {	// send special gossip to notify the deletion
			if (p != null)
				p.receive(gossip);
		}
		if (this.lastEdge != null) {
			Context<Object> context = ContextUtils.getContext(this);
			Network<Object> net = (Network<Object>)context.getProjection("gossip network");
			net.removeEdge(this.lastEdge);
		}
		this.functionsMutualExclusionLock.unlock();
	}
	
	public void newEvent(Event event) {
		this.events.add(event);
		this.generatedEvents.add(event);
		event.seen(this.processId, (int)RepastEssentials.GetTickCount());
	}
	
	public void setView(Map<String, Process> view) {
		this.view = view;
	}
	
	public Map<String, Process> getView() {
		return view;
	}
	
	public String getProcessId() {
		return processId;
	}
	
	public void setAllProcesses(Map<String, Process> allProcesses) {
		this.allProcesses = allProcesses;
	}
	
	private class EventElement {
		private String eventId, sender, creator;
		private int round;
		
		public EventElement(String eventId, String sender, String creator, int round) {
			this.eventId = eventId;
			this.sender = sender;
			this.round = round;
			this.creator = creator;
		}

		public String getEventId() {
			return eventId;
		}

		public String getSender() {
			return sender;
		}

		public int getRound() {
			return round;
		}
		
		public String getCreator() {
			return creator;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof EventElement))
				return false;
			EventElement element = (EventElement) obj;
			return this.eventId.equals(element.getEventId()) && this.sender.equals(element.getSender()) && this.round == element.getRound();
		}
		@Override
		public int hashCode() {
			final int prime = 31;
		    int result = 1;
		    result = prime * result + ((this.eventId == null) ? 0 : this.eventId.hashCode());
		    result = prime * result + (int) (this.round ^ (this.round >>> 32));
		    result = prime * result + ((this.sender == null) ? 0 : this.sender.hashCode());
		    return result;
		}
	}
}