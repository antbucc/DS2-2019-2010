/**
 * 
 */
package chord;


import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.codec.digest.DigestUtils;

import analyses.Collector;
import chord.messages.Ack;
import chord.messages.CheckPredecessor;
import chord.messages.FindSuccessorReply;
import chord.messages.FindSuccessorRequest;
import chord.messages.FindSuccessorRequest.ReqType;
import chord.messages.JoinReply;
import chord.messages.JoinRequest;
import chord.messages.Message;
import chord.messages.NotifyPredecessor;
import chord.messages.NotifySuccessor;
import chord.messages.StabilizeReply;
import chord.messages.StabilizeRequest;
import chord.messages.VoluntaryLeave;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import utils.ChordUtilities;
import visualization.Visualizer;
import visualization.Visualizer.EdgeType;

/**
 * Represents a node of the network.
 * 
 * @author zanel
 * @author coffee
 *
 */
public class Node {
	
	/**
	 * The identity of the node in the identifier circle.
	 */
	private Long id;
	
	/**
	 * The IP address and port number of the node.
	 */
	private InetSocketAddress address;
	     
	/**
	 * The routing table.
	 * Each index i contains the successor of id + 2^i.
	 */
	private Long[] fingerTable;

	/**
	 * The index of the next finger entry to fix.
	 */
	private int fingerToFix;
	
	/**
	 * The successor node on the identifier circle.
	 */
	private Long successor;
	
	/**
	 * The node's first r successors on the identifier circle.
	 */
	private TreeSet<Long> successors;
	
	/**
	 * The previous node on the identifier circle.
	 */
	private Long predecessor;
	
	/**
	 * The buffer storing the incoming messages.
	 */
	private ConcurrentLinkedQueue<Message> messages;
	
	/**
	 * The buffer storing the lookup requests sent by the node to intermediary
	 * nodes together with the tick during which the message was sent.
	 */
	private HashMap<UUID, SentMessage> lookupLocalTimeouts;
	
	/**
	 * The buffer storing the lookup requests initiated by the node together
	 * with the tick during which the message was sent.
	 */
	private HashMap<UUID, SentMessage> lookupGlobalTimeouts;
	
	/**
	 * The buffer storing the stabilize requests sent by the node to the
	 * successor together with the tick during which the message was sent.
	 */
	private HashMap<UUID, SentMessage> stabilizeTimeouts;
	
	/**
	 * The buffer storing the messages sent by the node to check whether
	 * the predecessor has failed together with the tick during which
	 * the message was sent. 
	 */
	private HashMap<UUID, SentMessage> predecessorTimeouts;
	
	/**
	 * The agent responsible for collecting the information necessary to
	 * visualize the state of the network.
	 */
	private Visualizer visualizer;
	
	/**
	 * The agent responsible for collecting the information necessary to
	 * perform the analyses.
	 */
	private Collector collector;
	
	/**
	 * The parameters provided by the user during the simulation.
	 */
	private HashMap<String, Object> parameters;
	
	/**
	 *  Buffer that stores all joined nodes in the network, new nodes which have not completed the 
	 *  joining phase are not contained in this buffer
	 */
	private TreeSet<Long> joinedNodesId;
	
	/**
	 * Needed in order to colour the new joining nodes
	 */
	private boolean isNewNode;
	
	/**
	 * The tick of the last stabilize call
	 */
	private double lastStabilizeTick;
	
	/**
	 * The tick of the last checkPredecessor call
	 */
	private double lastCheckPredecessorTick;
	
	/**
	 * The tick of the last fixFinger call
	 */
	private double lastFixFingerTick;
	
	/**
	 * The number of requests received from nodes the node thought were failed
	 */
	private int expiredRequestsCounter;
	
	/**
	 * Instantiates a fictitious node, needed for debugging and for creating temporary nodes
	 * 
	 * @param id the id of the fictitious node
	 */
	public Node(Long id) {
		this.id = id;
	}
	
	/**
	 * Instantiates a new node.
	 * 
	 * @param address the IP address together with the port number
	 * @param visual the agent responsible for visualization
	 * @param collector the agent responsible for performing analyses
	 * @param joinedNodesId the buffer that stores all joined nodes in the network
	 */
	public Node(InetSocketAddress address, Visualizer visualizer, Collector collector, TreeSet<Long> joinedNodesId) {
		this.joinedNodesId = joinedNodesId;
		this.parameters = ChordUtilities.getParameters();
		this.address = address;
		this.messages = new ConcurrentLinkedQueue<Message>();
		this.lookupGlobalTimeouts = new HashMap<>();
		this.lookupLocalTimeouts = new HashMap<>();
		this.stabilizeTimeouts = new HashMap<>();
		this.predecessorTimeouts = new HashMap<>();
		this.lastStabilizeTick = 0;
		this.lastCheckPredecessorTick = 0;
		this.lastFixFingerTick = 0;
		isNewNode = false;
		// computes the hash of the IP address and trasforms it into a long id
		byte[] digest = DigestUtils.sha1(this.address.getAddress().toString() + this.address.getPort() + UUID.randomUUID());
		try {
			this.id = ChordUtilities.convertIntoKeySpace(digest, (int)parameters.get(ChordUtilities.KEY_SIZE));
		} catch(IllegalArgumentException e) {
			// If the chosen key space is too large the program crash with the error message
			System.exit(1);
		}
		this.visualizer = visualizer;
		this.collector = collector;
		// elements inside successors list are ordered based on identifier space
		this.successors = new TreeSet<>(new Comparator<Long>() {
			@Override
			public int compare(Long node1Id, Long node2Id) {
				if(Long.compareUnsigned(id, node1Id) < 0 && Long.compareUnsigned(id, node2Id) > 0) {
					return -1;
				} else if (Long.compareUnsigned(id, node1Id) > 0 && Long.compareUnsigned(id, node2Id) < 0) {
					return 1;
				} else {
					return Long.compareUnsigned(node1Id, node2Id);
				}
			}
		});
		int keySize = (Integer) parameters.get(ChordUtilities.KEY_SIZE);
		// finger table has log2^m elements 
		this.fingerTable = new Long[keySize];
		// initialize all elements of finger table to the node identifier
		Arrays.fill(fingerTable, id);
	}

