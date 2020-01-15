package chord;

import java.util.Random;
import java.util.TreeMap;

import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.WrapAroundBorders;

public class NodeContext extends DefaultContext<Node> {
	/**
	 * Space of the simulation.
	 */
	public ContinuousSpace<Node> space;

	/**
	 * Map that links each ID to the corresponding node. The keys are sorted in
	 * ascending order.
	 */
	public TreeMap<Integer, Node> idMap = new TreeMap<Integer, Node>();

	/**
	 * Router that handles the communication.
	 */
	public Router router;
	
	// ------------------------------------------------------------------------
	// PARAMETERS FOR THE SIMULATION

	/**
	 * Size of the space of the simulator.
	 */
	private final int SPACE_SIZE = 100;

	/**
	 * Total number of nodes.
	 */
	private final int NUMBER_NODES;

	/**
	 * Define the ID space as 2^ID_SIZE.
	 */
	private final int ID_SIZE;

	/**
	 * Constructor of the context for the nodes.
	 */
	public NodeContext() {
		// Set the context ID.
		super("chord nodes");

		// Get the simulation parameters.
		Parameters p = RunEnvironment.getInstance().getParameters();
		NUMBER_NODES = p.getInteger("NUMBER_NODES");
		ID_SIZE = p.getInteger("ID_SIZE");

		// Build the space of the simulation.
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		space = spaceFactory.createContinuousSpace("space", this, new RandomCartesianAdder<Node>(),
				new WrapAroundBorders(), SPACE_SIZE, SPACE_SIZE);

		// Initialize the network.
		NetworkBuilder<Node> netBuilder = new NetworkBuilder<Node>("connections network", this, true);
		netBuilder.buildNetwork();

		// Check if the key space is large enough.
		if (NUMBER_NODES > Math.pow(2, ID_SIZE)) {
			System.err.println("The ID size is too small, please select a larger value");
		} else {
			// Initialize the router.
			router = new Router(this);

			// Add the nodes.
			for (int i = 0; i < NUMBER_NODES; i++) {
				addNode();
			}

			// Initialize each node with its correct successor.
			initNodes();
		}
	}

	/**
	 * Adds a new node to the context.
	 * 
	 * @return the new node.
	 */
	public Node addNode() {
		int ringSize = (int) Math.pow(2, ID_SIZE);
		Random random = new Random();

		// Enforce the ID to be unique, but randomly chosen.
		int id = random.nextInt(ringSize);

		while (idMap.containsKey(id)) {
			id = random.nextInt(ringSize);
		}

		Node node = new Node(this, id);

		// Add the node to the context and to the ID map.
		this.add(node);
		idMap.put(node.id, node);

		// Move the node to the correct position.
		double angle = (360.0 / ringSize) * id;
		space.moveTo(node, 50 + 40 * Math.sin(Math.toRadians(angle)), 50 + 40 * Math.cos(Math.toRadians(angle)));

		return node;
	}
	
	/**
	 * Removes a node from the context.
	 * 
	 * @param the ID of the node to remove.
	 */
	public void removeNode(int nodeId) {
		// Remove the node from the context and the ID map.
		remove(idMap.get(nodeId));
		idMap.remove(nodeId);
	}

	/**
	 * Initializes the existing nodes with the correct pointers.
	 */
	public void initNodes() {
		// Initialize each node with its correct successor.
		idMap.keySet().iterator().forEachRemaining(key -> {
			idMap.get(key).initializeNode(idMap.navigableKeySet());
		});
	}
}
