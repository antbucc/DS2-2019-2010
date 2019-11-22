package lpbcast;

import java.util.HashSet;

import communication.Channel;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.WrapAroundBorders;
import util.Logger;
import util.PersistencyDetector;
import util.Visualizer;

public class LPBCastBuilder implements ContextBuilder<Object> {
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context
	 * .Context)
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		context.setId("lpbcast");
		Logger.getLogger().setContext(context);
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new WrapAroundBorders(), 50, 50);

		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(
				"event_1 network", context, true);
		netBuilder.buildNetwork();	
			
		Channel channel = new Channel(params.getInteger("max_channel_latency"), 
									  params.getInteger("probability_network_fail"));
		context.add(channel);
		
		int maxNodes = params.getInteger("max_nodes");
		int minNodes = params.getInteger("min_nodes");
		int initialNodes = params.getInteger("initial_nodes");
		
		Visualizer.init(space, maxNodes);
		
		context.add(PersistencyDetector.getInstance());
		
		NodeManager nodeManager = new NodeManager(context, channel, params);
		context.add(nodeManager);
		
		HashSet<Integer> initialView = new HashSet<>();
		for (int i = 0; i < initialNodes; i++) {
			LPBCastNode newNode = new LPBCastNode(i, 
												  (maxNodes + minNodes) / 2, 
												  channel, params);
			context.add(newNode);
			initialView.add(newNode.getId());
			channel.registerNode(newNode);
			Visualizer.getVisualizer().addNode(newNode);
			
			Logger.getLogger().logNodeCreation(newNode);
		}
		
		for (Object obj : context.getObjects(LPBCastNode.class)) {
			((LPBCastNode) obj).setView(new HashSet<>(initialView));
		}

		return context;
	}
}
