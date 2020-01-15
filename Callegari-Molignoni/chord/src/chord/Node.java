package chord;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.Network;

/**
 * Implements a generic process that is part of the Chord protocol.
 */
public class Node {
	/**
	 * Context of the simulation.
	 */
	private NodeContext context;

	/**
	 * Identifier of the node.
	 */
	public int id;

	/**
	 * Whether the node has joined the Chord ring.
	 */
	public boolean ready = false;

	/**
	 * List of the successors in the ring.
	 */
	private List<Integer> successorList;

	/**
	 * Previous node in the ring.
	 */
	private int predecessor = -1;

	/**
	 * Table for nodes with distance of at least 2^k from the node.
	 */
	private FingerTable finger;

	/**
	 * Buffer containing all the messages received in the previous and current step.
	 */
	private LinkedList<Message> messageBuffer = new LinkedList<Message>();

	/**
	 * Number of rounds since the creation of this node.
	 */
	private int nRound = 0;

	/**
	 * Number of lookups that started from this node. Used to generate a unique ID
	 * for the search.
	 */
	private int nLookup = 1;

	/**
	 * List containing all the requests done by the node for which it has yet to
	 * receive a response.
	 */
	private HashMap<String, Integer> openRequests = new HashMap<String, Integer>();

	// ------------------------------------------------------------------------
	// PARAMETERS FOR THE SIMULATION

	/**
	 * Defines the maximum size of the ID space (i.e. 2^ID_SPACE).
	 */
	private final int ID_SIZE;

	/**
	 * Number of rounds between stabilizations.
	 */
	private final int NUMBER_STABILIZE_ROUNDS;

	/**
	 * Number of rounds between finger updates.
	 */
	private final int NUMBER_FIX_FINGERS_ROUNDS;

	/**
	 * Maximum size of the successor list.
	 */
	private int SUCCESSOR_LIST_SIZE = 1;
	
	/**
	 * Whether the node uses a successor list with more than one entry.
	 */
	private final boolean USE_SUCCESSOR_LIST;

	// ------------------------------------------------------------------------
	// GRAPHICAL INFORMATION
	
	/**
	 * Contains IDs of nodes with which it has currently an open request. Used to
	 * change color of edges.
	 */
	public LinkedList<Integer> askedTo = new LinkedList<Integer>();

	/**
	 * Defines the action performed in the last step by the node. Used to change the
	 * color of the node.
	 */
	public enum ActionPerformed {
		IDLE, SEARCH_SUCCESSOR, FOUND_RESPONSE, RETURN_RESPONSE
	};

	public ActionPerformed action = ActionPerformed.IDLE;

	/**
	 * Constructor of the node that saves the required parameters.
	 * 
	 * @param context the context of the simulation.
	 * @param id      the identifier of the node, in the range [0,2^m).
	 */
	public Node(NodeContext context, int id) {
		this.context = context;
		this.id = id;
		
		// Get the simulation parameters.
		Parameters p = RunEnvironment.getInstance().getParameters();
		ID_SIZE = p.getInteger("ID_SIZE");
		NUMBER_STABILIZE_ROUNDS = p.getInteger("NUMBER_STABILIZE_ROUNDS");
		NUMBER_FIX_FINGERS_ROUNDS = p.getInteger("NUMBER_FIX_FINGERS_ROUNDS");
		USE_SUCCESSOR_LIST = p.getBoolean("USE_SUCCESSOR_LIST");

		// Create the successor list and the finger table.
		successorList = new LinkedList<Integer>();
		finger = new FingerTable(ID_SIZE);
		successorList.add(-1);
	}

	/**
	 * Initializes the successor of the current node.
	 * 
	 * @param ids the ordered set of node IDs.
	 */
	public void initializeNode(NavigableSet<Integer> ids) {
		// Update the size of the successor list.
		updateListSize();
		
		// Initialize the first successor.
		if (id == ids.last()) {
			successorList.set(0, ids.first());
		} else {
			successorList.set(0, ids.higher(id));
		}

		ready = true;
		
		// System.out.println("The successor of " + id + " is initialized to " + successorList.get(0));
	}

