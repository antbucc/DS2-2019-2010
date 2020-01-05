/**
 * 
 */
package utils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import chord.Node;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.collections.IndexedIterable;

/**
 * Represents a Utility Class which contains methods which can be reused across the application.
 * 
 * @author zanel
 * @author coffee
 *
 */
//final, because it's not supposed to be subclassed
public final class ChordUtilities {
	
	/**
	 * The type of dataset that will be produced as output
	 */
	public enum DatasetType {NONE, LOOKUPS_TIME_PER_ROUND, LOOKUPS_TIME_LEAVE_JOIN, LOOKUPS_STEPS, EXPIRED_REQUESTS, STABILIZATION_PERIOD, STABILIZATION_INTERVAL};
	
	/**
	 * Key to retrieve the initial number of nodes in the network.
	 */
	public static final String INITIAL_NODES_NUMBER = "initialNodesNumber";

	/**
	 * Key to retrieve the size of the successors list
	 */
	public static final String SUCCESSOR_LIST_SIZE = "successorListSize";

	/**
	 * Key to retrieve the mean of the exponential distribution ruling the delays in the network.
	 */
	public static final String NETWORK_DELAY_MEAN = "networkDelayMean";

	
	/**
	 * Key to retrieve whether the protocol is executed in Iterative style or not. 
	 */
	public static final String ITERATIVE_MODE = "iterativeMode";
	
	/**
	 * Key to retrieve whether the protocol is executed in Recursive style or not. 
	 */
	public static final String RECURSIVE_MODE = "recursiveMode";
	
	/**
	 * Key to retrieve whether the optimization in which a node that changes predecessor signals this change 
	 * to its old predecessor is enabled or not.
	 */
	public static final String NOTIFY_PREDECESSOR_OPTIMIZATION = "notifyPredecessorOptimization";
	
	/**
	 * Key to retrieve the number of bits used to represent a key in the chord ring.
	 */
	public static final String KEY_SIZE = "keySize";
	
	/**
	 * Key to retrieve the timeout of a local lookup request.
	 */
	public static final String LOCAL_TIMEOUT = "local_timeout";
	
	/**
	 * Key to retrieve the timeout of a global lookup request.
	 */
	public static final String GLOBAL_TIMEOUT = "global_timeout";
	
	/**
	 * Key to retrieve the timeout of a stabilize request.
	 */
	public static final String STABILIZE_TIMEOUT = "stabilize_timeout";
	
	/**
	 * Key to retrieve the timeout of a check predecessor request.
	 */
	public static final String PREDECESSOR_TIMEOUT = "predecessor_timeout";

	/**
	 *  The probability that a node lookups a key at the current step
	 */
	public static final String LOOKUP_GENERATION_PROBABILITY = "lookup_generation_probability";
	
	/**
	 * Mean of Poisson distribution related to joining nodes
	 */
	public static final String JOIN_MEAN = "join_mean";
	
	/**
	 * Mean of Poisson distribution related to failing nodes
	 */
	public static final String FAILURE_MEAN = "failure_mean";
	
	/**
	 * Mean of Poisson distribution related to voluntary leaving nodes
	 */
	public static final String VOLUNTARY_LEAVE_MEAN = "voluntary_leave_mean";
	
	/**
	 * Time elapsed between two stabilize calls
	 */
	public static final String STABILIZE_INTERVAL = "stabilize_interval";
	
	/**
	 * Time elapsed between two checkPredecessor calls
	 */
	public static final String CHECK_PREDECESSOR_INTERVAL = "check_predecessor_interval";
	
	/**
	 * Time elapsed between two fixFinger calls
	 */
	public static final String FIX_FINGER_INTERVALL = "fix_finger_interval";
	/**
	 * The type of dataset that has to be generated
	 */
	public static final String DATASET_TYPE ="dataset_type";
	
	// private constructor to avoid unnecessary instantiation of the class
    private ChordUtilities() {
    }
    
