package lpbcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import communication.Channel;
import communication.Message;
import communication.Node;
import lpbcast.messages.EventRequest;
import lpbcast.messages.EventResponse;
import lpbcast.messages.Gossip;
import lpbcast.messages.Unsubscription;
import util.Coordinates;
import util.Logger;
import util.PersistencyDetector;
import util.Visualizer;

import static util.Util.extractRandom;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;


public class LPBCastNode extends Node {
	
	private static int eventsCount = 0;
    private static Random random = new Random();
    
    // Internal state of the node
	private int currentRound = 0;
	private Coordinates position;
    private HashSet<Integer> view;
    private HashMap<Integer, Integer> viewFrequencies = new HashMap<>();  // Frequency based membership purging
	private HashSet<Integer> subs = new HashSet<>();
	private HashMap<Integer, Integer> subsFrequencies = new HashMap<>();  // Frequency based membership purging
	private HashSet<Integer> unsubs = new HashSet<>();
	private HashMap<EventId, Event> events = new HashMap<>();
	private LinkedHashSet<EventId> eventIds = new LinkedHashSet<>();
	private ArrayList<Element> retrieveBuffer = new ArrayList<>();
    
	private int probNewEvent;
    private int roundTicks;
    private int maxGossipTargets;
    private int maxUnsubs;
    private int maxView;
    private int maxSubs;
    private int maxEventIds;
    private int maxEvents;
    
    // Optimizations
    private boolean optimizationEvents;  // Age based event purging
    private int longAgo;
    private boolean optimizationSubs;  // Frequency based membership purging
    private double kAvg;
    
    // Event broadcasting visualization
    private int greenEvent;
    private int redEvent;
    private int orangeEvent;
    
	public LPBCastNode(int id, int avgNodes, Channel channel, Parameters params) {
		super(id, channel);
		this.probNewEvent = (int) Math.floor(avgNodes * 3 / 125.0);
		this.maxView = params.getInteger("max_view");
		this.maxSubs = params.getInteger("max_subs");
		this.maxUnsubs = params.getInteger("max_unsubs");
		this.maxEvents = params.getInteger("max_events");
		this.maxEventIds = params.getInteger("max_eventids");
		this.roundTicks = params.getInteger("round_ticks");
		this.maxGossipTargets = params.getInteger("max_gossip_targets");
		this.optimizationEvents = params.getBoolean("optimization1");
		this.longAgo = params.getInteger("long_ago");
		this.optimizationSubs = params.getBoolean("optimization2");
		this.kAvg = params.getDouble("k_avg");
		this.greenEvent = params.getInteger("green_event");
		this.redEvent = params.getInteger("red_event");
		this.orangeEvent = params.getInteger("orange_event");
	}
	
    public void setView(HashSet<Integer> view) {
    	this.view = view;
    	// If the node itself is included in the view, remove it
    	this.view.remove(this.id);
    	// Truncate `view` if it exceeds the maximum size
		while (this.view.size() > maxView) {
			int target = extractRandom(view);
			view.remove(target);
		}
		// Update viewFrequency (used by frequency based membership purging)
    	for (int viewElement : this.view) {
    		viewFrequencies.put(viewElement, 1);
    	}
    }
    
    public void setPosition(Coordinates position) {
    	this.position = position;
    }
    
    public Coordinates getPosition() {
    	return position;
    }

	@ScheduledMethod(start = 1, interval = 1, priority = 10)
	public void step() {
		
		PersistencyDetector.getInstance().updatePersistency(events);
		
		if (random.nextInt(100) < probNewEvent && currentRound > 25) {
			generateEvent();
		}
		retrieveEvents();
		if (currentRound % roundTicks == this.id % roundTicks) {
			sendGossips();
		}

		currentRound++;
	}
	
	@Override
	public void receiveMessage(Message msg) {
		if (msg instanceof Gossip) {
			receiveGossip((Gossip) msg);
		} else if (msg instanceof EventRequest) {
			receiveEventRequest((EventRequest) msg);
		} else if (msg instanceof EventResponse) {
			receiveEventResponse((EventResponse) msg);
		} else if (msg instanceof Unsubscription) {
			receiveUnsubscription((Unsubscription) msg);
		} else {
			throw new UnsupportedOperationException("Message unknown");
		}
	}
	
	private void sendGossips() {
		
		ArrayList<Integer> viewList = new ArrayList<>(view);
		Collections.shuffle(viewList);
		Set<Integer> targets = viewList.stream()
									   .limit(maxGossipTargets)
									   .collect(Collectors.toSet());
		
		for (EventId eventId : events.keySet()) {
			events.get(eventId).increaseAge();
		}
		
		for(int targetId: targets) {
			HashMap<EventId, Event> eventsToSend = new HashMap<>();
			for (EventId eventId : events.keySet()) {
				eventsToSend.put(eventId, new Event(events.get(eventId)));
			}
			Gossip gossip = new Gossip(this.id, targetId,
									   new HashSet<>(subs),
									   new HashSet<>(unsubs), 
									   eventsToSend,
									   new HashSet<>(eventIds));
			channel.sendMessage(gossip);
		}
		
		events.clear();
	}
	
