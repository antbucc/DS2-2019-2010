package lpbcast;

import java.util.HashSet;
import java.util.Random;

import communication.Channel;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import util.Logger;
import util.Visualizer;

public class NodeManager {
	
	private static Random random = new Random();
	
	private Context<Object> context;
	private Channel channel;
	private Parameters params;
	private int nodeCount;
	private int nodesCreated;
	private int maxNodes;
	private int minNodes;
	private int probCreateNode = 5;
	private int probRemoveNode = 5;
	private int probCrash = 50;
	
	public NodeManager(Context<Object> context, Channel channel, Parameters params) {
		this.context = context;
		this.channel = channel;
		this.params = params;
		this.nodeCount = params.getInteger("initial_nodes");
		this.nodesCreated = nodeCount;
		this.maxNodes = params.getInteger("max_nodes");
		this.minNodes = params.getInteger("min_nodes");
		this.probCreateNode = params.getInteger("probability_create_node");
		this.probRemoveNode = params.getInteger("probability_remove_node");
		this.probCrash = params.getInteger("probability_crash");
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 150)
	public void step() {
		if (nodeCount < maxNodes - 1) {
			if (random.nextInt(100) < probCreateNode) {
				LPBCastNode node = (LPBCastNode) context.getRandomObjects(LPBCastNode.class, 1).iterator().next();
				LPBCastNode newNode = new LPBCastNode(nodesCreated++, 
													  (maxNodes + minNodes) / 2,
													  channel, params);
				context.add(newNode);
				channel.registerNode(newNode);
				Visualizer.getVisualizer().addNode(newNode);
				nodeCount++;
				
				HashSet<Integer> view = new HashSet<>();
				view.add(node.getId());
				newNode.setView(view);
				
				Logger.getLogger().logNodeCreation(newNode);
			}
		}
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 150)
	public void crashNodes() {
		if (nodeCount > minNodes) {
			if (random.nextInt(100) < probRemoveNode) {
				LPBCastNode node = (LPBCastNode) context.getRandomObjects(LPBCastNode.class, 1).iterator().next();
				
				if (random.nextInt(100) > probCrash) {
					node.sendUnsub();
				}
				
				channel.removeNode(node);
				Visualizer.getVisualizer().removeNode(node);
				context.remove(node);
				nodeCount--;
				
				Logger.getLogger().logNodeDelete(node);
			}
		}
	}
	
}
