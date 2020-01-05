package chord;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import javax.naming.LimitExceededException;

import analyses.Collector;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.StrictBorders;
import repast.simphony.space.graph.Network;
import utils.ChordUtilities;
import utils.ChordUtilities.DatasetType;
import visualization.RingAdder;
import visualization.Visualizer;

public class ChordBuilder implements ContextBuilder<Object> {

	/**
	 * The parameters provided by the user during the simulation.
	 */
	private HashMap<String, Object> parameters; 
	
	/**
	 * The address of the last node added to the network
	 */
	private InetSocketAddress checkpointAddress;
	
	/**
	 * The instance of the support agent data collector used for collect data for the analyses
	 */
	private Collector collector;
	
	/**
	 * The instance of the support agent data collector used for visualise on the display the various informations
	 */
	private Visualizer visual;
	
	/**
	 * The context where the agents are added
	 */
	private Context<Object> context;
	
	/**
	 * Buffer containing all nodes which has joined the network
	 */
	private TreeSet<Long> joinedNodesId;
	
	/**
	 * Increments a pseudo-random byte of the ip address, ensuring that equals addresses cannot be generated
	 * 
	 * @throws LimitExceededException all addresses are generated
	 * @throws UnknownHostException error in parsing the address
	 */
	private void updateNextRandomAddress() throws LimitExceededException, UnknownHostException{
		byte[] addressArr = checkpointAddress.getAddress().getAddress();
		// Declare an arrayList of keys to be randomly chosen, the chosen key will be the index of the address array to be incremented (index 4 corresponds to the port)
		ArrayList<Integer> keys = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
		boolean incremented = false;
		byte[] nextRandomAddress = addressArr;
		int port = checkpointAddress.getPort();
		for(int i = 4; i >= 0; i--) {
			port ++;
			if(port == Math.pow(2, 16)) {
				port = 0;
				nextRandomAddress[3] ++;
			}
			
			if(i < 4 && nextRandomAddress[i] == -1) {
				if(i > 0) {
					nextRandomAddress[i + 1] ++;
				} else {
					throw new LimitExceededException("All possible nodes have been generated");
				}
			}
		}
		
		try {
			checkpointAddress = new InetSocketAddress(InetAddress.getByAddress(nextRandomAddress), port);
		} catch (UnknownHostException e) {
			throw e;
		}
		
	}	