	public void sendUnsub() {
		ArrayList<Integer> viewList = new ArrayList<>(view);
		Collections.shuffle(viewList);
		Set<Integer> targets = viewList.stream()
									   .limit(maxGossipTargets)
									   .collect(Collectors.toSet());
		
		for(int targetId: targets) {
			Unsubscription unsub = new Unsubscription(this.id, targetId);
			channel.sendMessage(unsub);
		}
	}
	
	private void deliverEvent(Event event, int senderId) {
		if (event.getId().getSourceId() != this.id) {			
			Logger.getLogger().logEventDelivery(event, this.id, senderId);
			
			if (event.getId().getId() % 1000 == greenEvent) {
				Visualizer.getVisualizer().addEdge(senderId, this, 0);
			} else if (event.getId().getId() % 1000 == redEvent){
				Visualizer.getVisualizer().addEdge(senderId, this, 1);
			} else if (event.getId().getId() % 1000 == orangeEvent) {
				Visualizer.getVisualizer().addEdge(senderId, this, 2);
			}
		}		
	}
	
	private void generateEvent() {
		EventId newEventId = new EventId(eventsCount, this.id);
		Event newEvent = new Event(newEventId);
		events.put(newEventId, newEvent);
		eventIds.add(newEventId);
		removeOldestNotifications();
		eventsCount++;
		
		PersistencyDetector.getInstance().addEvent(newEventId);
		Logger.getLogger().logEventCreation(newEvent);
	}
	
	private void receiveGossip(Gossip gossip) {
		
		// Phase 1
		// Update `view`, `subs`, `unsubs` with unsubscriptions
		for (int unsub: gossip.getUnsubs()) {
			view.remove(unsub);
			viewFrequencies.remove(unsub);
			subs.remove(unsub);
			subsFrequencies.remove(unsub);
			unsubs.add(unsub);
		}
		
		// Truncate `unsubs` if it exceeds the maximum size
		while (unsubs.size() > maxUnsubs) {
			unsubs.remove(extractRandom(unsubs));
		}
		
		// Phase 2
		// Update `view`, `subs` with new subscriptions
		for (int newSubId: gossip.getSubs()) {
			
			if (newSubId != this.id) {
				if (view.contains(newSubId)) {
					viewFrequencies.replace(newSubId, viewFrequencies.get(newSubId) + 1);
				} else {
					view.add(newSubId);
					viewFrequencies.put(newSubId, 1);
				}
				
				if (subs.contains(newSubId)) {
					subsFrequencies.replace(newSubId, subsFrequencies.get(newSubId) + 1);
				} else {
					subs.add(newSubId);
					subsFrequencies.put(newSubId, 1);
				}
			}
			
			if (optimizationSubs) {
				// Truncate `view` if it exceeds the maximum size
				while (view.size() > maxView) {
					int target = selectProcess(viewFrequencies);
					view.remove(target);
					viewFrequencies.remove(target);
					if (!view.contains(target)) {
						subs.add(target);
						subsFrequencies.put(target, 1);
					}
				}
				// Truncate `subs` if it exceeds the maximum size
				while (subs.size() > maxSubs) {
					int target = selectProcess(subsFrequencies);
					subs.remove(target);
					subsFrequencies.remove(target);
				}
				
			} else {
				// Truncate `view` if it exceeds the maximum size
				while (view.size() > maxView) {
					int target = extractRandom(view);
					view.remove(target);
					viewFrequencies.remove(target);
					subs.add(target);
					subsFrequencies.put(target, 1);
				}
				
				// Truncate `subs` if it exceeds the maximum size
				while (subs.size() > maxSubs) {
					int target = extractRandom(subs);
					subs.remove(target);
					subsFrequencies.remove(target);
				}
			}
			
		}
		
		// Phase 3
		// Update `events`, `eventIds` with new events 
		for (Event e: gossip.getEvents().values()) {
			if (events.containsKey(e.getId()) && e.getAge() > events.get(e.getId()).getAge()) {
				events.put(e.getId(), e);
			}
			if (!eventIds.contains(e.getId())) {
				events.put(e.getId(), e);
				deliverEvent(e, gossip.getSenderId());
				eventIds.add(e.getId());
			}
		}
		
		// Retrieve missing events
		for (EventId eventId: gossip.getEventIds()) {
			if (!eventIds.contains(eventId) && eventId.getSourceId() != this.id) {
				retrieveBuffer.add(new Element(eventId,
											   currentRound,
											   gossip.getSenderId()));
			}
		}
		
		// Truncate `eventIds` if it exceeds the maximum size
		while (eventIds.size() > maxEventIds) {
			EventId eventToRemove = eventIds.iterator().next();
			eventIds.remove(eventToRemove);
			
			if (eventToRemove.getId() % 1000 == greenEvent) {
				Visualizer.getVisualizer().removeEdge(this, 0);
			} else if (eventToRemove.getId() % 1000 == redEvent){
				Visualizer.getVisualizer().removeEdge(this, 1);
			} else if (eventToRemove.getId() % 1000 == orangeEvent) {
				Visualizer.getVisualizer().removeEdge(this, 2);
			}
		}
		
		// Truncate `events` if it exceeds the maximum size
		removeOldestNotifications();
	}
	