	/*
	 * Method executed at every s
	 */
	@ScheduledMethod(start=0, interval=1, priority=ScheduleParameters.LAST_PRIORITY)
	public void step() {
		// getNeededParameters
		HashMap<String, Object> parameters = ChordUtilities.getParameters();
		Float lookupGenerationProb = (Float)parameters.get(ChordUtilities.LOOKUP_GENERATION_PROBABILITY);
		Integer keySize = (Integer)parameters.get(ChordUtilities.KEY_SIZE);
		Integer stabilizeInterval = (Integer)parameters.get(ChordUtilities.STABILIZE_INTERVAL);
		Integer checkPredecessorInterval = (Integer)parameters.get(ChordUtilities.CHECK_PREDECESSOR_INTERVAL);
		Integer fixFingerInterval = (Integer)parameters.get(ChordUtilities.FIX_FINGER_INTERVALL);
		// expired requests analysis
		expiredRequestsCounter = 0;
		
		// Messages dispatcher
		Iterator<Message> it = messages.iterator();
		while(it.hasNext()) {
			Message message = it.next();
			if(ChordUtilities.getCurrentTick() >= message.getTick()) {
				if(message instanceof Ack) {
					ackHandler((Ack) message);
					messages.remove(message);
				} else if(message instanceof CheckPredecessor) {
					checkPredecessorHandler((CheckPredecessor) message);
					messages.remove(message);
				} else if(message instanceof FindSuccessorReply) {
					findSuccessorReplyHandler((FindSuccessorReply) message);
					messages.remove(message);
				} else if(message instanceof FindSuccessorRequest) {
					findSuccessorRequestHandler((FindSuccessorRequest) message);
					messages.remove(message);
				} else if(message instanceof NotifyPredecessor) {
					notifyPredecessorHandler((NotifyPredecessor) message);
					messages.remove(message);
				} else if(message instanceof NotifySuccessor) { 
					notifySuccessorHandler((NotifySuccessor) message);
					messages.remove(message);
				} else if(message instanceof StabilizeReply) {
					stabilizeReplyHandler((StabilizeReply) message);
					messages.remove(message);
				} else if(message instanceof StabilizeRequest) {
					stabilizeRequestHandler((StabilizeRequest) message);
					messages.remove(message);
				} else if(message instanceof VoluntaryLeave) {
					voluntaryLeaveHandler((VoluntaryLeave) message);
					messages.remove(message);
				} else if(message instanceof JoinReply) {
					joinReplyHandler((JoinReply) message);
					messages.remove(message);
				} else if(message instanceof JoinRequest) {
					joinRequestHandler((JoinRequest) message);
					messages.remove(message);
				} else {
					
				}
			}
		}

		if(successor != null) {
			// generate a new lookup with a certain probability
			if(RandomHelper.nextDouble() < lookupGenerationProb) {
				// if maxKey is less then 0 means that unsigned values have to be used
				Long keyToFind = RandomHelper.getUniform().nextLongFromTo(Long.MIN_VALUE, Long.MAX_VALUE);
				BigInteger key = new BigInteger(Long.toUnsignedString(keyToFind));
				keyToFind = key.mod(BigInteger.valueOf(2).pow(keySize)).longValue();
				findSuccessor(keyToFind);
			}
			// methods needed in order to stabilize the network and update the known nodes
			if(ChordUtilities.getCurrentTick() - lastStabilizeTick > stabilizeInterval) {
	
				if(successor != id) {
					// only in this case because otherwise, it means that the node may be 
					// the first node in the network and calling the stabilise method, it will set
					// its predecessor to its own id and the network will never stabilise
					stabilize();
				}
				lastStabilizeTick = ChordUtilities.getCurrentTick();
			}
			if(ChordUtilities.getCurrentTick() - lastFixFingerTick > fixFingerInterval) {
				fixFingers();
				lastFixFingerTick = ChordUtilities.getCurrentTick();
			}
			fixFingers();
			
			if(ChordUtilities.getCurrentTick() - lastCheckPredecessorTick > checkPredecessorInterval && predecessor != null) {
				checkPredecessor();
				lastCheckPredecessorTick = ChordUtilities.getCurrentTick();
			}
		}
		
		// check the timeouts of each sent message in every timeout table
		checkLookupLocalTimeouts();
		checkLookupGlobalTimeouts();
		checkStabilizeTimeouts();
		checkPredecessorTimeouts();
		
		// analysis: expired requests
		collector.notifyExpiredRequest(id, expiredRequestsCounter);
		
		// analysis: stabilization
		Long realSuccessor = joinedNodesId.ceiling(id + 1);
		if(realSuccessor == null) {
			realSuccessor = joinedNodesId.first();
		} 
		
		// analysis: stabilize period and stabilize interval
		if(joinedNodesId.contains(id) && Long.compareUnsigned(realSuccessor, successor) != 0) {
			collector.notifyStabilization();
		} else if(!joinedNodesId.contains(id)) {
			collector.notifyStabilization();
		}
	}
	
	/**
	 * Simulates the sending of a message to this node.
	 * 
	 * @param message the message to be sent.
	 */
	public void receive(Message message) {
		// get needed runtime parameters
		float networkDelayMean = (Float) parameters.get(ChordUtilities.NETWORK_DELAY_MEAN);
		
		double msgTick = ChordUtilities.getDelayedTick(networkDelayMean);
		message.setTick(msgTick);
		
		messages.add(message);
	}
	
	/**
	 * Creates a new Chord ring.
	 */
	public void create() {
		predecessor = null;
		successor = id;
		for(Long finger : fingerTable) {
			finger = id;
		}
	}
	
