package lpbcast;



import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;



public class Visualization {
	public enum EdgeType {FANOUT, VIEW, RETRIEVE_REQUEST, RETRIEVE_REPLY} 
	public Set<Link> currentLinks; // Set of links to print at the current step
	public Event currentVisEvent; // Current event the system has to consider
	public Double currentVisEventTick;  // tick in which the currentVisEvent is set
	public Network<Object> network; 
	public Context<Object> context;
	public boolean newEvent;
	public int currentColorIndex;
	public Color[] eventColors;

	public Visualization(Network<Object> network, Context<Object> context) {
		this.network = network;
		this.currentLinks = ConcurrentHashMap.newKeySet();
		this.currentVisEvent = new Event(new EventId(UUID.randomUUID(), -1), 0); 
		this.currentVisEventTick = 0 - Double.MAX_VALUE;
		this.context = context;
		this.newEvent = false;
		this.currentColorIndex = 0;
		this.eventColors = new Color[]{Color.GREEN, Color.yellow, Color.CYAN, Color.magenta};
		
	}
	
	/**
	 * Gets the current tick of the simulation
	 * @return the current tick
	 */
	public double getCurrentTick() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}
	
	public Color getNextColor() {
		currentColorIndex ++;
		if(currentColorIndex == eventColors.length) {
			currentColorIndex = 0;
		}
		return this.eventColors[currentColorIndex];
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void startStep() {
		// Remove all the current edges 
		this.network.removeEdges();
		//if EVENT_VISUAL_TIME timeout is expired, a new event has to be considered
		if(this.getCurrentTick() - this.currentVisEventTick > Configuration.EVENT_VISUAL_TIME) {
			// set currentVisEvent to an unexisting event -> reset the visualization
			this.currentVisEvent = new Event(new EventId(UUID.randomUUID(), -1), 0); 
			//Send to each process the new event to consider
			Iterator<Object> it = context.getAgentLayer(Object.class).iterator();
			while(it.hasNext()) {
				Object agent = it.next();
				if(agent instanceof Process) {
					((Process)agent).setCurrentVisualEvent(this.currentVisEvent);
				}
			}
			newEvent = false;
		}
		// Update edges
		for(Link entry : this.currentLinks) {
			try {
				if(!entry.target.isUnsubscribed)
					this.network.addEdge(entry.source, entry.target, Double.valueOf(entry.type.ordinal()));
			} catch(Exception e) {
				// some process is removed from the context, I can not show that edge
			}
		}

		// Empty the list of links and the list of new Events at every step
		this.currentLinks.clear();
	}
	public void addViewLinks(Process source, HashMap<Integer, Integer> view) {
		for(Map.Entry<Integer, Integer> entry : view.entrySet()) {
			this.addLink(source, Process.getProcessById(entry.getKey(), ContextUtils.getContext(this)), EdgeType.VIEW);
		}
	}

	public void addLink(Process source, Process target, EdgeType type) {
		this.currentLinks.add(new Link(source, target, type));
	}
	
	public void notifyNewEvent(Event event) {
		// Check if the event to consider has to be changed
		if(this.getCurrentTick() - this.currentVisEventTick > Configuration.EVENT_VISUAL_TIME) {
			// the first event delivered after the timeout is considered
			this.currentVisEventTick = this.getCurrentTick();
			this.currentVisEvent = event;
			this.newEvent = true;
			//Generate a random color for the new event
			Color current = getNextColor();
			NodeStyle.deliveredNodeColor = current;
			Custom2DEdge.gossipEventEdgeColor = current;
			
			
		} 
	}
		
	
	class Link {
		public Process source;
		public Process target;
		public Visualization.EdgeType type;
		
		public Link(Process source, Process target, Visualization.EdgeType type) {
			this.source = source;
			this.target = target;
			this.type = type;
		}
	}
	
}
