package chord;

import communication.Channel;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import util.Logger;
import util.Visualizer;

public class NodeManager {
	
	private Parameters params;
	private int maxNodes;  // Number of nodes when the system is a steady state
	private int churnRate;  // Probability [0-100] that a random node crashes each tick
	private Channel channel;
	private TrafficGenerator trafficGenerator;
	private Context<Object> context;
	
	private int firstNodeAddress;
	private int nodesCreated;
	private int nodeCount;

	public NodeManager(Parameters params, 
					   Channel channel, 
					   TrafficGenerator trafficGenerator, 
					   Context<Object> context) 
	{
		this.params = params;
		this.maxNodes = params.getInteger("maxNodes");
		this.churnRate = params.getInteger("churnRate");
		
		this.channel = channel;
		this.trafficGenerator = trafficGenerator;
		this.context = context;
		
		this.firstNodeAddress = RandomHelper.getSeed();		
		
		// Bootstrapping nodes
		ChordNode node0 = new ChordNode(firstNodeAddress, 
										params, 
										channel, 
										true);
		ChordNode node1 = new ChordNode(firstNodeAddress + 1, 
										params, 
										channel, 
										true);
		context.add(node0);
		context.add(node1);
		channel.registerNode(node0);
		channel.registerNode(node1);
		trafficGenerator.addNode(node0);
		trafficGenerator.addNode(node1);
		Visualizer.getVisualizer().addNode(node0);
		Visualizer.getVisualizer().addNode(node1);
		
		this.nodesCreated = 2;
		this.nodeCount = 2;
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 100)
	public void step() {
		if (nodeCount <= maxNodes) {
			ChordNode newNode = new ChordNode(firstNodeAddress + nodesCreated, 
											  params, 
											  channel, 
											  false);
			context.add(newNode);
			Visualizer.getVisualizer().addNode(newNode);
			channel.registerNode(newNode);
			trafficGenerator.addNode(newNode);
			nodesCreated++;
			nodeCount++;
		}
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 150)
	public void crashNodes() {
		if (nodesCreated >= maxNodes) {
			if (RandomHelper.nextIntFromTo(0, 100) < churnRate) {
				ChordNode node = (ChordNode) context.getRandomObjects(ChordNode.class, 1)
													.iterator()
													.next();
				channel.removeNode(node);
				context.remove(node);
				trafficGenerator.removeNode(node);
				Logger.getLogger().logNodeCrash(node);
				nodeCount--;
			}
		}
	}

}
