package chord;

import java.util.Random;

/**
 * Class that simulates the presence of a router in the message exchange between
 * nodes.
 */
public class Router {
	/**
	 * The context of the simulation.
	 */
	private NodeContext context;

	/**
	 * Probability that the message is sent by the router.
	 */
	private final double PROBABILITY_SEND = 1;

	/**
	 * Constructor of the router.
	 * 
	 * @param context the context of the simulation.
	 */
	public Router(NodeContext context) {
		this.context = context;
	}

	/**
	 * Sends a message to the specified node.
	 * 
	 * @param message the message to send.
	 * @param targetId  the ID of the target node.
	 */
	public void send(Message message, int targetId) {
		// Get the node with the specified ID.
		Node target = context.idMap.get(targetId);

		// Check if the message will be sent.
		if ((new Random()).nextDouble() < PROBABILITY_SEND) {
			// Check whether the target is available.
			if (target != null) {
				// Send the message to the target.
				target.receive(message);
			} else if (targetId >= 0) {
				// Notify the sender of the target's absence.
				context.idMap.get(message.sender).timeout(message, targetId);
			}
		}
	}
}