	/**
	 * At each tick of the simulation, the node stabilizes, reads every message it
	 * has in its buffer and runs the handler.
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		action = ActionPerformed.IDLE;

		// Check whether the node is part of the ring.
		if (ready) {
			if (NUMBER_STABILIZE_ROUNDS != 0 && nRound % NUMBER_STABILIZE_ROUNDS == 0) {
				// Keep the successor updated.
				stabilize();
			}

			if (NUMBER_FIX_FINGERS_ROUNDS != 0 && nRound % NUMBER_FIX_FINGERS_ROUNDS == 0) {
				// Keep the successor list updated.
				stabilizeList();
				
				// Keep the fingers updated.
				fixFingers();
			}
		}

		// Handle incoming messages.
		while (messageBuffer.size() > 0) {
			// Get the oldest message in the buffer.
			Message m = messageBuffer.pop();

			// Check the type of message.
			if (m instanceof SearchSuccessorMessage) {
				findSuccessor((SearchSuccessorMessage) m);
			} else if (m instanceof LookupResultMessage) {
				returnLookup((LookupResultMessage) m);
			} else if (m instanceof PredecessorRequestMessage) {
				returnPredecessor((PredecessorRequestMessage) m);
			} else if (m instanceof PredecessorReplyMessage) {
				checkSuccessor((PredecessorReplyMessage) m);
			} else if (m instanceof NotificationMessage) {
				handleNotification((NotificationMessage) m);
			} else if (m instanceof JoinRequestMessage) {
				handleJoin((JoinRequestMessage) m);
			} else if (m instanceof JoinReplyMessage) {
				initSuccessor((JoinReplyMessage) m);
			} else if (m instanceof ListRequestMessage) {
				returnSuccessorList((ListRequestMessage) m);
			} else if (m instanceof ListReplyMessage) {
				reconcileSuccessorList((ListReplyMessage) m);
			}
		}

		// Increment the round number.
		nRound += 1;
	}

	/**
	 * Function called when an external entity wants to perform a lookup.
	 * 
	 * @param key the searched ID. It has to be in the range [0,2^m).
	 */
	public void lookup(int key) {
		// System.out.println("ASK NODE " + id + " FOR KEY " + key + " AT ROUND " + nRound);
		
		// Check whether the node is part of the ring.
		if (ready) {
			int successor = getFirstSuccessor();
			
			// Check whether the successor is responsible for the key.
			if (isIn(id, successor, key, true)) {
				// System.out.println("LOOKUP FINISHED: The handler of key " + key + " is node " + successor);
				
				action = ActionPerformed.FOUND_RESPONSE;
			} else {
				
				// Ask another node to perform the lookup.
				int nextNode = closestPrecedingNode(key);
				SearchSuccessorMessage m = new SearchSuccessorMessage(id, id + "_" + nLookup, key);
				send(m, nextNode);
				
				askedTo.add(nextNode);				
				nLookup += 1;
				
				action = ActionPerformed.SEARCH_SUCCESSOR;
			}
		}
	}

	// ------------------------------------------------------------------------
	// EXTERNAL EVENTS
	
	/**
	 * Lets the current node join the Chord ring by asking an existing node.
	 * 
	 * @param nodeId the node already in the ring.
	 */
	public void join(int nodeId) {
		send(new JoinRequestMessage(id), nodeId);
		
		// System.out.println("\n\n" + id + " ATTEMPTING TO JOIN THROUGH " + nodeId);
	}
	
	/**
	 * Lets the current node leave voluntarily, notifying its predecessor and successor.
	 */
	public void leave() {
		int successor = getFirstSuccessor();
		
		// Send the successor list to the predecessor.
		send(new ListReplyMessage(id, successorList), predecessor);
		
		// Send the predecessor to the successor.
		send(new PredecessorReplyMessage(id, predecessor), successor);
		
		// Remove the node from the context.
		context.removeNode(id);
		
		// System.out.println("NODE " + id + " LEFT");
	}
	
	/**
	 * Simulates the failure of the current node.
	 */
	public void fail() {
		// Remove the node from the context.
		context.removeNode(id);
		
		// System.out.println("NODE " + id + " FAILED");
	}
	
	/**
	 * Simulates a timeout after sending a message to a departed node.
	 * 
	 * @param message the original message.
	 * @param the departed target of the message.
	 */
	public void timeout(Message m, int target) {
		// Check whether a lookup was being performed.
		if (m instanceof SearchSuccessorMessage) {
			SearchSuccessorMessage message = (SearchSuccessorMessage) m;
			
			// Retry the lookup, avoiding the same node.
			int nextNode = closestPrecedingNode(message.key);
			if (nextNode != target) {
				send(message, nextNode);

				// System.out.println("Node " + target + " timed out. Retrying the lookup on " + nextNode + "...");
			}
		}
		
		// TODO: Notify other nodes?
	}
	