	private void receiveEventRequest(EventRequest eventRequest) {
		if (events.containsKey(eventRequest.getEventId())) {
			// If the node has the request event in the `events` buffer,
			// send it to the requesting node
			channel.sendMessage(new EventResponse(this.id, eventRequest.getSenderId(),
												  events.get(eventRequest.getEventId())));
		}
	}
	
	private void receiveEventResponse(EventResponse eventResponse) {
		Event event = eventResponse.getEvent();
		events.put(event.getId(), event);
		deliverEvent(event, eventResponse.getSenderId());
		eventIds.add(event.getId());
	}
	
	private void receiveUnsubscription(Unsubscription unsub) {
		view.remove(unsub.getSenderId());
		subs.remove(unsub.getSenderId());
		unsubs.add(unsub.getSenderId());
	}
	
	private void retrieveEvents() {
		ArrayList<Element> elementsToRemove = new ArrayList<>();
		for (Element element: retrieveBuffer) {
			if (currentRound - element.getRound() > roundTicks) {
				if (!eventIds.contains(element.getEventId())) {
					
					// Request the event from the sender of the gossip
					if (element.getRoundFirstRequest() == -1) {
						channel.sendMessage(new EventRequest(
								this.id,
								element.getGossipSenderId(),
								element.getEventId()));
						element.setRoundFirstRequest(currentRound);
					
					// After R rounds from the first request, request the event
					// from a random node
					} else if (currentRound - element.getRoundFirstRequest() > roundTicks
							   && element.getRoundSecondRequest() == -1) {
						if (view.size() > 0) {
							channel.sendMessage(new EventRequest(
									this.id,
									extractRandom(view),
									element.getEventId()));						
							element.setRoundSecondRequest(currentRound);	
						}
						
					// After R rounds from the second request, request the event
					// from the source of the event
					} else if (currentRound - element.getRoundFirstRequest() > roundTicks
							   && currentRound - element.getRoundSecondRequest() > roundTicks
							   && element.getRoundThirdRequest() == -1) {
						channel.sendMessage(new EventRequest(
								this.id,
								element.getEventId().getSourceId(),
								element.getEventId()));
						element.setRoundThirdRequest(currentRound);
					
					// After R rounds from the third request, discard the element
					// from the retrieve buffer
					} else if (currentRound - element.getRoundFirstRequest() > roundTicks
							   && currentRound - element.getRoundSecondRequest() > roundTicks
							   && currentRound - element.getRoundThirdRequest() > roundTicks) {
						elementsToRemove.add(element);
					}
				
				// The node has received the event, remove the element from the
				// retrieve buffer
				} else {
					elementsToRemove.add(element);
				}
			}
		}
		retrieveBuffer.removeAll(elementsToRemove);
	}
	
	private void removeOldestNotifications() {
		
		if (optimizationEvents) {
			if (events.size() > maxEvents) {
				
				HashSet<EventId> eventsToRemove = new HashSet<>();
				
				outerloop:
				for (EventId eventId1 : events.keySet()) {
					for (EventId eventId2 : events.keySet()) {
						if (eventId1.getSourceId() == eventId2.getSourceId() 
								&& eventId1.getId() - eventId2.getId() > longAgo) {
							eventsToRemove.add(eventId2);
							if (events.size() - eventsToRemove.size() > maxEvents) {
								break outerloop;
							}
						}
					}
				}
			
				for (EventId eventId : eventsToRemove) {
					events.remove(eventId);
				}
			}
			
			while (events.size() > maxEvents) {
				
				int currentMaxAge = 0;
				EventId currentEventToRemove = null;
				
				for (Event event : events.values()) {
					if (event.getAge() >= currentMaxAge) {
						currentMaxAge = event.getAge();
						currentEventToRemove = event.getId();
					}
				}
				
				events.remove(currentEventToRemove);
			}
		} else {
			while (events.size() > maxEvents) {
				events.remove(extractRandom(events.keySet()));
			}
		}
		
	}
	
	private int selectProcess(HashMap<Integer, Integer> frequencies) {
		double avg = frequencies.values()
								.stream()
								.mapToInt(Integer::intValue)  // IntStream
								.average()
								.getAsDouble();
		while (true) {
			int target = extractRandom(frequencies.keySet());
			if (frequencies.get(target) > kAvg * avg) {
				return target;
			} else {
				frequencies.replace(target, frequencies.get(target) + 1);
			}
		}
	}

}