	/**
	 * Join the Chord ring using the knowledge of a node contained in it.
	 * 
	 * @param nodeId the Chord identifier of the node known by the current node used
	 * to join the network.
	 */
	public void join(Long nodeId) {
		predecessor = null;
		successor = null;
		isNewNode = true;
		// Prepare the join request to the known node nodeId
		JoinRequest requestMsg = new JoinRequest(id);
		
		// Send the join request to the knownNode
		try {
			Node knownNode = ChordUtilities.getNodeById(nodeId, ContextUtils.getContext(this));
			knownNode.receive(requestMsg);
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
	}
	
	/**
	 * Simulates the voluntarily leave of the node from the Chord ring.
	 * The node sends a VoluntaryLeave message to both the successor and the predecessor.
	 */
	public void leave() {
		// The node is no more in the network, it is removed from joinedNodesId buffer
		joinedNodesId.remove(id);
		
		@SuppressWarnings("unchecked")
		VoluntaryLeave leaveMsg = new VoluntaryLeave(id, predecessor, (TreeSet<Long>) successors.clone());
		
		// sends leave message to the predecessor
		try {
			if(predecessor != null) {
				// the predecessor exists
				Node predNode = ChordUtilities.getNodeById(predecessor, ContextUtils.getContext(this));
				predNode.receive(leaveMsg);
			} else {
				// either the predecessor has failed or the node has just joined the Chord ring
			}
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
		// sends leave message to the successor
		try {
			if(successor != null) {
				// the successor exists
				Node succNode = ChordUtilities.getNodeById(successor, ContextUtils.getContext(this));
				succNode.receive(leaveMsg);	
			} else {
				// either the successor has failed or the node is the only node in the network
			}
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
		// notify the visualizer that in case the view is set to some lookup initiated by the failing node,
		// it has to be changed
		for(Map.Entry<UUID, SentMessage> entry : lookupGlobalTimeouts.entrySet()) {
			FindSuccessorRequest oldReq = (FindSuccessorRequest)entry.getValue().getMessage();
			visualizer.notifyEndedLookup(oldReq.getTargetId());
		}
		ContextUtils.getContext(this).remove(this);
	}
	
	/**
	 * Simulates the failure of the node, after which the node will be removed from
	 * the Chord ring.
	 */
	public void fail() {
		// The node is no more in the network, it is removed from joinedNodesId buffer
		// and from context
		joinedNodesId.remove(id);
		// notify the visualizer that in case the view is set to some lookup initiated by the failing node,
		// it has to be changed
		for(Map.Entry<UUID, SentMessage> entry : lookupGlobalTimeouts.entrySet()) {
			FindSuccessorRequest oldReq = (FindSuccessorRequest)entry.getValue().getMessage();
			visualizer.notifyEndedLookup(oldReq.getTargetId());
		}
		ContextUtils.getContext(this).remove(this);
	}
	
	/**
	 * Clear all the variables of the node.
	 */
	private void clearNode() {
		id = null;
		address = null;
		fingerTable = null;
		fingerToFix = -1;
		successor = null;
		successors = null;
		predecessor = null;
		messages = null; 
		lookupLocalTimeouts = null;
		lookupGlobalTimeouts = null;
		stabilizeTimeouts = null;
		predecessorTimeouts = null;
		visualizer = null;
		collector = null;
	}
	
	/**
	 * Checks whether the successor holds the identifier the node is looking for.
	 * 
	 * @param targetId the Chord identifier the node is looking for.
	 * @return whether the successor holds the identifier the node is looking for.
	 */
	private boolean successorHasKey(Long targetId) {
		boolean successorFound = false;
		
		// successor > id
		if(Long.compareUnsigned(id, successor) < 0) {
			// targetId > id && targetId <= successor
			if(Long.compareUnsigned(id, targetId) < 0 && (Long.compareUnsigned(targetId, successor) <= 0)) {
				successorFound = true;
			}
		} else {
			// targetId > id
			if(Long.compareUnsigned(id, targetId) < 0) {
				successorFound = true;
			} else {
				// targetId <= successor
				if(Long.compareUnsigned(targetId, successor) <= 0) {
					successorFound = true;
				}
			}
		}
		
		return successorFound;
	}
	
	/**
	 * Wrapper method of findSuccessor which is going to be called by the application level,
	 * abstracting the type of request parameter (which should not be considered by the user).
	 * 
	 * @param targetId the identifier the node is looking for.
	 */
	public void findSuccessor(Long targetId) {
		// if the searching is resolved within the current step, it cannot be visualised
		if(!successorHasKey(targetId) || Long.compare(targetId, id) == 0)
			visualizer.notifyChordLookup(targetId, this);
		findSuccessor(targetId, FindSuccessorRequest.ReqType.LOOKUP);
	}
	
	/**
	 * Initiates a lookup for a certain identifier.
	 * 
	 * @param targetId the identifier the node is looking for.
	 * @param reqType the enum denoting whether this method is invoked to refresh a finger table entry,
	 * join the network or to accomplish a request coming from the application level. 
	 */
	public void findSuccessor(Long targetId, FindSuccessorRequest.ReqType reqType) {
		double takenTicks = 0.0;
		
		// id == targetId
		if(Long.compareUnsigned(id, targetId) == 0) {
			// meaning that the node is looking for a key which has the same 
			// Chord identifier of the node's identifier. In this way, we avoid
			// to pass around requests which will finally mark the node itself
			// as the successor of the targetId
			switch(reqType) {
				case JOIN:
					// this method is called only when the node joins the network
					// to find its successor. In order to call the join method, the
					// node has to know one other node inside the network. This implies
					// that the node cannot be the successor of itself
					throw new IllegalArgumentException();
				case FIXFINGER:
					// refreshes the finger table entry
					fingerTable[fingerToFix] = id;
					break;
				case LOOKUP:
					// delivers the successor to the application
					chordDeliver(targetId, id, takenTicks);
				default:
					throw new IllegalArgumentException();
			}
		}
		
		if(successorHasKey(targetId)) {
			// meaning that the successor holds the key the node is looking for.
			switch(reqType) {
				case JOIN:
					// A reply to the joining node with its successor has to be sent
					Long joiningNodeId = targetId;
					JoinReply replyMsg = new JoinReply(id, successor);
					try {
						Node joiningNode = ChordUtilities.getNodeById(joiningNodeId, ContextUtils.getContext(this));
						joiningNode.receive(replyMsg);
					} catch(NoSuchElementException e) {
						// the destination node has failed
					}
					break;
				case FIXFINGER:
					// refreshes the finger table entry
					fingerTable[fingerToFix] = successor;
					break;
				case LOOKUP:
					// delivers the successor to the application
					chordDeliver(targetId, successor, takenTicks);
					break;
				default:
					throw new IllegalArgumentException();
			}
		} else {
			Long intermediateId = closestPrecedingNode(targetId);
			// the node itself is the closest preceding node of the successor it
			// is looking for, but we have already checked that the successor is not
			// the successor of the identifier the node is looking for. This implies
			// that the node is the successor of the node it is looking for. This might
			// happens when the node has no elements in both the successor list and the
			// finger table.
			// intermediateId == id
			if(Long.compareUnsigned(id, intermediateId) == 0) {
				switch(reqType) {
					case JOIN:
						// if this is the case, it means that the node known by the joining node
						// is its successor
						Long joiningNodeId = targetId;
						JoinReply reply = new JoinReply(id, id);
						try {
							Node joiningNode = ChordUtilities.getNodeById(joiningNodeId, ContextUtils.getContext(this));
							joiningNode.receive(reply);
						} catch(NoSuchElementException e) {
							// the destination node has failed
						}
						break;
					case FIXFINGER:
						// refreshes the finger table entry
						fingerTable[fingerToFix] = id;
						break;
					case LOOKUP:
						// delivers the successor to the application
						chordDeliver(targetId, id, takenTicks);
						break;
					default:
						throw new IllegalArgumentException();
				}
			} else {
				Integer next = null;
				if(reqType == FindSuccessorRequest.ReqType.FIXFINGER) {
					// if the request comes from fixFinger method, the FindSuccessorRequest
					// has to carry the index to fix. Notice that fingerToFix contains the
					// index of the next finger table entry to fix, its value will be increased
					// fixFinger method after this method ends.
					next = fingerToFix;
				}
				
				// sends a FindSuccessorMessage to the closest preceding node
				FindSuccessorRequest requestMsg = new FindSuccessorRequest(id, id, targetId, null, reqType, next, intermediateId);
				requestMsg.setLookupId(requestMsg.getMsgId());
				
				try {
					Node intermediate = ChordUtilities.getNodeById(intermediateId, ContextUtils.getContext(this));
					intermediate.receive(requestMsg);
					if(requestMsg.getTargetId() == visualizer.getTargetId()) 
						visualizer.addLink(this, intermediate, EdgeType.REQUEST);
				} catch(NoSuchElementException e) {
					// the destination node has failed
				}
				
				SentMessage sentMsg = new SentMessage(requestMsg, ChordUtilities.getCurrentTick());
				lookupGlobalTimeouts.put(requestMsg.getMsgId(), sentMsg);
				lookupLocalTimeouts.put(requestMsg.getMsgId(), sentMsg);
			}
		}
	}
	
	/**
	 * Delivers to the application the Chord identifier of the successor node.
	 * 
	 * @param targetId the identifier the initiator is looking for.
	 * @param successorId the Chord identifier of the successor node of targetId.
	 * @param ticks the number of ticks required to find the successor of targetId.
	 */
	public void chordDeliver(Long targetId, Long successorId, double time) {
		// Notifies the collector the lookup's time of the related targetId
		collector.notifyChordDeliver(targetId, ChordUtilities.getCurrentTick(), time);
		// delivers the successorId to the application
		visualizer.notifyEndedLookup(targetId);
	}
	
	/**
	 * Searches among the node's finger table and successor list for the node whose chord 
	 * identifier most immediately precedes id.
	 * 
	 * @param targetId the identifier for which the closest preceding node has to be found.
	 */
	public Long closestPrecedingNode(Long targetId) {
		// get needed runtime parameters
		int keySize = (Integer) parameters.get(ChordUtilities.KEY_SIZE);
		
		Long closestNodeId = null;
		Long closestFingerEntryId = null;
		Long closestSuccessorsEntryId = null;
		
		for(int i = keySize - 1; i >= 0 && closestFingerEntryId == null; i--) {
			Long fingerEntryId = fingerTable[i];
			// Chord ring
			// id < targetId
			if(Long.compareUnsigned(id, targetId) < 0) {
				// fingerEntryId > id && fingerEntryId < targetId
				if(Long.compareUnsigned(id, fingerEntryId) < 0 && Long.compareUnsigned(fingerEntryId, targetId) < 0) {
					closestFingerEntryId = fingerEntryId;
				}
			} else if(Long.compareUnsigned(id, targetId) > 0) {
				// fingerEntryId > id || (fingerEntryId <= id && fingerEntryId < targetId)
				if((Long.compareUnsigned(id, fingerEntryId) < 0) || (Long.compareUnsigned(fingerEntryId, id) <= 0 && Long.compareUnsigned(fingerEntryId, targetId) < 0)) {
					closestFingerEntryId = fingerEntryId;
				}
			}
		}
		
		// iterates over the elements in descending order
		Iterator<Long> succIt = successors.descendingIterator();
		while(succIt.hasNext() && closestSuccessorsEntryId == null) {
			Long successorsEntryId = succIt.next();
			
			// Chord ring
        	// id < targetId
			if(Long.compareUnsigned(id, targetId) < 0) {
				// successorsEntryId > id && successorsEntryId < targetId
				if(Long.compareUnsigned(id, successorsEntryId) < 0 && Long.compareUnsigned(successorsEntryId, targetId) < 0) {
					closestSuccessorsEntryId = successorsEntryId;
				}
			} else if(Long.compareUnsigned(id, targetId) > 0) {
				// successorsEntryId > id || (successorsEntryId <= id && successorsEntryId < targetId)
				if((Long.compareUnsigned(id, successorsEntryId) < 0) || (Long.compareUnsigned(successorsEntryId, id) <= 0 && Long.compareUnsigned(successorsEntryId, targetId) < 0)) {
					closestSuccessorsEntryId = successorsEntryId;
				}
			}
		}
        
        if(closestFingerEntryId == null && closestSuccessorsEntryId == null) {
        	closestNodeId = id;
        } else {
        	if(closestFingerEntryId == null) {
        		closestNodeId = closestSuccessorsEntryId;
        	} else if (closestSuccessorsEntryId == null) {
        		closestNodeId = closestFingerEntryId;
        	} else {
        		// distance(id, targetId) = (targetId - id) > 0 ? (targetId - id) : (targetId + (Math.pow(2, 64) - id))
        		// targetId > closestFingerEntryId ? 
        		Long targetEntryD = Long.compareUnsigned(closestFingerEntryId, targetId) <= 0 ? (targetId - closestFingerEntryId) : (targetId + (BigInteger.valueOf(2).pow(keySize).longValue() - closestFingerEntryId));
        		// targetId > closestSuccessorsEntryId ? 
        		Long successorsEntryD = Long.compareUnsigned(closestSuccessorsEntryId, targetId) <= 0 ? (targetId - closestSuccessorsEntryId) : (targetId + (BigInteger.valueOf(2).pow(keySize).longValue() - closestSuccessorsEntryId));
        		
        		closestNodeId = (Long.compareUnsigned(targetEntryD, successorsEntryD) < 0) ? closestFingerEntryId : closestSuccessorsEntryId;
        	}
        }
        
		return closestNodeId;
	}
	
	/**
	 * Learns about newly joined successors and notifies its successor about its existence.
	 */
	public void stabilize() {
		// message sent by n to get the predecessor of its successor
		StabilizeRequest stabilizeMsg = new StabilizeRequest(id, successor);
		try {
			Node succNode = ChordUtilities.getNodeById(successor, ContextUtils.getContext(this));
			succNode.receive(stabilizeMsg);
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
		
		SentMessage sentStabilize = new SentMessage(stabilizeMsg, ChordUtilities.getCurrentTick());
		// puts the stabilize message together with the tick during which it was sent in the timeout table
		stabilizeTimeouts.put(stabilizeMsg.getMsgId(), sentStabilize);
	}
	
	/**
	 * Refreshes finger table's entries one at a time.
	 */
	public void fixFingers() {
		// get needed runtime parameters
		int keySize = (Integer) parameters.get(ChordUtilities.KEY_SIZE);
		
		findSuccessor(id + BigInteger.valueOf(2).pow(fingerToFix).longValue(), FindSuccessorRequest.ReqType.FIXFINGER);
		
		// updates the index of the finger table to refresh. Notice that this is updated
		// after findSuccessor in this node ends.
		fingerToFix += 1;
		// the finger table contains keySize elements, indexed from 0 to keySize -1
		if(fingerToFix > (keySize - 1)) {
			fingerToFix = 0;
		}
	}
	
	/**
	 * Verifies whether the predecessor has crashed or not.
	 */
	public void checkPredecessor() {
		CheckPredecessor checkMsg = new CheckPredecessor(id);
		try {
			Node predNode = ChordUtilities.getNodeById(predecessor, ContextUtils.getContext(this));
			predNode.receive(checkMsg);
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
		
		SentMessage sentCheck = new SentMessage(checkMsg, ChordUtilities.getCurrentTick());
		predecessorTimeouts.put(checkMsg.getMsgId(), sentCheck);
	}
	
	/**
	 * Handles the incoming request of finding the successor of a given identifier.
	 * 
	 * @param message the incoming FindSuccessorRequest message.
	 */
	public void findSuccessorRequestHandler(FindSuccessorRequest message) {
		// Needed for analysis, notifies the collector about a new find successor request for a given targetId
		if(message.getReqType() == ReqType.LOOKUP) {
			collector.notifyFindSuccessorRequest(message.getTargetId());
		}
		// get needed runtime parameters
		boolean iterativeMode = (Boolean) parameters.get(ChordUtilities.ITERATIVE_MODE);
		if(message.getTargetId() == visualizer.getTargetId()) { 
			visualizer.getIntermediators().add(id);
			visualizer.setFingers(fingerTable);
			visualizer.setSuccessors((TreeSet<Long>)successors.clone());
		}
		if(successorHasKey(message.getTargetId())) {
			// the successor holds the identifier the initiator of this request is looking for
			FindSuccessorReply replyMsg = null;
			
			if(iterativeMode) {
				replyMsg = new FindSuccessorReply(id, message.getMsgId(), successor, FindSuccessorReply.NodeType.SUCCESSOR);
			} else {
				// meaning that the protocol is running in recursive mode. The reply that has
				// to be sent to the initiator has to contain the lookupId in the requestId field
				replyMsg = new FindSuccessorReply(id, message.getLookupId(), successor, FindSuccessorReply.NodeType.SUCCESSOR);
			
				// sends the ACK to the sender of this message
				Ack ackMsg = new Ack(id, message.getMsgId(), Ack.AckType.FINDSUCCESSOR);
				try {
					Node reqSender = ChordUtilities.getNodeById(message.getSender(), ContextUtils.getContext(this));
					reqSender.receive(ackMsg);
					if(message.getTargetId() == visualizer.getTargetId()) 
						visualizer.addLink(this, reqSender, EdgeType.ACK);
				} catch(NoSuchElementException e) {
					// the destination node has failed
				}
			}
			try {
				// sends the reply back to the successor
				Node initiatorNode = ChordUtilities.getNodeById(message.getInitiatorId(), ContextUtils.getContext(this));
				initiatorNode.receive(replyMsg);
				if(message.getTargetId() == visualizer.getTargetId()) 
					visualizer.addLink(this, initiatorNode, EdgeType.REPLY_SUCCESSOR);
			} catch(NoSuchElementException e) {
				// the destination node has failed
			}
		} else {
			Long intermediateId = closestPrecedingNode(message.getTargetId());
			if(iterativeMode) {
				FindSuccessorReply replyMsg = new FindSuccessorReply(id, message.getMsgId(), intermediateId, FindSuccessorReply.NodeType.INTERMEDIARY);
				try {
					Node initiatorNode = ChordUtilities.getNodeById(message.getSender(), ContextUtils.getContext(this));
					initiatorNode.receive(replyMsg);
					if(message.getTargetId() == visualizer.getTargetId()) 
						visualizer.addLink(this, initiatorNode, EdgeType.REPLY);
				} catch(NoSuchElementException e) {
					// the destination node has failed
				}		
			} else {
				// meaning that the protocol is running in recursive mode
				FindSuccessorRequest requestMsg = new FindSuccessorRequest(id, message.getInitiatorId(), message.getTargetId(), message.getLookupId(), message.getReqType(), message.getFingerToFix(), intermediateId);
				try {
					Node intermediate = ChordUtilities.getNodeById(intermediateId, ContextUtils.getContext(this));
					intermediate.receive(requestMsg);
					if(message.getTargetId() == visualizer.getTargetId()) 
						visualizer.addLink(this, intermediate, EdgeType.REQUEST);
				} catch(NoSuchElementException e) {
					// the destination node has failed
				}
				
				SentMessage sentMsg = new SentMessage(requestMsg, ChordUtilities.getCurrentTick());
				lookupLocalTimeouts.put(requestMsg.getMsgId(), sentMsg);
				
				Ack ackMsg = new Ack(id, message.getMsgId(), Ack.AckType.FINDSUCCESSOR);
				try {
					Node reqSender = ChordUtilities.getNodeById(message.getSender(), ContextUtils.getContext(this));
					reqSender.receive(ackMsg);
					if(message.getTargetId() == visualizer.getTargetId()) 
						visualizer.addLink(this, reqSender, EdgeType.ACK);
				} catch(NoSuchElementException e) {
					// the destination node has failed
				}
			}
		}
	}
	
	/**
	 * Handles the reply message containing the successor the initiator is looking for
	 * based on the type of the request.
	 * 
	 * @param sentLookup the lookup request initiated by the initiator together with the tick 
	 * during which it was sent.
	 * @param successorId the id of the successor the initiator is looking for.
	 */
	private void handleLookupType(SentMessage sentLookup, Long successorId) {
		
		FindSuccessorRequest lookupMsg = (FindSuccessorRequest) sentLookup.getMessage();
		
		switch(lookupMsg.getReqType()) {
			case JOIN:
				// reply to the joining node its successor.
				// the joining node has id = targetId because it is trying to find the successor of
				// its id
				Long joiningNodeId = lookupMsg.getTargetId();
				JoinReply reply = new JoinReply(id, successorId);
				try {
					Node joiningNode = ChordUtilities.getNodeById(joiningNodeId, ContextUtils.getContext(this));
					joiningNode.receive(reply);
				} catch(NoSuchElementException e) {
					// the destination node has failed
				}
				break;
			case FIXFINGER:
				// refreshes the finger table entry
				int next = lookupMsg.getFingerToFix();
				fingerTable[next] = successorId;
				break;
			case LOOKUP:
				// delivers the successor to the application
				double takenTicks = ChordUtilities.getCurrentTick() - sentLookup.getTick();
				chordDeliver(lookupMsg.getTargetId(), successorId, takenTicks);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Handles the incoming response of a FindSuccessorRequest message.
	 * 
	 * @param message the incoming FindSuccessorReply message.
	 */
	public void findSuccessorReplyHandler(FindSuccessorReply message) {
		// get needed runtime parameters
		boolean iterativeMode = (Boolean) parameters.get(ChordUtilities.ITERATIVE_MODE);
		
		if(iterativeMode) {
			if(message.getNodeType() == FindSuccessorReply.NodeType.INTERMEDIARY) {
				// meaning that the message contains an intermediate node
				SentMessage sentReq = lookupLocalTimeouts.remove(message.getRequestId());
				if(sentReq != null) {
					FindSuccessorRequest sentReqMessage = (FindSuccessorRequest)sentReq.getMessage();
					Long intermediateId = message.getNodeId();
					FindSuccessorRequest requestMsg = new FindSuccessorRequest(id, sentReqMessage.getInitiatorId(), sentReqMessage.getTargetId(), sentReqMessage.getLookupId(), sentReqMessage.getReqType(), sentReqMessage.getFingerToFix(), intermediateId);
					
					try {
						Node intermediate = ChordUtilities.getNodeById(intermediateId, ContextUtils.getContext(this));
						intermediate.receive(requestMsg);
						if(visualizer.getTargetId() != null &&Long.compare(requestMsg.getTargetId(),visualizer.getTargetId()) == 0) 
							visualizer.addLink(this, intermediate, EdgeType.REQUEST);
					} catch(NoSuchElementException e) {
						// the destination node has failed
					}
					
					// add a new entry in local timeout
					SentMessage sentMsg = new SentMessage(requestMsg, ChordUtilities.getCurrentTick());
					lookupLocalTimeouts.put(requestMsg.getMsgId(), sentMsg);
				} else {
					// meaning that this is a message received from a node n' which n thought was crashed
					expiredRequestsCounter++;
				}
			} else {
				// meaning that message.getNodeType() == FindSuccessorReply.NodeType.SUCCESSOR
				// meaning that the message contains the successor
				SentMessage sentReq = lookupLocalTimeouts.remove(message.getRequestId());
				if(sentReq != null) {
					FindSuccessorRequest sentReqMessage = (FindSuccessorRequest)sentReq.getMessage();
					SentMessage sentLookup = lookupGlobalTimeouts.remove(sentReqMessage.getLookupId());
					if(sentLookup != null) {
						// meaning that the global timeout has not expired
						handleLookupType(sentLookup, message.getNodeId());
					} else {
						// meaning that this is the response of a lookup which has expired
						expiredRequestsCounter++;
					}
				} else {
					// meaning that this is a message received from a node n' which n thought was crashed
					expiredRequestsCounter++;
				}
			}
		} else {
			// meaning that the protocol is running in recursive mode
			// in recursive mode the nodes can receive only replies containing the successor 
			// as the receipt of a message is signaled by an ACK
			SentMessage sentLookup = lookupGlobalTimeouts.remove(message.getRequestId());
			if(sentLookup != null) {
				// meaning that the global timeout has not expired
				handleLookupType(sentLookup, message.getNodeId());
			} else {
				// meaning that this is the response of a lookup which has expired
				expiredRequestsCounter++;
			}
		}
	}

	/**
	 * Handles the incoming acknowledgment of either a FindSuccessorRequest message
	 * or a CheckPredecessorRequest message.
	 * 
	 * @param message the incoming Ack message.
	 */
	public void ackHandler(Ack message) {
		// get needed runtime parameters
		boolean recursiveMode = (Boolean) parameters.get(ChordUtilities.RECURSIVE_MODE);
		
		if(message.getAckType() == Ack.AckType.FINDSUCCESSOR) {
			// meaning that this is an ACK oid create() {for a FindSuccessorRequest message
			// this ACK can be received only in Recursive mode
			assert(recursiveMode);
			SentMessage sentReq = lookupLocalTimeouts.remove(message.getRequestId());
			if(sentReq == null) {
				// meaning that this is an ACK received from a node n' which n thought was crashed
				expiredRequestsCounter++;
			}
		} else if(message.getAckType() == Ack.AckType.CHECKPREDECESSOR) {
			// meaning that this is an ACK for a CheckPredecessor message
			SentMessage sentCheck = predecessorTimeouts.remove(message.getRequestId());
			if(sentCheck == null) {
				// meaning that this is an ACK received from a node n' which n thought was crashed
				expiredRequestsCounter++;
			}
		} else {
			// this should never be executed
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Handles the incoming stabilization request from a node.
	 * 
	 * @param message the incoming StabilizeRequest message.
	 */
	public void stabilizeRequestHandler(StabilizeRequest message) {
		// adds successors list and predecessor
		@SuppressWarnings("unchecked")
		StabilizeReply replyMsg = new StabilizeReply(id, predecessor, (TreeSet<Long>) successors.clone(), message.getMsgId());
		// sends the reply to the sender of the stabilization request
		try {
			Node stabilizeSender = ChordUtilities.getNodeById(message.getSender(), ContextUtils.getContext(this));
			stabilizeSender.receive(replyMsg);
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
	}
	
	/**
	 * Handles the incoming stabilization response related to a StabilizeRequest previously sent.
	 * 
	 * @param message the incoming StabilizeReply message.
	 */
	public void stabilizeReplyHandler(StabilizeReply message) {
		int successorListSize = (Integer) parameters.get(ChordUtilities.SUCCESSOR_LIST_SIZE);
		
		SentMessage sentReq = stabilizeTimeouts.remove(message.getRequestId());
		if(sentReq != null) {
			
			if(Long.compareUnsigned(id, Long.parseLong("-1026933898696583411")) != 0) {
				
			}
			// The predecessor of the successor is null. e.g  when a node joins
			if(message.getPredecessor() != null) {
				// the stabilize reply contains the successors list and predecessor of the n's successor
				// message.getPredecessor() belongs to (id, successor) 
				// n = id, k = message.getPredecessor(), n' = successor
				if((Long.compareUnsigned(id, successor) < 0 && Long.compareUnsigned(id, message.getPredecessor()) < 0 && Long.compareUnsigned(message.getPredecessor(), successor) <= 0) ||
						(Long.compareUnsigned(id, successor) >= 0 && 
						((Long.compareUnsigned(id, message.getPredecessor()) < 0) || (Long.compareUnsigned(message.getPredecessor(), id) <= 0 && Long.compareUnsigned(message.getPredecessor(), successor) <= 0)))) {
					
					// replaces n successors list with the successors list of the old successor
					// need to clear and use addAll because of different comparators between nodes
					successors.clear();
					successors.addAll(message.getSuccessors());
					// prepends the new successor and the sender of the stabilize to the successors list
					successors.add(message.getPredecessor());
					successors.add(message.getSender());
					// removes itself from the successor list
					successors.remove(id);
					if(!successors.isEmpty()) {
						successor = successors.first();
					} else {
						successor = id;
					}
					// ensures the first element of successors list is the new successor 
					
					// removes the last element from successors list: this is done only in the case in which the 
					// successor list of the successor is full. This is not the case for example when a node has
					// already joined the network, its successor list contains only the successor identifier
					if(successors.size() > successorListSize) {
						Long lastSuccessor = successors.last();
						// the list of successors exceeds the max size
						successors.remove(lastSuccessor);
					}
				} else if(Long.compareUnsigned(id, message.getPredecessor()) == 0) {
					// In this case id == message.getPredecessor, it means that the successor has not changed its
					// predecessor. Can happen anyway that my successor list is not
					// updated w.r.t. the successor, for this reason the successor list has to be updated in any case
					
					// replaces n successors list with the successors list of the old successor
					// need to clear and use addAll because of different comparators between nodes
					successors.clear();
					successors.addAll(message.getSuccessors());
					// prepends the new successor to the successors list
					successors.add(message.getSender());
					// removes itself from the successor list
					successors.remove(id);
					if(!successors.isEmpty()) {
						successor = successors.first();
					} else {
						successor = id;
					}

					// removes the last element from successors list: this is done only in the case in which the 
					// successor list of the successor is full. This is not the case for example when a node has
					// already joined the network, its successor list contains only the successor identifier
					if(successors.size() > successorListSize) {
						Long lastSuccessor = successors.last();
						successors.remove(lastSuccessor);
					}
				} else {
					// From experiments, it might be possible. In this situation, the successor's
					// predecessor is located before the node itself on the Ring
					// replaces n successors list with the successors list of the old successor
					// need to clear and use addAll because of different comparators between nodes
					successors.clear();
					successors.addAll(message.getSuccessors());
					// prepends the new successor to the successors list
					successors.add(message.getSender());
					// removes itself from the successor list
					successors.remove(id);
					if(!successors.isEmpty()) {
						successor = successors.first();
					} else {
						successor = id;
					}
					
					// removes the last element from successors list: this is done only in the case in which the 
					// successor list of the successor is full. This is not the case for example when a node has
					// already joined the network, its successor list contains only the successor identifier
					if(successors.size() > successorListSize) {
						Long lastSuccessor = successors.last();
						successors.remove(lastSuccessor);
					}
				}
			} else {
				// It means that the successor's predecessor is null
				// needed because it is possible that I have stored a wrong successor because some node has failed
				// replaces n successors list with the successors list of the old successor
				// need to clear and use addAll because of different comparators between nodes
				successors.clear();
				successors.addAll(message.getSuccessors());
				// prepends the new successor to the successors list
				successors.add(message.getSender());
				// removes itself from the successor list
				successors.remove(id);
				if(!successors.isEmpty()) {
					successor = successors.first();
				} else {
					successor = id;
				}
				
				// removes the last element from successors list: this is done only in the case in which the 
				// successor list of the successor is full. This is not the case for example when a node has
				// already joined the network, its successor list contains only the successor identifier
				if(successors.size() > successorListSize) {
					Long lastSuccessor = successors.last();
					successors.remove(lastSuccessor);
				}
			}
			// the id of the node is already contained in sender field of the message
			NotifySuccessor notifyMsg = new NotifySuccessor(id);
			// the successor may be changed due to the previous step
			try {
				Node succNode = ChordUtilities.getNodeById(successor, ContextUtils.getContext(this));
				succNode.receive(notifyMsg);
			} catch(NoSuchElementException e) {
				// the destination node has failed
			}
		} else {
			// meaning that this is a message received from a node n' which n thought was crashed
			expiredRequestsCounter++;
		}
	}
	
	/**
	 * Handles the notification coming from a node which believes it's the node's predecessor.
	 *  
	 * @param message the incoming NotifySuccessor message.
	 */
	public void notifySuccessorHandler(NotifySuccessor message) {
		// get needed runtime parameters
		boolean notifyPredecessorOptimization = (Boolean) parameters.get(ChordUtilities.NOTIFY_PREDECESSOR_OPTIMIZATION);
		// (predecessor == null || message.getSender() belongs to (predecessor, id)
		if(predecessor == null || 
				((Long.compareUnsigned(predecessor, id) < 0 && Long.compareUnsigned(predecessor, message.getSender()) < 0 && Long.compareUnsigned(message.getSender(), id) < 0)) || 
				(Long.compareUnsigned(predecessor, id) >= 0 && ((Long.compareUnsigned(predecessor, message.getSender()) < 0) || (Long.compareUnsigned(message.getSender(), predecessor) <= 0 && Long.compareUnsigned(message.getSender(), id) <= 0)))) {
			
			Long oldPredecessor = predecessor;
			predecessor = message.getSender();
				
			if(successor == id) {
				// this is the case of the node which initiates the network, 
				// the ring has to be closed. The figenr table will be adjusted through
				// fixFinger calls
				successor = predecessor;
			}
			if(notifyPredecessorOptimization && oldPredecessor != null) {
				// sends a notification to the old predecessor signaling that n's predecessor has changed
				NotifyPredecessor notifyMsg = new NotifyPredecessor(id, predecessor);
				try {
					Node oldPredNode = ChordUtilities.getNodeById(oldPredecessor, ContextUtils.getContext(this));
					oldPredNode.receive(notifyMsg);
				} catch(NoSuchElementException e) {
					// the destination node has failed
				}
			}
		}	
	}
	
	/**
	 * Handles the notification coming from the node's successor which informs the node that
	 * it has changed predecessor. 
	 * 
	 * @param message the incoming NotifyPredecessor message.
	 */
	public void notifyPredecessorHandler(NotifyPredecessor message) {
		// get needed runtime parameters
		boolean notifyPredecessorOptimization = (Boolean) parameters.get(ChordUtilities.NOTIFY_PREDECESSOR_OPTIMIZATION);
		int successorListSize = (Integer) parameters.get(ChordUtilities.SUCCESSOR_LIST_SIZE);
		
		// if a node receives a NotifyPredecessor message, the corresponding optimization must be enabled
		assert(notifyPredecessorOptimization);
		
		// the successor's predecessor is within the node and the node's successor
		// n = id, k = message.getNewPredecessor(), n' = successor
		if((Long.compareUnsigned(id, successor) < 0 && Long.compareUnsigned(id, message.getNewPredecessor()) < 0 && Long.compareUnsigned(message.getNewPredecessor(), successor) <= 0) ||
				(Long.compareUnsigned(id, successor) >= 0 && 
				((Long.compareUnsigned(id, message.getNewPredecessor()) < 0) || (Long.compareUnsigned(message.getNewPredecessor(), id) <= 0 && Long.compareUnsigned(message.getNewPredecessor(), successor) <= 0)))) {
			// the successor has changed
			successor = message.getNewPredecessor();
		}
		
		// elements inside successors list are ordered based on identifier space
		// prepends the new successor to the successors list
		successors.add(message.getNewPredecessor());
		successor = successors.first();
		// remove the node if it is contained inside its list of successors
		successors.remove(id);
		if(successors.size() > successorListSize) {
			// removes the last element from successors list
			Long lastSuccessor = successors.last();
			// the list of successors exceeds the max size
			successors.remove(lastSuccessor);
		}
	}
	
	/**
	 * Handles the message coming from the node successor to verify whether the node has crashed or not.
	 * 
	 * @param message the incoming CheckPredecessor message.
	 */
	public void checkPredecessorHandler(CheckPredecessor message) {	
		// the current node n sends an ACK to its successor signaling that it has not failed
		Ack ackMsg = new Ack(id, message.getMsgId(), Ack.AckType.CHECKPREDECESSOR);
		try {
			Node senderNode = ChordUtilities.getNodeById(message.getSender(), ContextUtils.getContext(this));
			senderNode.receive(ackMsg);
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
	}
	
	/**
	 * Handles the message coming from a node which is voluntarily leaving the Chord ring. The sender
	 * of this message can be either the predecessor or the successor.
	 * 
	 * @param message the incoming VoluntaryLeave message.
	 */
	public void voluntaryLeaveHandler(VoluntaryLeave message) {
		if(predecessor != null && Long.compareUnsigned(message.getSender(), predecessor) == 0) {
			// sender == predecessor, meaning that current node n is the successor of the node
			// which left the Chord ring
			// current node n replaces its predecessor with the predecessor of the node which left the
			// Chord ring
			predecessor = message.getPredecessor();
		} else if(successor != null && Long.compareUnsigned(message.getSender(), successor) == 0) {
			// sender == successor, meaning that current node n is the predecessor of the node
			// which left the Chord ring
			// current node n replaces its successor list with the one of the node which left the
			// Chord ring and sets successor accordingly
			// need to clear and use addAll because of different comparators between nodes
			
			// It is possible that the successor list of the successor is empty, in boundary case
			// I maintain the old successor and successorList and I treat the voluntaryLeav as a failure
			// (if the node has a null successor it has no possibility to restore its state)
			if(!message.getSuccessors().isEmpty()) {
				successors.clear();
				successors.addAll(message.getSuccessors());
				successor = successors.first();
				// remove the node itself from the list of successors
				successors.remove(id);
			} else {
				Long failedNodeId = message.getSender();
				// replace the leaved successor with the first element in the successor list
				if(!successors.isEmpty()) {
					successor = successors.ceiling(failedNodeId + 1);
					if(successor == null) {
						// the list of successors contains only the successor which has failed
						successor = id;
					}
				}
				// we need to remove the successor which has leaved from the successor list
				successors.remove(failedNodeId);
			}
		} else {
			// this should be executed only if the message is received after the current node n
			// has changed predecessor and/or successor
		}
	}
	
	public void joinRequestHandler(JoinRequest message) {
		Long targetId = message.getSender();
		findSuccessor(targetId, ReqType.JOIN);
	}
	
	public void joinReplyHandler(JoinReply message) {
		// get needed runtime parameters
		int keySize = (Integer) parameters.get(ChordUtilities.KEY_SIZE);
		// updates successor
		this.successor = message.getSuccessor();
		// updates also the finger table
		if(Long.compareUnsigned(id, successor) < 0) {
			// id < successor
			for(int i = 0; i < keySize; i++) {
				Long fingerToFind = BigInteger.valueOf(2).pow(i).add(BigInteger.valueOf(id)).longValue();
				// fingerToFind <= successor
				if(Long.compareUnsigned(fingerToFind, successor) <= 0) {
					// meaning that the successor is the successor of the i-th finger table entry
					fingerTable[i] = successor;
				}
			}
		} else {
			// id > successor
			for(int i = 0; i < keySize; i++) {
				Long fingerToFind = BigInteger.valueOf(2).pow(i).add(BigInteger.valueOf(id)).longValue();
				// fingerToFind < 2^64
				if(Long.compareUnsigned(fingerToFind, BigInteger.valueOf(2).pow(keySize).longValue()) < 0) {
					fingerTable[i] = successor;
				} else if(Long.compareUnsigned(fingerToFind, successor) <= 0) {
					// fingerToFind <= successor
					fingerTable[i] = successor;
				}
			}
		}	
		// adds the successor inside its successor list
		successors.add(successor);
		// if it is contained, remove the node itself from its list of successors
		successors.remove(id);
		NotifySuccessor notifyMsg = new NotifySuccessor(id);
		try {
			Node successorNode = ChordUtilities.getNodeById(successor, ContextUtils.getContext(this));
			successorNode.receive(notifyMsg);
		} catch(NoSuchElementException e) {
			// the destination node has failed
		}
		isNewNode = false;
		joinedNodesId.add(id);
	}
	/**
	 * Checks for each element in the LookupLocalTimeouts table whether there is an expired request,
	 * if this is the case, the corresponding entry is removed
	 */
	public void checkLookupLocalTimeouts() {
		// get needed runtime parameters
		int localTimeout = (Integer) parameters.get(ChordUtilities.LOCAL_TIMEOUT);
		boolean iterativeMode = (Boolean) parameters.get(ChordUtilities.ITERATIVE_MODE);
		boolean recursiveMode = (Boolean) parameters.get(ChordUtilities.RECURSIVE_MODE);
		
		double currentTick = ChordUtilities.getCurrentTick();
		HashMap <UUID, SentMessage> newEntries = new HashMap<>();
		Iterator<Entry<UUID, SentMessage>> it = lookupLocalTimeouts.entrySet().iterator();
		while(it.hasNext()) {
			Entry<UUID, SentMessage> pair = it.next();
			if(currentTick - pair.getValue().getTick() > localTimeout) {
				// meaning that the timeout of a local lookup has expired
				// this implies that the node to which we sent a request has failed
				it.remove();
				
				FindSuccessorRequest failedReq = (FindSuccessorRequest) pair.getValue().getMessage();
				Long failedNodeId = failedReq.getDestinationId();
				
				// meaning that the successor has failed
				if(Long.compareUnsigned(failedNodeId, successor) == 0) {
					// this happens only when the successor is the closest preceding node. If
					// the successor is the successor of the key we are looking for, the 
					// current node will never send a request to it.
					// replaces the successor with the 2nd element in the successor list
					if(!successors.isEmpty()) {
						successor = successors.ceiling(failedNodeId + 1);
						
						if(successor == null) {
							// the list of successors contains only the successor which has failed
							successor = id;
						}
					} else {
						// the list of successor does not contain any element, replace the successor
						// with the node itself
						successor = id;
					}
				} 
				
				// remove the failed node from successor list
				successors.remove(failedNodeId);
				
				// remove the failed node from finger table
				Long nextFinger = fingerTable[0];
				for(int i = fingerTable.length - 1; i >= 0; i--) {
					if(fingerTable[i] == failedNodeId) {
						fingerTable[i] = nextFinger;
					} else {
						nextFinger = fingerTable[i];
					}
				}
				
				// re-trasmission of the request
				if(successorHasKey(failedReq.getTargetId())) {
					// the successor holds the identifier the initiator is looking for
					if(Long.compareUnsigned(id, failedReq.getInitiatorId()) == 0) {
						// the current node is the initiator of the lookup, this can happen
						// both in iterative and recursive mode
						SentMessage sentLookup = lookupGlobalTimeouts.remove(failedReq.getMsgId());
						if(sentLookup != null) {
							// meaning that the global timeout has not expired
							handleLookupType(sentLookup, successor);
						} else {
							// meaning that the global timeout has expired for this lookup
						}
					} else {
						// the successor holds the identifier the initiator of this request is looking for
						// the current node is not the initiator of the lookup, this can happen only in recursive 
						// mode since the entries in lookupLocalTimeouts are always added by the initiator in 
						// iterative mode
						assert(recursiveMode);
						
						FindSuccessorReply replyMsg = new FindSuccessorReply(id, failedReq.getLookupId(), successor, FindSuccessorReply.NodeType.SUCCESSOR);
						try {
							Node initiatorNode = ChordUtilities.getNodeById(failedReq.getInitiatorId(), ContextUtils.getContext(this));
							initiatorNode.receive(replyMsg);
							if(failedReq.getTargetId() == visualizer.getTargetId()) 
								visualizer.addLink(this, initiatorNode, EdgeType.REPLY_SUCCESSOR);
						} catch(NoSuchElementException e) {
							// the destination node has failed
						}
					}
				} else {
					// the successor does not hold the identifier the initiator is looking for
					// find the closest preceding node of the failed one
					Long intermediateId = closestPrecedingNode(failedNodeId);
					
					if(iterativeMode) {
						FindSuccessorRequest requestMsg = new FindSuccessorRequest(id, id, failedReq.getTargetId(), failedReq.getLookupId(), failedReq.getReqType(), null, intermediateId);
						try {
							Node intermediateNode = ChordUtilities.getNodeById(intermediateId, ContextUtils.getContext(this));
							intermediateNode.receive(requestMsg);
							if(failedReq.getTargetId() == visualizer.getTargetId()) 
								visualizer.addLink(this, intermediateNode, EdgeType.REPLY);
						} catch(NoSuchElementException e) {
							// the destination node has failed
						}
						
						SentMessage sentMsg = new SentMessage(requestMsg, ChordUtilities.getCurrentTick());
						newEntries.put(requestMsg.getMsgId(), sentMsg);
					} else {
						// meaning that the protocol is running in recursive mode
						FindSuccessorRequest requestMsg = new FindSuccessorRequest(id, failedReq.getInitiatorId(), failedReq.getTargetId(), failedReq.getLookupId(), failedReq.getReqType(), failedReq.getFingerToFix(), intermediateId);
						try {
							Node intermediate = ChordUtilities.getNodeById(intermediateId, ContextUtils.getContext(this));
							intermediate.receive(requestMsg);
							if(failedReq.getTargetId() == visualizer.getTargetId()) 
								visualizer.addLink(this, intermediate, EdgeType.REQUEST);
						} catch(NoSuchElementException e) {
							// the destination node has failed
						}
						
						SentMessage sentMsg = new SentMessage(requestMsg, ChordUtilities.getCurrentTick());
						newEntries.put(requestMsg.getMsgId(), sentMsg);
					}
				}
			}
		}
		lookupLocalTimeouts.putAll(newEntries);
	}

	/**
	 * Checks for each element in the LookupGlobalTimeouts table whether there is an expired request,
	 * if this is the case, the corresponding entry is removed
	 */
	public void checkLookupGlobalTimeouts() {
		// get needed runtime parameters
		int globalTimeout = (Integer) parameters.get(ChordUtilities.GLOBAL_TIMEOUT);
		
		double currentTick = ChordUtilities.getCurrentTick();
		Iterator<Entry<UUID, SentMessage>> it = lookupGlobalTimeouts.entrySet().iterator();
		while(it.hasNext()) {
			Entry<UUID, SentMessage> pair = it.next();
			if(currentTick - pair.getValue().getTick() > globalTimeout) {
				// meaning that the timeout of a global lookup has expired
				FindSuccessorRequest oldRequest = (FindSuccessorRequest) pair.getValue().getMessage();
				visualizer.notifyEndedLookup(oldRequest.getTargetId());
				it.remove();
			}
		}
	}
	
	/**
	 * Checks for each element in the StabilizeTimeouts table whether there is an expired request, 
	 * if this is the case, the corresponding entry is removed.
	 */
	public void checkStabilizeTimeouts() {
		// get needed runtime parameters
		int stabilizeTimeout = (Integer) parameters.get(ChordUtilities.STABILIZE_TIMEOUT);
		
		double currentTick = ChordUtilities.getCurrentTick();
		HashMap<UUID, SentMessage> newStabilizeMessages = new HashMap<>();
		Iterator<Entry<UUID, SentMessage>> it = stabilizeTimeouts.entrySet().iterator();
		while(it.hasNext()) {
			Entry<UUID, SentMessage> pair = it.next();
			
			StabilizeRequest failedReq = (StabilizeRequest) pair.getValue().getMessage();
			Long failedSuccId = failedReq.getDestinationId();
			
			if(currentTick - failedReq.getTick() > stabilizeTimeout) {
				// meaning that the timeout of a stabilize request has expired
				// this implies that the successor to which we sent a stabilize request has failed
				it.remove();
				
				// replaces the successor with the 2nd element in the successor list
				if(!successors.isEmpty()) {
					successor = successors.ceiling(failedSuccId + 1);
					if(successor == null) {
						successor = id;
					}
					// we need to remove the failed successor
					successors.remove(failedSuccId);
				} else {
					// the list of successor does not contain any element, replace the successor
					// with the node itself
					successor = id;
				}
				
				if(Long.compareUnsigned(id, successor) != 0) {
					// sends a notify to the new successor
					NotifySuccessor notifySuccessorMsg = new NotifySuccessor(id);
					try {
						Node succNode = ChordUtilities.getNodeById(successor, ContextUtils.getContext(this));
						succNode.receive(notifySuccessorMsg);
					} catch(NoSuchElementException e) {
						// the destination node has failed
					}
				}
			}
		}
		stabilizeTimeouts.putAll(newStabilizeMessages);
	}
	
	/**
	 * Checks for each element in the PredecessorTimeouts table whether there is an expired request, 
	 * if this is the case, the corresponding entry is removed.
	 */
	public void checkPredecessorTimeouts() {
		// get needed runtime parameters
		int predecessorTimeout = (Integer) parameters.get(ChordUtilities.PREDECESSOR_TIMEOUT);
		
		double currentTick = ChordUtilities.getCurrentTick();
		Iterator<Entry<UUID, SentMessage>> it = predecessorTimeouts.entrySet().iterator();
		while(it.hasNext()) {
			Entry<UUID, SentMessage> pair = it.next();
			if(currentTick - pair.getValue().getTick() > predecessorTimeout) {
				// meaning that the timeout of a check predecessor has expired
				// this implies that the predecessor to which we sent a check request has failed
				it.remove();
				predecessor = null;
			}
		}
	}
	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
    }
	
	/**
	 * @param successor the successor to set
	 */
	public void setSuccessor(Long successor) {
		this.successor = successor;
	}

	/**
	 * @return the successor
	 */
	public Long getSuccessor() {
		return successor;
	}

	/**
	 * @param successors the successors to set
	 */
	public void setSuccessors(TreeSet<Long> successors) {
		// must be called only by ChordBuilder due to different nodes' comparators
		this.successors = successors;
	}

	/**
	 * @param predecessor the predecessor to set
	 */
	public void setPredecessor(Long predecessor) {
		this.predecessor = predecessor;
	}

	/**
	 * @return the successors
	 */
	public TreeSet<Long> getSuccessors() {
		return successors;
	}

	/**
	 * @return the predecessor
	 */
	public Long getPredecessor() {
		return predecessor;
	}
	
	/**
	 * @return the fingerTable
	 */
	public Long[] getFingerTable() {
		return fingerTable;
	}

	/**
	 * @return the isNewNode
	 */
	public boolean isNewNode() {
		return isNewNode;
	}
	
	
}
	