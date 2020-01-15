package lpb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

public class GossipManager {
	
	private static ArrayList<Node> nodes = null;
	private int eventinterval = 0;
	private int gossipinterval = 0;
	private int crashinterval = 0;
	private int recoverinterval = 0;
	private int subinterval = 0;
	private int unsubinterval = 0;
	private ContinuousSpace<Object> space;
	private static Network<Object> net;
	private Parameters params;
	private static Context<Object> context;
	private static int round = 0;
	private ArrayList<Node> crashedNodes = new ArrayList<Node>();
	private static ArrayList<Node> unsubbings = null;
	private static HashMap<Integer, Integer> eventCreators = new HashMap<Integer, Integer>();
	
	public GossipManager(ContinuousSpace<Object> space, Network<Object> net, Context<Object> context) {
		params = RunEnvironment.getInstance().getParameters();
		this.eventinterval = params.getInteger("event_interval");
		this.crashinterval = params.getInteger("crash_interval");
		this.recoverinterval = params.getInteger("recover_interval");
		this.subinterval = params.getInteger("sub_interval");
		this.unsubinterval = params.getInteger("unsub_interval");
		this.gossipinterval = params.getInteger("gossip_interval");
		GossipManager.round = 0;
		GossipManager.context = context;
		this.space = space;
		this.net = net;
		int initNodeCount = (Integer)params.getValue("node_count");
		nodes = new ArrayList<Node>();
		unsubbings = new ArrayList<Node>();
		initNodes(initNodeCount);
	}
	
	private void subNewNode() {
		Node tmp = new Node(space);
		int firstNodeIndex = RandomHelper.nextIntFromTo(0, nodes.size()-1);
		tmp.addToView(nodes.get(firstNodeIndex), 0);
		net.addEdge(tmp, nodes.get(firstNodeIndex));
		nodes.add(tmp);
		context.add(tmp);
		tmp.setLocation(space.getLocation(tmp));
	}
	
	// Environment initialization function
	// Initializes the nodes
	// Generates randomized views and subs for each node
	
	private void initNodes(int nnodes) {
		int maxViewSize = (Integer)params.getValue("max_view_size");
		int maxSubSize = (Integer)params.getValue("sub_set_size");
		ArrayList<Node> initNodes = new ArrayList<Node>();
		for(int i = 0; i<nnodes;i++) {
			Node tmp = new Node(space);
			initNodes.add(tmp);
		}
		for(int i = 0;i<nnodes;i++) {
			Node tmp = initNodes.get(i);
			while(tmp.getViewSize() < Math.min(maxViewSize, initNodes.size()-1)) {
				int index = RandomHelper.nextIntFromTo(0, initNodes.size()-1);
				if(!initNodes.get(index).equals(tmp)) {
					tmp.addToView(initNodes.get(index), 0);
					net.addEdge(tmp, initNodes.get(index));
				}
			}
			while(tmp.getSubSize() < Math.min(maxSubSize, initNodes.size()-1)) {
				int index = RandomHelper.nextIntFromTo(0, initNodes.size()-1);
				if(!initNodes.get(index).equals(tmp)) {
					tmp.addSub(initNodes.get(index), 0);
				}
			}
			nodes.add(tmp);
			context.add(tmp);
			tmp.setLocation(space.getLocation(tmp));
		}
	}
	
	// Refreshes the connections between nodes based on their current views
	
	public static void refreshViewNetwork() {
		net.removeEdges();
		for(Node node : nodes) {
			if(!unsubbings.contains(node)) {
				for(Node viewNode: node.getView().keySet()) {
					if(!unsubbings.contains(viewNode)) {
						if(context.contains(viewNode))
							net.addEdge(node, viewNode);
					} else {
						if(context.contains(viewNode.getUnsubbingNode()))
							net.addEdge(node, viewNode.getUnsubbingNode());
					}
				}
			}
		}
	}
	
	public static int getRound() {
		return round;
	}
	
	// Main loop for the environment
	
