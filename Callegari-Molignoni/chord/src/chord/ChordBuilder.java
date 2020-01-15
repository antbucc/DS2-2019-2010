package chord;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;

/**
 * Builder of the simulation.
 * 
 * It is in charge of creating the networks and the nodes that will be used
 * during the simulation.
 */
public class ChordBuilder implements ContextBuilder<Object> {
	/**
	 * Creates the nodes and the network, then initializes the simulation.
	 * 
	 * @param context the context of the simulation.
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		// Clear existing context information.
		context.clear();

		// Set the ID of the context.
		context.setId("chord");

		// Create a sub-context for the nodes.
		NodeContext nodeContext = new NodeContext();
		context.addSubContext(nodeContext);
		context.add(nodeContext);

		// Add the traffic generator to the context.
		context.add(new TrafficGenerator(nodeContext));

		return context;
	}
}