	// ------------------------------------------------------------------------
	// COMMUNICATION

	/**
	 * Generic function used to receive a message from the router. The message is
	 * just added to the buffer.
	 * 
	 * @param m the message received from the router.
	 */
	public void receive(Message m) {
		messageBuffer.add(m);
	}

	/**
	 * Sends a message to another node through the router.
	 * 
	 * @param m      the message to send.
	 * @param nodeId the ID of the target node.
	 */
	private void send(Message m, int nodeId) {
		context.router.send(m, nodeId);
	}

	// STABILIZATION
	
	/**
	 * Updates the successor periodically.
	 */
	private void stabilize() {
		// Ask the successor for its own predecessor.
		send(new PredecessorRequestMessage(id), getFirstSuccessor());
	}
	
	/**
	 * Updates the successor list periodically.
	 */
	private void stabilizeList() {
		// Ask the successor for its successor list.
		send(new ListRequestMessage(id), getFirstSuccessor());
	}

	/**
	 * Updates the finger table periodically.
	 */
	private void fixFingers() {
		// Get the next finger to update.
		int k = finger.getNext();

		int target_key = (id + (int) Math.pow(2, k)) % (int) Math.pow(2, ID_SIZE);
		String searchId = id + "_" + "f" + k;

		// Find the finger as successor of the target key.
		int nextNode = closestPrecedingNode(target_key);
		send(new SearchSuccessorMessage(id, searchId, target_key), nextNode);
	}

	// ------------------------------------------------------------------------
	// UTILITY FUNCTIONS
	
	/**
	 * Adds a finger to the finger table.
	 * 
	 * @param k     the index of the finger.
	 * @param newId the new ID of the finger.
	 */
	private void setFinger(int k, int newId) {
		Network<Node> net = (Network<Node>) context.getProjection("connections network");
		Map<Integer, Node> idMap = context.idMap;
		int oldId = finger.get(k);

		// Set the finger and add the corresponding edge.
		if (newId != oldId && idMap.get(newId) != null) {
			finger.set(k, newId);
			net.addEdge(this, idMap.get(newId));
			net.removeEdge(net.getEdge(this, idMap.get(oldId)));
			
			// System.out.println("\t" + id + " UPDATED ITS FINGER " + k + " TO " + newId);
		}
	}
	
	/**
	 * Returns the first active successor from the successor list. All the preceding entries are removed.
	 * 
	 * @return the first successor.
	 */
	private int getFirstSuccessor() {
		int index = 0;
		boolean found = false;
		
		// Find the first active successor.
		for (int i = 0; i < successorList.size() && !found; i++) {
			Node node = context.idMap.get(successorList.get(i));
			
			if (node != null) {
				index = i;
				found = true;
			}
		}
		
		// Shift the list by removing the preceding entries.
		int res = successorList.get(index);
		while (successorList.get(0) != res) {
			successorList.remove(0);
		}
		
		// System.out.println("The first active successor of " + id + " is " + successorList.get(res));
		
		// Return the first element, as all the previous ones have been removed.
		return res;
	}
	
	/**
	 * Updates the size of the successor list to the log base 2 of the number of nodes.
	 */
	private void updateListSize() {
		// Check whether the successor list can contain multiple entries.
		if (USE_SUCCESSOR_LIST) {
			SUCCESSOR_LIST_SIZE = 2 * ((int) (Math.log(context.space.size()) / Math.log(2)));
		} else {
			SUCCESSOR_LIST_SIZE = 1;
		}
	}
	
	/**
	 * Checks whether a number n is in the circular range (lower,upper) or (lower,upper].
	 * 
	 * @param lower the lower bound.
	 * @param upper the upper bound.
	 * @param n the number to check.
	 * @param includeUpper whether to include the upper bound.
	 * @return the outcome.
	 */
	private boolean isIn(int lower, int upper, int n, boolean includeUpper) {
		if (includeUpper) {
			return ((lower < n && n <= upper) || (upper < lower && lower < n) || (n <= upper && upper < lower));
		} else {
			return ((lower < n && n < upper) || (upper < lower && lower < n) || (n < upper && upper < lower));
		}
	}
	
	/**
	 * Computes the clockwise distance between two points of the ring.
	 * 
	 * @param a the first number.
	 * @param b the second number.
	 * @return the clockwise distance.
	 */
	private int clockwiseDistance(int a, int b) {
		if (b > a) {
			return b - a;
		} else {
			return (int) Math.pow(2, ID_SIZE) - a + b;
		}
	}

