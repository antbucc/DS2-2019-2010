/**
 * 
 */
package visualization;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import chord.Node;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;
import utils.ChordUtilities;
import visualization.Marker.MarkerType;

/**
 * @author zanel
 * @author coffee
 *
 */
public class Visualizer {

	public enum EdgeType {SUCCESSOR, REQUEST, REPLY, ACK, REPLY_SUCCESSOR};
	
	/**
	 * Buffer that stores all the newest lookup, it is needed in order to choose a new lookup to visualise
	 */
	private ConcurrentHashMap<Long, Node> chordLookups;
	
	/**
	 * The initiator of the considered lookup
	 */
	private Long initiator;
	
	/**
	 * The intermediaries which have contributed to find the target before the current id
	 */
	private Set<Long> intermediaries;
	
	/**
	 * The successor list of the initiator 
	 */
	private	TreeSet<Long> successors;
	
	/**
	 * The finger table of the initiator
	 */
	private TreeSet<Long> fingers;
	
	/**
	 * The link which has to be printed
	 */
	private Set<Link> links;
	
	/**
	 * The marker which has to be add to the context
	 */
	private Marker targetMarker;
	
	/*
	 * The number of digit which which the normalised id has to have
	 */
	private static final int NORMALIZE_ID_FACTOR = 4;
	
	/**
	 * The network projection where the edges have to be added
	 */
	private Network<Node> network;
	
	/**
	 * The Id of the considered lookup
	 */
	private Long targetId;
	
	/**
	 * Instantiates the support agent Visualizer which is in charge of collecting all information to be plotted on the display
	 * 
	 * @param network the network projection where the edges have to be added
	 */
	public Visualizer(Network network) {
		// Initialise the local variables
		this.network = network;
		chordLookups= new ConcurrentHashMap<>();
		targetId = null;
		initiator = null;
		successors = new TreeSet<>();
		fingers = new TreeSet<>();
		links = ConcurrentHashMap.newKeySet();
		intermediaries = ConcurrentHashMap.newKeySet();
		
		
	}

	/**
	 * @return the parameter used to normalise the id, the "convertedId" used for the ring visualisation is obtained dividing it
	 * by this parameter
	 */
	public static BigDecimal getNormalizeIdParameter() {
		// Get needed parameters
		HashMap<String, Object> parameters = ChordUtilities.getParameters();
		int keySize = (int) parameters.get(ChordUtilities.KEY_SIZE);
		
		// Compute normalizedIdParameter
		int normalizedSize = BigInteger.valueOf(BigDecimal.valueOf(2)
				.pow(keySize).toString().length())
				.subtract(BigInteger.valueOf(NORMALIZE_ID_FACTOR)).intValue();
		
		BigDecimal normalizeIdParameter = normalizedSize > 0 ? new BigDecimal(10).pow(normalizedSize) : new BigDecimal(1);
		return normalizeIdParameter;
	}
	
	/**
	 * Stores the new lookup in the lookup buffer, it will a possible candidate for the next visualised 
	 * lookup
	 * 
	 * @param targetId the id to looking for
	 * @param initiator the initiator of the lookup
	 */
	public void notifyChordLookup(Long targetId, Node initiator) {
		chordLookups.put(targetId, initiator);
	}
	
	/**
	 * Notifies that the current lookup has been accomplished 
	 */
	public void notifyEndedLookup(Long targetId) {
		chordLookups.remove(targetId);
		if(targetId == this.targetId) {
			//Remove the marker
			ContextUtils.getContext(this).remove(targetMarker);
			// Reset the display
			this.targetMarker = null;
			this.targetId = null;
			this.intermediaries.clear();
			this.successors.clear();
			this.initiator = null;
		
		}
	}
	
	
	/**
	 * Add to to the links buffer all the links which have to be stored
	 * 
	 * @param source the node from which the arrow begin
	 * @param target the node where the arrow ends
	 * @param type the type of the arrow
	 */
	public void addLink(Node source, Node target, EdgeType type) {
		links.add(new Link(source, target, type));
	}
	
	@ScheduledMethod(start=0, interval=1, priority=ScheduleParameters.FIRST_PRIORITY)
	public void step() {
		//Remove all edges from the display, they are updated in this step
		network.removeEdges();
		// Get the successor of every node and print the arrow
		Iterator<Node> it = ContextUtils.getContext(this).getObjects(Node.class).iterator();
		while(it.hasNext()) {
			Node currentNode = it.next();
			
			// Meaning that the node has not a successor, e.g. it is joining the network
			if(currentNode.getSuccessor() != null) {
				try {
					Node successor = ChordUtilities.getNodeById(currentNode.getSuccessor(), ContextUtils.getContext(this));
					network.addEdge(currentNode, successor, EdgeType.SUCCESSOR.ordinal());
				} catch(NoSuchElementException e) {
					// the successor has failed
				}
			}
		}
		
		// If no targetId is selected choose a new one
		if(targetId == null) {
			// Check if at this step some node has performed a new lookup
			if(chordLookups.size() != 0) {
				// choose randomly a lookup from the chordLookups buffer
				Long[] chordLookupsArr = chordLookups.keySet().toArray(new Long[chordLookups.size()]);
				int randomIndex = RandomHelper.nextIntFromTo(0, chordLookupsArr.length - 1);
				targetId = chordLookupsArr[randomIndex];
				
				// Set the successors, fingers and initiator which have to be shown
				setSuccessors(chordLookups.get(targetId).getSuccessors());
				setFingers(chordLookups.get(targetId).getFingerTable());
				initiator = chordLookups.get(targetId).getId();
				
				// Add the related new marker to the context
				targetMarker = new Marker(targetId, MarkerType.TARGET);
				ContextUtils.getContext(this).add(targetMarker);
			}
		} else {
			// In any case the edges have to be updated according to the current step
			for(Link entry : this.links) {
				try {
					this.network.addEdge(entry.source, entry.target, Double.valueOf(entry.type.ordinal()));
				} catch(Exception e) {
					// some process is removed from the context, I can not show that edge
				}
			}
		}
		
		// At every step, the buffer links has to be emptied and all edged resetted
		chordLookups.clear();
		links.clear();
		
	}

	/**
	 * @return the successors
	 */
	public TreeSet<Long> getSuccessors() {
		return successors;
	}

	/**
	 * @param successors the successors to set
	 */
	public void setSuccessors(TreeSet<Long> successors) {
		this.successors = successors;
	}
	
	/**
	 * @param successors the fingers to set
	 */
	public void setFingers(Long[] fingers) {
		this.fingers = new TreeSet<>(Arrays.asList(fingers));
	}
	

	/**
	 * @return the fingers
	 */
	public TreeSet<Long> getFingers() {
		return fingers;
	}

	/**
	 * @return the targetId
	 */
	public Long getTargetId() {
		return targetId;
	}

	/**
	 * @return the initiator
	 */
	public Long getInitiator() {
		return initiator;
	}
	
	
	/**
	 * @return the intermediators
	 */
	public Set<Long> getIntermediators() {
		return intermediaries;
	}



	class Link {
		public Node source;
		public Node target;
		public Visualizer.EdgeType type;
		
		public Link(Node source, Node target, Visualizer.EdgeType type) {
			this.source = source;
			this.target = target;
			this.type = type;
		}
	}
	
	
	
	
	
}