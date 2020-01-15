package lpb;

import java.util.ArrayList;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;

public class LPBBuilder implements ContextBuilder<Object> {
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("lpb");
		
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("view network", context, true);
		netBuilder.buildNetwork();
		Network<Object> net = (Network<Object>)context.getProjection("view network");
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, 
				new RandomCartesianAdder<Object>(), 
				new repast.simphony.space.continuous.StrictBorders(), 
				50, 50);
		
		GossipManager manager = new GossipManager(space, net, context);
		
		context.add(manager);
		
		return context;
	}

}