	// ------------------------------------------------------------------------
	// PROTOCOL METHODS
	
	/**
	 * Function used to find, in the fingers, the closest node to the key.
	 * 
	 * @param key the key that has to be found.
	 * @return the ID of the closest node to the key.
	 */
	private int closestPrecedingNode(int key) {
		int res = getFirstSuccessor();
		
		int resFinger = res;
		boolean foundFinger = false;
		// Iterate backwards over the finger table.
		for (int i = finger.size() - 1; i >= 0 && !foundFinger; i--) {
			int curr = finger.get(i);
			
			// Check whether curr is in the interval (id, key).
			if (context.idMap.get(curr) != null && isIn(id, key, curr, false)) {
				resFinger = curr;
				foundFinger = true;
			}
		}

		int resSucc = res;
		boolean foundSucc = false;
		// Iterate backwards over the successor list.
		for (int i = successorList.size() - 1; i >= 0 && !foundSucc; i--) {
			int curr = successorList.get(i);
			
			// Check whether curr is in the interval (id, key).
			if (context.idMap.get(curr) != null && isIn(id, key, curr, false)) {
				resSucc = curr;
				foundSucc = true;
			}
		}
		
		// Compare the results and choose the best one.
		int diffFinger = clockwiseDistance(resFinger, key);
		int diffSucc = clockwiseDistance(resSucc, key);
		res = diffFinger < diffSucc ? resFinger : resSucc;
		
		/*System.out.println("\nID: " + id + "\tTarget: " + key);
		System.out.println("\t" + finger);
		System.out.println("\t" + successorList);
		System.out.println("\tresFinger: " + resFinger + "\tresSucc: " + resSucc);
		System.out.println("\tdiffFinger: " + diffFinger + "\tdiffSucc: " + diffSucc);*/
		
		// System.out.println("The closest preceding node for key " + key + " is " + res);

		return res;
	}

	/**
	 * Method called each time the node receives the request of lookup from another
	 * node. If it is the responsible, then it sends back the response, otherwise it
	 * asks to another node.
	 * 
	 * @param message the message received from the node which contains the key to
	 *                be searched.
	 */
	private void findSuccessor(SearchSuccessorMessage message) {
		int successor = getFirstSuccessor();
		
		// Check whether the current node is the responsible for the key.
		if (isIn(id, successor, message.key, true)) {
			action = ActionPerformed.FOUND_RESPONSE;
			
			// Reply with the result of the lookup.
			LookupResultMessage response = new LookupResultMessage(id, message.searchId, successor, message.key);
			send(response, message.sender);

			// System.out.println("\t" + id + " FOUND A RESPONSE FOR KEY " + message.key);
		} else {
			// Ask another node to perform the lookup.
			int nextNode = closestPrecedingNode(message.key);
			SearchSuccessorMessage response = new SearchSuccessorMessage(id, message.searchId, message.key);
			
			// System.out.println("\t" + id + " asks " + nextNode + " for key: " + message.key);
			
			openRequests.put(message.searchId, message.sender);
			send(response, nextNode);
			action = ActionPerformed.SEARCH_SUCCESSOR;
			askedTo.add(nextNode);
		}
	}

	/**
	 * Method called each time the node receives a response for a request it
	 * performed. It removes the request from the open requests and forwards the
	 * response to other nodes or prints the response if it was the one who started
	 * the lookup.
	 * 
	 * @param message the message that contains the result of a request.
	 */
	private void returnLookup(LookupResultMessage message) {
		action = ActionPerformed.RETURN_RESPONSE;

		// Check whether the reply corresponds to an open request.
		if (!openRequests.containsKey(message.searchId)) {
			// If the request was not in the buffer, the node itself was the requester.
			int resNode = message.resultNode;
			
			// System.out.println("LOOKUP FINISHED: The handler of key " + message.key + " is node " + resNode);

			// Parse the search ID to obtain more information.
			String[] tokens = message.searchId.split("_");
			String lookup = tokens[1];

			// Check the type of result.
			if (lookup.equals("0")) {  // Join request
				int joiningId = Integer.parseInt(tokens[0]);

				// Reply to the requester.
				send(new JoinReplyMessage(id, resNode), joiningId);
			} else if (lookup.startsWith("f")) {  // Finger update
				int index = Integer.parseInt(lookup.substring(1));

				if (index == finger.getNext()) {
					// Update the finger.
					setFinger(index, resNode);

					// Set the next finger to update.
					finger.setNext();
				}
			}
		} else {
			// Forward the request to the responder.
			LookupResultMessage result = new LookupResultMessage(id, message.searchId, message.resultNode, message.key);
			send(result, openRequests.get(message.searchId));

			// Remove the request from the open requests.
			openRequests.remove(message.searchId);

			// System.out.println("Forwarding response from " + id + " to " + openRequests.get(message.searchId));
		}

		askedTo.remove(Integer.valueOf(message.sender)); // For edge visualization.
	}
	