	@ScheduledMethod(start = 0, interval = 1)
	public void step() {
		int ticks = (int)RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		// Unsubscriptions
		if((ticks+(unsubinterval/2)) % unsubinterval == 0) {
			Node n = null;
			do {
				n = nodes.get(RandomHelper.nextIntFromTo(0, nodes.size()-1));
			} while(crashedNodes.contains(n));
			unsubbings.add(n);
			n.unsub();
		}
		// New event generation
		if((ticks % eventinterval) == 0) {
			
			int numnodes = 0;
			while(numnodes < 5) {
				int index = RandomHelper.nextIntFromTo(0, nodes.size()-1);
				int addedEvent = nodes.get(index).createEvent();
				if(addedEvent != -1) {
					numnodes++;
					eventCreators.put(addedEvent, index);
				}
			}
		}
		// Gossip rounds
		if((ticks % gossipinterval) == 0) {
			round++;
			HashSet<Node> referencedUnsubs = new HashSet<Node>();
			HashSet<Node> toWipe = new HashSet<Node>();
			for(Node node : nodes) {
				node.spawnGossip();
			}
			nodes.removeAll(unsubbings);
			for(Node node : nodes) {
				if(!unsubbings.contains(node)) {
					for(Node viewNode: node.getView().keySet()) {
						if(unsubbings.contains(viewNode)) {
							referencedUnsubs.add(viewNode);
						}
					}
					for(Node subsNode: node.getSubs().keySet()) {
						if(unsubbings.contains(subsNode)) {
							referencedUnsubs.add(subsNode);
						}
					}
					for(Object msgToCast: context.getObjects(GossipMsg.class)) {
						GossipMsg msg = (GossipMsg)msgToCast;
						if(unsubbings.contains(msg.getTarget())) {
							referencedUnsubs.add(msg.getTarget());
						}
						for(Node sub: msg.getSubs().keySet()) {
							if(unsubbings.contains(sub)) {
								referencedUnsubs.add(sub);
							}
						}
					}
					for(Object msgToCast: context.getObjects(EventFetchMsg.class)) {
						EventFetchMsg msg = (EventFetchMsg)msgToCast;
						if(unsubbings.contains(getUnsubById(msg.getReceiverId()))) {
							referencedUnsubs.add(getUnsubById(msg.getReceiverId()));
						}
					}
				}
			}
			for(Node toRemove: unsubbings) {
				if(!referencedUnsubs.contains(toRemove)) {
					context.remove(toRemove.getUnsubbingNode());
					toWipe.add(toRemove);
				}
			}
			
			for(Node toRemove: toWipe) {
				unsubbings.remove(toRemove);
			}
		}
		// Crashes
		if((ticks % crashinterval) == 0 && ticks != 0) {
			Node n = null;
			do {
				n = nodes.get(RandomHelper.nextIntFromTo(0, nodes.size()-1));
			} while(unsubbings.contains(n) && !crashedNodes.contains(n));
			crashedNodes.add(n);
			n.crash();
		}
		// Crash recoveries
		if((ticks+recoverinterval/2) % recoverinterval == 0) {
			if(!crashedNodes.isEmpty()) {
				Node n = crashedNodes.get(RandomHelper.nextIntFromTo(0, crashedNodes.size()-1));
				crashedNodes.remove(n);
				n.recover();
			}
			
		}
		// New subscribers
		if(ticks % subinterval == 0 && ticks != 0) {
			subNewNode();
		}
	}
	
	public static int getEventCreator(int eventid) {
		return eventCreators.get(eventid);
	}
	
	public static Node getRandomActiveNode(int id) {
		Node node = null;
		do{
			node = nodes.get(RandomHelper.nextIntFromTo(0,nodes.size()-1));
		} while(unsubbings.contains(node) || (id != -1 && id == node.getNodeId()));
		// selects a random node until the id is different than the requester one (if set) and the node is not unsubbing
		return node;
	}

	public static NdPoint getNodePosition(ContinuousSpace<Object> space, int nodeId) {
		for(Node n : nodes) {
			if(nodeId == n.getNodeId())
				return space.getLocation(n);
		}
		return null;
	}
	
	private static Node getUnsubById(int id) {
		for(Node n : unsubbings) {
			if(n.getNodeId() == id) {
				return n;
			}
		}
		return null;
	}
}
