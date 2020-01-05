package util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import chord.ChordNode;
import communication.Node;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;
import repast.simphony.space.graph.RepastEdge;;

public class Visualizer {
	
	private static Visualizer instance = null;
	private static boolean initialized = false;	
	private ContinuousSpace<Object> space;
	
	private HashMap<Integer, HashSet<RepastEdge<Object>>> requests = new HashMap<>();
	private HashMap<Integer, Integer> requestsDuration = new HashMap<>();

	private Visualizer(ContinuousSpace<Object> space) {
		this.space = space;
	}
	
	public static void init(ContinuousSpace<Object> space) {
		if (!initialized) {
			instance = new Visualizer(space);
			initialized = true;
		} else {
			throw new RuntimeException("Visualizer already initialized");
		}
	}
	
	public static Visualizer getVisualizer() {
		if (initialized) {
			return instance;
		} else {
			throw new RuntimeException("Visualizer not initialized");
		}
	}
	
	public void addNode(ChordNode node) {
		double spaceSize = space.getDimensions().getHeight();
		double center = spaceSize / 2;
	    double radius = center - 2;
	    
	    int index = node.getNodeKey().getTenMostSignifDigits();	    
    	double theta = -2 * Math.PI * index / 1024 + Math.PI / 2;
        double x = center + radius * Math.cos(theta);
        double y = center + radius * Math.sin(theta);
		space.moveTo(node, x, y);
	}
	
	public void addRequest(int requestId) {
		requests.put(requestId, new HashSet<>());
		requestsDuration.put(requestId, (int) Util.getTick() + 100);
	}

	public void addEdge(int senderId, Node dest, int colour, int requestId) {
		if (requests.containsKey(requestId)) {
			Context<Object> context = ContextUtils.getContext(dest);
			
			for (Object obj : context.getObjects(ChordNode.class)) {
				if (senderId == ((ChordNode) obj).getAddress()) {
					Network<Object> net = (Network<Object>) context.getProjection("event_1 network");
					requests.get(requestId).add(net.addEdge(obj, dest, (double) colour));
					return;
				}
			}
		}

	}
	
	public void addFinger(FingerNodeVisual node, Key fingerKey) {
		double spaceSize = space.getDimensions().getHeight();
		double center = spaceSize / 2;
	    double radius = center - 2;
	    int index = fingerKey.getTenMostSignifDigits();	    
    	double theta = -2 * Math.PI * index / 1024 + Math.PI / 2;
        double x = center + radius * Math.cos(theta);
        double y = center + radius * Math.sin(theta);
		space.moveTo(node, x, y);
	}
	
	
	@ScheduledMethod(start = 1, interval = 1, priority = -100)
	public void step() {
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> net = (Network<Object>) context.getProjection("event_1 network");
		
		Iterator<Map.Entry<Integer, Integer>> iterator = requestsDuration.entrySet().iterator();
		
		while (iterator.hasNext()) {
			Map.Entry<Integer, Integer> entry = iterator.next();
			if (entry.getValue() <= Util.getTick()) {
				for (RepastEdge<Object> edge : requests.get(entry.getKey())) {
					net.removeEdge(edge);
				}
				requests.remove(entry.getKey());
				iterator.remove();
			}
		}

	}
}
