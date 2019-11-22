package lpbcast;

import java.util.HashMap;
import java.util.Random;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;

/**
 * Class that simulates the presence of a router in the message exchange between nodes.
 */
public class Router {
	/**
	 * Probability that the message is sent by the router.
	 */
	private double PROBABILITY_SEND;

	/**
	 * Nodes of the system.
	 */
	private HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

	/**
	 * Object for the generation of random values.
	 */
	private Random random = new Random();

	
	/**
	 * Constructor of Router.
	 */
	public Router() {
		// Get the parameters from the run environment.
		Parameters p = RunEnvironment.getInstance().getParameters();
		PROBABILITY_SEND = p.getDouble("PROBABILITY_SEND");
	}

	/**
	 * Initializes the nodes of the router.
	 * 
	 * @param space the space of the nodes
	 */
	public void initializeRouter(ContinuousSpace<Object> space) {
		// Iterate through the objects of the space.
		space.getObjects().iterator().forEachRemaining(item -> {
			Node node = (Node) item;

			// Add the node, using its ID as key.
			nodes.put(node.id, node);
		});
	}

	/**
	 * Sends a message to the specified node.
	 * 
	 * @param message the message to send
	 * @param nodeId  the ID of the target node
	 */
	public void send(Message message, int nodeId) {
		// Get the node with the specified ID.
		Node target = nodes.get(nodeId);

		// Check if the message will be sent.
		double rand = random.nextDouble();
		if (rand < PROBABILITY_SEND) {
			// Send the message to the target node.
			target.receive(message);
		}
	}
}
