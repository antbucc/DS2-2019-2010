package lpbcast;

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

/**
 * Builder of the simulation.
 * 
 * It is in charge of creating the networks and the nodes that will be used during the simulation.
 */
public class LpbcastBuilder implements ContextBuilder<Object> {
	private int NUMBER_NODES;
	private final int SPACE_SIZE = 100;

	
	@Override
	public Context<Object> build(Context<Object> context) {
		// Set the ID of the context.
		context.setId("lpbcast");

		// Build the space of the simulation.
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context,
				new RandomCartesianAdder<Object>(), new WrapAroundBorders(), SPACE_SIZE, SPACE_SIZE);

		// Build the networks.
		NetworkBuilder<Object> viewNetBuilder = new NetworkBuilder<Object>("view network", context, true);
		viewNetBuilder.buildNetwork();
		NetworkBuilder<Object> subsNetBuilder = new NetworkBuilder<Object>("subs network", context, true);
		subsNetBuilder.buildNetwork();
		NetworkBuilder<Object> unsubsNetBuilder = new NetworkBuilder<Object>("unsubs network", context, true);
		unsubsNetBuilder.buildNetwork();
		NetworkBuilder<Object> sentMessagesNetBuilder = new NetworkBuilder<Object>("sentMessages network", context,
				true);
		sentMessagesNetBuilder.buildNetwork();
		
		// Create the router.
		Router router = new Router();

		// Add nodes to the space.
		Parameters p = RunEnvironment.getInstance().getParameters();
		NUMBER_NODES = p.getInteger("NUMBER_NODES");
		for (int i = 0; i < NUMBER_NODES; i++) {
			context.add(new Node(space, router, i));
		}
		
		// Initialize the router.
		router.initializeRouter(space);

		// Initialize the view of each node once it has been added to the space.
		for (Object el : space.getObjects()) {
			Node node = (Node) el;
			node.initializeView();
		}

		return context;
	}
}