	// ------------------------------------------------------------------------
	// MESSAGE HANDLERS

	/**
	 * Method called each time the node is asked for its predecessor.
	 * 
	 * @param message the message that asks for the predecessor.
	 */
	private void returnPredecessor(PredecessorRequestMessage message) {
		// Send the predecessor to the requester.
		send(new PredecessorReplyMessage(id, predecessor), message.sender);
	}

	/**
	 * Notifies the successor of the existence of the current node.
	 */
	private void notifySuccessor() {
		// Notify the successor.
		send(new NotificationMessage(id), getFirstSuccessor());
	}

	/**
	 * Checks if the successor's predecessor is the current node.
	 * 
	 * @param message the message that contains the successor's predecessor.
	 */
	private void checkSuccessor(PredecessorReplyMessage message) {
		int x = message.predecessor;

		// Check whether the successor must be updated.
		if (x >= 0 && isIn(id, getFirstSuccessor(), x, false)) {
			successorList.set(0, x);
			
			// System.out.println("\t" + id + " UPDATED ITS SUCCESSOR TO " + successorList.get(0));
		}

		// Notify the successor of the existence of the current node.
		notifySuccessor();
	}

	/**
	 * Handles an notification from a possible predecessor.
	 * 
	 * @param message the message that contains the notification.
	 */
	private void handleNotification(NotificationMessage message) {
		int x = message.sender;
		
		// Check whether the predecessor must be updated.
		if (x >= 0 && isIn(predecessor, id, x, false)) {
			int oldPredecessor = predecessor;
			predecessor = x;
			
			// Notify the old predecessor so that it can update its successor.
			send(new PredecessorReplyMessage(id, x), oldPredecessor);			
			
			// System.out.println("\t" + id + " UPDATED ITS PREDECESSOR TO " + predecessor);
		}
	}

	/**
	 * Handles a join request from a new node.
	 * 
	 * @param message the message of the request.
	 */
	private void handleJoin(JoinRequestMessage message) {
		// Find the successor of the new node.
		int key = message.sender;
		int nextNode = closestPrecedingNode(key);
		send(new SearchSuccessorMessage(id, message.sender + "_" + 0, key), nextNode);
		
		// System.out.println(id + " is searching the successor of " + key + " through " + nextNode);
	}

	/**
	 * Initializes the successor to join the Chord ring.
	 * 
	 * @param message the message that contains the successor.
	 */
	private void initSuccessor(JoinReplyMessage message) {
		successorList.set(0, message.successor);
		ready = true;

		// System.out.println("\t" + id + " JOINED WITH SUCCESSOR " + successorList.get(0));
	}
	
	/**
	 * Sends the node's successor list to the requester.
	 * 
	 * @param message the message of the request.
	 */
	private void returnSuccessorList(ListRequestMessage message) {
		send(new ListReplyMessage(id, successorList), message.sender);
	}
	
	/**
	 * Reconciles the successor list with the one of the message.
	 * 
	 * @param message the message that contains the successor list of the successor.
	 */
	private void reconcileSuccessorList(ListReplyMessage message) {
		List<Integer> newList = message.successorList;

		// Clear the current successor list and add the immediate successor.
		successorList.clear();
		successorList.add(0, message.sender);
		
		updateListSize();
		int nElem = Math.min(newList.size(), SUCCESSOR_LIST_SIZE - 1);
		
		// Copy the remaining elements from the message's list, stopping if the current ID is found.
		boolean found = false;
		for (int i = 0; i < nElem && !found; i++) {
			int curr = newList.get(i);
			
			if (curr != id) {
				successorList.add(curr);
			} else {
				found = true;
			}
		}

		// System.out.println("\t" + id + " UPDATED ITS SUCCESSOR LIST TO " + successorList);
	}
}