    /**
	 * Gets the current tick of the simulation.
	 * 
	 * @return the current tick.
	 */
	public static double getCurrentTick() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	} 
	
	/**
	 * Gets the tick during which the message has to be processed, taking into
	 * account the network delay.
	 * 
	 * @return the delayed tick.
	 */
	public static double getDelayedTick(float networkDelayMean) {
		// tick from which the message can be processed by the receiver
		double delayedTick = getCurrentTick();
		double delay = 0.0;
		// a networkDelayMean equals to 0 means that messages arrive in the next round
		if(networkDelayMean > 0.0) {
			delay = Math.abs(RandomHelper.createExponential(networkDelayMean).nextDouble());
		}
		delayedTick = delayedTick + 1 + delay;
		return delayedTick;
	}
	
	/**
	 * Gets the reference of the node from its identifier.
	 * 
	 * @param nodeId the id of the node to be retrieved.
	 * @return the node with the given id if it exists, null otherwise.
	 */
	public static Node getNodeById(Long nodeId, Context context) throws NoSuchElementException{
		Node target = null;
		IndexedIterable<Node> collection =  context.getObjects(Node.class);
		Iterator<Node> iterator = collection.iterator();
		while(iterator.hasNext() & target == null) {
			Node node = iterator.next();
			if(Long.compareUnsigned(node.getId(),nodeId) == 0) {
				target = node;
			}
		}
		if(target == null) {
			throw new NoSuchElementException("The node is not present in the network");
		} else {
			return target;
		}
	}
	
	/**
	 * Convert a 160 bit array to a long value (max 64 bit value) of length KEY_SIZE applying mod(KEY_SIZE) on the original sequence of bits
	 * 
	 * @return the converted long value
	 */
	public static Long convertIntoKeySpace(byte[] byteSequence, int keySpace) throws IllegalArgumentException{
		BigInteger entireValue = new BigInteger(1, byteSequence);
		BigInteger convertedKey = entireValue.mod(BigInteger.valueOf(2).pow(keySpace));
		if(convertedKey.bitLength() <= 64) {
			return convertedKey.longValue();
		} else {
			throw new IllegalArgumentException("The keySpace has to be maximum 64 bit (the bit length of a unsigned long variable)");
		}
	}
	
	
	public static HashMap<String, Object> getParameters() {
		Parameters runTimeParams = RunEnvironment.getInstance().getParameters();
		HashMap<String, Object> parameters = new HashMap<>();
		
		// The initial number of nodes in the network
		Integer initialNodesNumber = runTimeParams.getInteger("nodes_number");
		// Mean of the Exponential distribution ruling the delays in the network
		Float networkDelayMean = runTimeParams.getFloat("network_delay_mean");
		// Iterative version of the protocol
		Boolean iterativeMode = runTimeParams.getString("protocol_mode").equals("Iterative");
		// Recursive version of the protocol
		Boolean recursiveMode = runTimeParams.getString("protocol_mode").equals("Recursive");
		assert(iterativeMode != recursiveMode);
		// Whether the optimization in which a node that changes predecessor signals this change to its old predecessor
		// is enabled or not
		Boolean notifyPredecessorOptimization = runTimeParams.getBoolean("notify_predecessor_optimization");
		// The number of bits used to represent a key
		Integer keySize = 64;
		// The size of the successor list
		Integer successorListSize = runTimeParams.getInteger("successor_list_size");
		// The timeout of a local lookup
		Integer localTimeout = runTimeParams.getInteger("local_timeout");
		// The timeout of a global lookup
		Integer globalTimeout = runTimeParams.getInteger("global_timeout");
		// The timeout of a stabilize request
		Integer stabilizeTimeout = runTimeParams.getInteger("stabilize_timeout");
		// The timeout of a check predecessor
		Integer predecessorTimeout = runTimeParams.getInteger("predecessor_timeout");
		// The probability that a considered node lookups a key at the current step
		Float lookupGenerationProbability = runTimeParams.getFloat("lookup_generation_probability");
		// Mean of Poisson distribution related to failing nodes
		Float failureMean = runTimeParams.getFloat("failure_mean");
		// Mean of Poisson distribution related to joining nodes
		Float joinMean = runTimeParams.getFloat("join_mean");
		// Mean of Poisson distribution related to voluntary leaving nodes
		Float voluntaryLeaveMean = runTimeParams.getFloat("voluntary_leave_mean");
		// Time elapsed between two stabilize calls
		Integer stabilizeInterval = runTimeParams.getInteger("stabilize_interval");
		// Time elapsed between two checkPredecessor calls
		Integer checkPredecessorInterval = runTimeParams.getInteger("check_predecessor_interval");	
		// Time elapsed between tow fixFinger calls
		Integer fixFingerInterval = runTimeParams.getInteger("fix_finger_interval");
		// The dataset that will be produced as output
		DatasetType datasetType = null;
		if(runTimeParams.getString("dataset_type").equals("None")) {
			datasetType = DatasetType.NONE;
		} else if(runTimeParams.getString("dataset_type").equals("Lookups-Time-Stable")) {
			datasetType = DatasetType.LOOKUPS_TIME_PER_ROUND;
		} else if(runTimeParams.getString("dataset_type").equals("Lookups-Time-Unstable")){ 
			datasetType = DatasetType.LOOKUPS_TIME_LEAVE_JOIN;
		}else if(runTimeParams.getString("dataset_type").equals("Lookup-Steps")) {
			datasetType = DatasetType.LOOKUPS_STEPS;
		} else if(runTimeParams.getString("dataset_type").equals("Expired-Requests")) {
			datasetType = DatasetType.EXPIRED_REQUESTS;
		} else if(runTimeParams.getString("dataset_type").equals("Stabilization-Period")) {
			datasetType = DatasetType.STABILIZATION_PERIOD;
		} else if(runTimeParams.getString("dataset_type").equals("Stabilization-Interval")) {
			datasetType = DatasetType.STABILIZATION_INTERVAL;
		} else {
			assert(false);
		}
		
		parameters.put(INITIAL_NODES_NUMBER, initialNodesNumber);
		parameters.put(NETWORK_DELAY_MEAN, networkDelayMean);
		parameters.put(ITERATIVE_MODE, iterativeMode);
		parameters.put(RECURSIVE_MODE, recursiveMode);
		parameters.put(NOTIFY_PREDECESSOR_OPTIMIZATION, notifyPredecessorOptimization);
		parameters.put(KEY_SIZE, keySize);
		parameters.put(SUCCESSOR_LIST_SIZE, successorListSize);
		parameters.put(LOCAL_TIMEOUT, localTimeout);
		parameters.put(GLOBAL_TIMEOUT, globalTimeout);
		parameters.put(STABILIZE_TIMEOUT, stabilizeTimeout);
		parameters.put(PREDECESSOR_TIMEOUT, predecessorTimeout);
		parameters.put(LOOKUP_GENERATION_PROBABILITY, lookupGenerationProbability);
		parameters.put(FAILURE_MEAN, failureMean);
		parameters.put(JOIN_MEAN, joinMean);
		parameters.put(VOLUNTARY_LEAVE_MEAN, voluntaryLeaveMean);
		parameters.put(STABILIZE_INTERVAL, stabilizeInterval);
		parameters.put(CHECK_PREDECESSOR_INTERVAL, checkPredecessorInterval);
		parameters.put(DATASET_TYPE, datasetType);
		parameters.put(FIX_FINGER_INTERVALL, fixFingerInterval);
		
		return parameters;
	}
}
