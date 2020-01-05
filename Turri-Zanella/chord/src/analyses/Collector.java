package analyses;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import utils.ChordUtilities;
import utils.ChordUtilities.DatasetType;

public class Collector {
	/**
	 *  DataFactory corresponding to the lookups time per round dataset
	 */
	private DatasetFactory lookupsTimePerRoundDataset = null;
	
	/**
	 *  DataFactory corresponding to the lookups steps dataset
	 */
	private DatasetFactory lookupsStepsDataset = null;
	
	/**
	 * Dataset containing (node_id, #expired_requests) pairs
	 */
	private DatasetFactory expiredRequestsDataset = null;
	
	/**
	 * Dataset containing (tick, #non_stabilized_nodes) pairs 
	 */
	private DatasetFactory stabilizationPeriodDataset = null;
	
	/**
	 * Dataset containing (tick, #non_stabilized_nodes) pairs
	 */
	private DatasetFactory stabilizationIntervalDataset = null;
	
	/**
	 * Dataset containing (targetId, initiator, time) entries
	 */
	private DatasetFactory lookupsTimeLeaveJoinDataset = null;
	
	/**
	 *  Corresponds to the chord parameters
	 */
	private HashMap<String, Object> parameters;
	
	/**
	 * Table containing the current pair targetId - step of a particular lookup
	 */
	private HashMap<Long, Integer> lookupSteps;
	
	
	/**
	 * Instantiates a new collector agent
	 */
	public Collector() {
		lookupSteps = new HashMap<>();
		// get needed parameters
		parameters = ChordUtilities.getParameters();
		DatasetType datasetType = (DatasetType)parameters.get(ChordUtilities.DATASET_TYPE);
		Integer nodeNumber = (Integer)parameters.get(ChordUtilities.INITIAL_NODES_NUMBER);
		String mode = (Boolean)parameters.get(ChordUtilities.RECURSIVE_MODE) ? "recursive" : "iterative";
		int localTimeout = (Integer) parameters.get(ChordUtilities.LOCAL_TIMEOUT);
		float joinMean = (Float) parameters.get(ChordUtilities.JOIN_MEAN);
		float leaveMean = (Float) parameters.get(ChordUtilities.VOLUNTARY_LEAVE_MEAN);
		int stabilizeInterval = (Integer) parameters.get(ChordUtilities.STABILIZE_INTERVAL);
		
		String joinMeanStr = String.valueOf(joinMean).replace(".", ",");
		String leaveMeanStr = String.valueOf(leaveMean).replace(".", ",");
		
		// Instantiate the datafactoy corresponding to the dataset chosen by the user
		switch(datasetType) {
			case LOOKUPS_TIME_PER_ROUND:
				lookupsTimePerRoundDataset = new DatasetFactory("lookupsTime." + nodeNumber + "." + mode + ".csv", "Round", "Lookup Time");
				break;
			case LOOKUPS_TIME_LEAVE_JOIN:
				lookupsTimeLeaveJoinDataset = new DatasetFactory("lookupsTime." + mode + "." + joinMeanStr + "." + leaveMeanStr + ".csv", "Round", "Lookup Time");
				break;
			case LOOKUPS_STEPS:
				lookupsStepsDataset = new DatasetFactory("lookupsSteps." + nodeNumber + ".csv", "TargetId", "Steps");
				break;
			case EXPIRED_REQUESTS:
				expiredRequestsDataset = new DatasetFactory("expired_requests." + mode + "." + localTimeout + ".csv", "node_id", "expired_requests");
				break;
			case STABILIZATION_PERIOD:
				stabilizationPeriodDataset = new DatasetFactory("stabilization_period." + mode + "." + joinMeanStr + "." + leaveMeanStr + ".csv", "tick", "non_stabilized_nodes");
				break;
			case STABILIZATION_INTERVAL:
				stabilizationIntervalDataset = new DatasetFactory("stabilization_interval." + mode + "." + stabilizeInterval + ".csv", "tick", "non_stabilized_nodes");
			default:
				// do nothing e.g. the NONE case, no datasetfactory have to be instantiated
		}
	}
	
	/**
	 * Notifies a new Chord Deliver
	 * 
	 * @param targetId the target id that the lookup is related to
	 * @param tick the tick in which the lookup is accomplished
	 * @param time the time needed to resolve the lookup
	 */
	public void notifyChordDeliver(Long targetId, double tick, double time) {
		if(lookupsTimePerRoundDataset != null) {
			// meaning that the lookupsTimePerRound dataset has to be generated
			lookupsTimePerRoundDataset.addEntry(Double.toString(tick), Double.toString(time));
		}
		
		if(lookupsTimeLeaveJoinDataset != null) {
			lookupsTimeLeaveJoinDataset.addEntry(Double.toString(tick), Double.toString(time));
		}
		
		if(lookupsStepsDataset != null) {
			// meaning that the lookupssteps dataset has to be generated
			Integer steps = lookupSteps.get(targetId);
			if(steps != null) {
				lookupsStepsDataset.addEntry(Long.toString(targetId), Integer.toString(steps));
			}
		}
	}
	
	/**
	 * Notifies a new FindSuccessorRequest
	 * 
	 * @param targetId the target id that the request refers to 
	 */
	public void notifyFindSuccessorRequest(Long targetId) {
		if(lookupsStepsDataset != null) {
			// meaning that the lookupssteps dataset has to be generated
			Integer targetCurrentSteps = lookupSteps.putIfAbsent(targetId, 1);
			if(targetCurrentSteps != null) {
				lookupSteps.put(targetId, targetCurrentSteps + 1);
			}
		}
	}
	
	/**
	 * Notifies the receipt of a message coming from a node the current node thought
	 * was failed.
	 * 
	 * @param nodeId the identifier of the node which has received a message coming
	 * from a node it thought was failed.
	 */
	public void notifyExpiredRequest(Long nodeId, int expiredRequests) {
		if(expiredRequestsDataset != null) {
			// meaning that we are collecting data for expired requests analysis
			expiredRequestsDataset.addEntry(Long.toString(nodeId), Integer.toString(expiredRequests));
		}
	}
	
	/**
	 * Notifies the fact that the at the current tick the node has the wrong successor.
	 */
	public void notifyStabilization() {
		if(stabilizationPeriodDataset != null) {
			// meaning that we are collecting data for stabilization period analysis
			double tick = ChordUtilities.getCurrentTick();
			stabilizationPeriodDataset.addEntry(Double.toString(tick), Integer.toString(1));
		} else if(stabilizationIntervalDataset != null) {
			double tick = ChordUtilities.getCurrentTick();
			// meaning that we are collecting data for stabilization interval analysis
			stabilizationIntervalDataset.addEntry(Double.toString(tick), Integer.toString(1));
		}
	}
	
}
