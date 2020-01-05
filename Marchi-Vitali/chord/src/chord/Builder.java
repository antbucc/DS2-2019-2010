package chord;

import communication.Channel;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.WrapAroundBorders;
import util.Logger;
import util.Visualizer;


public class Builder implements ContextBuilder<Object> {
	
	@Override
	public Context<Object> build(Context<Object> context) {
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new WrapAroundBorders(), 50, 50);
		
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(
				"event_1 network", context, true);
		netBuilder.buildNetwork();

		Channel channel = new Channel(3);
		context.add(channel);
		
		TrafficGenerator trafficGenerator = new TrafficGenerator();
		context.add(trafficGenerator);
		
		context.add(Logger.getLogger());
		
		Visualizer.init(space);
		context.add(Visualizer.getVisualizer());
		
		NodeManager nodeManger = new NodeManager(
									RunEnvironment.getInstance().getParameters(),
									channel, 
									trafficGenerator, 
									context
								);
		context.add(nodeManger);
		
		return context;
	}
	
}