	/**
	 * Initialise the network with pseudo random generated nodes
	 */
	private void initNetwork() {
		// Get needed parameters
		parameters = ChordUtilities.getParameters();
		int successorListSize = (Integer)parameters.get(ChordUtilities.SUCCESSOR_LIST_SIZE);
		int initialNodesNumber = (Integer)parameters.get(ChordUtilities.INITIAL_NODES_NUMBER);
		int keySize = (Integer)parameters.get(ChordUtilities.KEY_SIZE);
		if(initialNodesNumber == 1) {
			Node node = new Node(checkpointAddress, visual, collector, joinedNodesId);
			joinedNodesId.add(node.getId());
			context.add(node);
			node.create();
			try {
				updateNextRandomAddress();
			} catch(LimitExceededException | UnknownHostException e) {
				// do nothing
			}
		} else {
			// orderedNodes contains the nodes ordered by id, a compare function must be override in order to specify that
			TreeSet<Node> orderedNodes = new TreeSet<>(new Comparator<Node>() {
				@Override
				public int compare(Node node1, Node node2) {
					return Long.compareUnsigned(node1.getId(), node2.getId());
				}
			});
			
			// Create nodes with random ip address, and put them into the TreeSet
			for (int i = 0; i < initialNodesNumber; i++) {
				Node currentNode = new Node(checkpointAddress, visual, collector, joinedNodesId);
				orderedNodes.add(currentNode);
				joinedNodesId.add(currentNode.getId());
				context.add(currentNode);
				try {
					updateNextRandomAddress();
				} catch(LimitExceededException | UnknownHostException e) {
					break;
				}
			}

			// Convert the TreeSet into an array in order to simplify and optimise the random access through indexes
			Node[] nodeArray = orderedNodes.toArray(new Node[orderedNodes.size()]);
			// Iterate the nodes in the array adding successor, predecessor and successor list
			for(int i = 0; i < nodeArray.length; i++) {
				ArrayList<Node> successorNodes = new ArrayList<>();
				// Add the successor and the predecessor: Math.floodMod is used in order to compute the mode, 
				// the % operator of java computes the remainder and not the module
				nodeArray[i].setSuccessor(nodeArray[Math.floorMod(i + 1, nodeArray.length)].getId());
				nodeArray[i].setPredecessor(nodeArray[Math.floorMod(i - 1, nodeArray.length)].getId());
				
				// Compute and add successorList
				if(nodeArray.length > successorListSize) {
					// If the successor list is smaller then the number of nodes in the network 
					if(nodeArray.length - i - 1 < successorListSize  && i != nodeArray.length - 1) {
						// if the successors list of the node includes both the last elements of the array and the firsts elements of the array (in between)
						// In this case the last elements and first elements at a time are added
						successorNodes.addAll(Arrays.asList(Arrays.copyOfRange(nodeArray, Math.floorMod(i + 1, nodeArray.length), nodeArray.length)));
						successorNodes.addAll(Arrays.asList(Arrays.copyOfRange(nodeArray, 0, successorListSize - (nodeArray.length - i) + 1)));
					
					} else if(i == nodeArray.length - 1){
						// If the node is in the first index take the first SUCCESSOR_LIST_SIZE nodes as the successor
						successorNodes.addAll(Arrays.asList(Arrays.copyOfRange(nodeArray, 0, successorListSize)));
						
					} else {
						// if the node is not the first element and all the successors are included consecutively in the array, the first SUCCESSOR_LIST_SIZE nodes are chosen
						successorNodes.addAll(Arrays.asList(Arrays.copyOfRange(nodeArray, i + 1, i + successorListSize + 1)));
					}
				} else {
					// If successor list is greater then the number of nodes in the network all nodes but the current node must to be added
					successorNodes.addAll(Arrays.asList(Arrays.copyOfRange(nodeArray, i + 1, nodeArray.length)));
					if(i != 0) {
						successorNodes.addAll(Arrays.asList(Arrays.copyOfRange(nodeArray, 0, i)));
					}
				}
				
				// Add the computed successors to the successors of the node
				for(Node node : successorNodes) {
					nodeArray[i].getSuccessors().add(node.getId());
				}
				
				// Compute finger table
				for(int j = 0; j < keySize; j++) {
					//nodeArray[i] + 2^keysize mod(2^64)
					Long minFinger = BigInteger.valueOf(2)
							.pow(j)
							.add(BigInteger.valueOf(nodeArray[i].getId()))
							.mod(BigInteger.valueOf(2).pow(keySize)).longValue();
					Node tempA = new Node(minFinger);
					Node succeedingNode = orderedNodes.ceiling(tempA);
					
					if(succeedingNode != null) {
						nodeArray[i].getFingerTable()[j] = succeedingNode.getId();
					} else {
						Node tempB = new Node(Long.valueOf(0));
						succeedingNode = orderedNodes.ceiling(tempB);
						nodeArray[i].getFingerTable()[j] = succeedingNode.getId();
					}
				}	
			}
		}
	}
	
	/**
	 * Add a pseudo random node to the network
	 * 
	 * @throws LimitExceededException if all possible addresses have been generated
	 * @throws UnknownHostException error in parsing the address
	 * @throws IllegalStateException No node can join the network, it cannot know any other node. For example because the network is empty
	 */
	private void joinRandomNode() throws LimitExceededException, UnknownHostException, IllegalStateException {
		// take a random node from the context
		if(!joinedNodesId.isEmpty()) {
			try {
				// get a new random address
				updateNextRandomAddress();
				// trasform the hashSet of joined nodes into array and pick a random index
				Long[] joinedNodesIdArr = joinedNodesId.toArray(new Long[joinedNodesId.size()]);
				int randomIndex = RandomHelper.nextIntFromTo(0, joinedNodesIdArr.length - 1);
				// create the new node passing the randomly chosen known node and adding it to the context
				Node joiningNode = new Node(checkpointAddress, visual, collector, joinedNodesId);
				context.add(joiningNode);
				joiningNode.join(joinedNodesIdArr[randomIndex]);
			} catch(LimitExceededException | UnknownHostException e) {
				throw e;
			}
		} else {
			throw new IllegalStateException("No node can join the network, it cannot know any other node");
		}
		
	}
	
