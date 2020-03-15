package chord;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.space.graph.Network;

public class ChordBuilder implements ContextBuilder<Object> {
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("chord");

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, 
				new SimpleCartesianAdder<Object>(), 
				new repast.simphony.space.continuous.StrictBorders(), 
				50, 50);
		
		ChordManager manager = new ChordManager(space, context);
		
		context.add(manager);
		return context;
	}
}
