package util;

import java.util.HashSet;

import lpbcast.LPBCastNode;
import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;

import static util.Util.extractRandom;

public class Visualizer {
	
	private static Visualizer instance = null;
	private static boolean initialized = false;
	
	private ContinuousSpace<Object> space;
	private HashSet<Coordinates> freePositions = new HashSet<>();
	private HashSet<Coordinates> occupiedPositions = new HashSet<>();
	
	private Visualizer(ContinuousSpace<Object> space, int maxNodes) {
		this.space = space;
		double spaceSize = space.getDimensions().getHeight();
		double center = spaceSize / 2;
	    double radius = center - 2;
	    for (int i = 0; i < maxNodes; i++) {
	    	double theta = 2 * Math.PI * i / maxNodes;
	        double x = center + radius * Math.cos(theta);
	        double y = center + radius * Math.sin(theta);
	        Coordinates coordinate = new Coordinates(x, y);
	        freePositions.add(coordinate);
	    }
	}
	
	public static void init(ContinuousSpace<Object> space, int maxNodes) {
		if (!initialized) {
			instance = new Visualizer(space, maxNodes);
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
	
	public void addNode(LPBCastNode node) {
		Coordinates position = extractRandom(freePositions);
		freePositions.remove(position);
		occupiedPositions.add(position);
		node.setPosition(position);
		space.moveTo(node, position.getX(), position.getY());
	}
	
	public void removeNode(LPBCastNode node) {
		Coordinates position = node.getPosition();
		freePositions.add(position);
		occupiedPositions.remove(position);
		
		Context<Object> context = ContextUtils.getContext(node);
		Network<Object> net = (Network<Object>) context.getProjection("event_1 network");
		for (RepastEdge<Object> edge : net.getEdges(node)) {
			net.removeEdge(edge);
		}
	}
	
	public void addEdge(int senderId, LPBCastNode dest, int colour) {
		Context<Object> context = ContextUtils.getContext(dest);
		
		for (Object obj : context.getObjects(LPBCastNode.class)) {
			if (senderId == ((LPBCastNode) obj).getId()) {
				Network<Object> net = (Network<Object>) context.getProjection("event_1 network");
				net.addEdge(obj, dest, (double) colour);
				return;
			}
		}
	}
	
	public void removeEdge(LPBCastNode node, int colour) {
		Context<Object> context = ContextUtils.getContext(node);
		Network<Object> net = (Network<Object>) context.getProjection("event_1 network");
		
		for (RepastEdge<Object> edge : net.getEdges(node)) {
			if (edge.getWeight() == (double) colour) {
				net.removeEdge(edge);
			}
			
		}
	}
	
}