	/**
	 * Given the number of nodes which fails and the number of nodes which voluntaryLeave the network simultaneously, the method
	 * provides the correct deletion of them from the network
	 * 
	 * @param failedNodes the nodes which has to fail simultaneously
	 * @param voluntaryLeaveNodes the nodes which has to voluntary leave simultaneously
	 */
	private void leaveRandomNodes(int failedNodes, int voluntaryLeaveNodes) {
		// take a random node from the context
		Iterator<Object> it = context.getRandomObjects(Node.class, failedNodes + voluntaryLeaveNodes).iterator();
		int failedCounter = 0;
		int voluntaryLeaveCounter = 0;
		for(int i = 0; it.hasNext(); i++) {
			Node leavingNode = (Node)it.next();
			if(i % 2 == 0) {
				if(failedCounter < failedNodes) {
					leavingNode.fail();
					failedCounter ++;
				} else if(voluntaryLeaveCounter < voluntaryLeaveNodes) {
					leavingNode.leave();
					voluntaryLeaveCounter ++;
				} 
			} else {
				if(voluntaryLeaveCounter < voluntaryLeaveNodes) {
					leavingNode.leave();
					voluntaryLeaveCounter ++;
				} else if(failedCounter < failedNodes) {
					leavingNode.fail();
					failedCounter ++;
				}
			}
		}
	}
	@Override
	public Context build(Context<Object> context) {
		checkpointAddress = new InetSocketAddress("0.0.0.0", 0);
		this.joinedNodesId = new TreeSet<>(new Comparator<Long>() {
			@Override
			public int compare(Long nodeId1, Long nodeId2) {
				return Long.compareUnsigned(nodeId1, nodeId2);
			}
		});
		NetworkBuilder<Node> builder = new NetworkBuilder("network", context, true);
		Network<Node> network = builder.buildNetwork();
		// Instantiate the support agents
		visual = new Visualizer(network);
		collector = new Collector();
		context.add(visual);
		context.add(collector);
		this.context = context;
		// Initialise runtime parameters
		parameters = ChordUtilities.getParameters();
		
		// Get needed runtime parameters
		int keySize = (Integer) parameters.get(ChordUtilities.KEY_SIZE);
		
		context.setId("chord");
		
		// Compute the dimension of continuousSpace: (normalised key space / pi) + 2
		double dimension = BigDecimal.valueOf(2)
				.pow(keySize)
				.divide(visual.getNormalizeIdParameter(), RoundingMode.HALF_UP)
				.divide(BigDecimal.valueOf(Math.PI), RoundingMode.HALF_UP).doubleValue() + 2;
		
		// Add projections
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, new RingAdder(), new StrictBorders(), dimension + 2, dimension + 2);
		
		
		
		// initialise the network with INITIAL_NODE_NUMBER nodes
		initNetwork();
		RunEnvironment.getInstance().getCurrentSchedule().schedule(ScheduleParameters.createRepeating(1, 1, ScheduleParameters.LAST_PRIORITY), ()-> step());
		return context;
	}
	
	public void step() {
		// Get needed parameters
		Float failureMean = (Float)parameters.get(ChordUtilities.FAILURE_MEAN);
		Float joinMean = (Float)parameters.get(ChordUtilities.JOIN_MEAN);
		Float voluntaryLeaveMean = (Float)parameters.get(ChordUtilities.VOLUNTARY_LEAVE_MEAN);
		DatasetType datasetType = (DatasetType) parameters.get(ChordUtilities.DATASET_TYPE);
		
		int failures = RandomHelper.createPoisson(failureMean).nextInt();
		int joins = RandomHelper.createPoisson(joinMean).nextInt();
		int voluntaryLeaves = RandomHelper.createPoisson(voluntaryLeaveMean).nextInt();
		
		if(datasetType == ChordUtilities.DatasetType.STABILIZATION_PERIOD || datasetType == ChordUtilities.DatasetType.STABILIZATION_INTERVAL) {
			if(ChordUtilities.getCurrentTick() < 500) {
				leaveRandomNodes(failures, voluntaryLeaves);
				
				for(int i = 0; i < joins; i++) {
					try {
						joinRandomNode();
					} catch (LimitExceededException | UnknownHostException | IllegalStateException e) {
						// do nothing
					}
				}
			}
		} else {
			leaveRandomNodes(failures, voluntaryLeaves);
			
			for(int i = 0; i < joins; i++) {
				try {
					joinRandomNode();
				} catch (LimitExceededException | UnknownHostException | IllegalStateException e) {
					// do nothing
				}
			}
		}
	}
}

