package chord;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;

/**
 * Class that iteratively performs lookups on random nodes with random keys.
 */
public class TrafficGenerator {
	/**
	 * Context of the simulation.
	 */
	private NodeContext context;

	/**
	 * Current round number.
	 */
	private int nRound = 0;

	// ------------------------------------------------------------------------
	// PARAMETERS FOR THE SIMULATION

	/**
	 * Size of the ID space.
	 */
	private final int ID_SIZE;

	/**
	 * Number of rounds to wait between two consecutive lookups.
	 */
	private final int NUMBER_WAIT_ROUNDS;

	/**
	 * Probability that a new node joins at each round.
	 */
	private final double PROBABILITY_JOIN;

	/**
	 * Probability that an existing node leaves at each round.
	 */
	private final double PROBABILITY_LEAVE;

	/**
	 * Probability that an existing node fails at each round.
	 */
	private final double PROBABILITY_FAIL;

	/**
	 * Constructor of the traffic generator which just saves some values.
	 * 
	 * @param context the context of the simulation.
	 */
	public TrafficGenerator(NodeContext context) {
		this.context = context;

		// Get the simulation parameters.
		Parameters p = RunEnvironment.getInstance().getParameters();
		ID_SIZE = p.getInteger("ID_SIZE");
		NUMBER_WAIT_ROUNDS = p.getInteger("NUMBER_WAIT_ROUNDS");
		PROBABILITY_JOIN = p.getDouble("PROBABILITY_JOIN");
		PROBABILITY_LEAVE = p.getDouble("PROBABILITY_LEAVE");
		PROBABILITY_FAIL = p.getDouble("PROBABILITY_FAIL");
	}

	/**
	 * Selects a random node from the Chord ring.
	 * 
	 * @return the selected node or null.
	 */
	private Node selectRandomNode() {
		Iterable<Node> objSpace = context.space.getObjects();
		List<Node> nodes = new LinkedList<Node>();

		// Create a list of nodes and shuffle it.
		objSpace.forEach(nodes::add);
		Collections.shuffle(nodes);

		Iterator<Node> itr = nodes.iterator();
		boolean found = false;
		Node curr = null;

		while (itr.hasNext() && !found) {
			curr = itr.next();

			// Check whether the node is valid.
			if (curr != null && curr.ready) {
				found = true;
			}
		}

		return curr;
	}

	/**
	 * Method called at each tick of simulation. Every "NUMBER_WAIT_ROUNDS" rounds
	 * it selects a random node and performs lookup on a randomly generated key.
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void generateTraffic() {
		// Generate a lookup request.
		if (NUMBER_WAIT_ROUNDS != 0 && nRound % NUMBER_WAIT_ROUNDS == 0) {
			// Select a random target.
			Node node = selectRandomNode();

			// Ask the node for a random key (to modify with a message).
			int key = (new Random()).nextInt((int) Math.pow(2, ID_SIZE));
			node.lookup(key);
		}

		// Check whether a new node will join.
		if ((new Random()).nextDouble() < PROBABILITY_JOIN) {
			// Create a new node.
			Node joiningNode = context.addNode();

			// Ask an existing node to join the ring.
			Node target = selectRandomNode();
			joiningNode.join(target.id);
		}

		// Check whether an existing node will leave.
		if ((new Random()).nextDouble() < PROBABILITY_LEAVE) {
			Node node = selectRandomNode();

			// Let the node leave.
			if (node != null) {
				node.leave();
			}
		}

		// Check whether an existing node will fail.
		if ((new Random()).nextDouble() < PROBABILITY_FAIL) {
			Node node = selectRandomNode();

			// Let the node fail.
			if (node != null) {
				node.fail();
			}
		}

		nRound += 1;
	}
}
