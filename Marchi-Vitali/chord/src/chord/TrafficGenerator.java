package chord;

import java.util.HashSet;
import java.util.UUID;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import util.Key;
import util.Util;

public class TrafficGenerator {
	
	private HashSet<ChordNode> nodes = new HashSet<>();
	private int observedNodeAddress;
	private ChordNode observedNode;

	public TrafficGenerator() {
		observedNodeAddress = RandomHelper.getSeed();
	}
	
	public void addNode(ChordNode newNode) {
		nodes.add(newNode);
		if (newNode.getAddress() == observedNodeAddress) {
			observedNode = newNode;
		}
	}
	
	public void removeNode(ChordNode nodeToRemove) {
		nodes.remove(nodeToRemove);
		if (nodeToRemove.getAddress() == observedNodeAddress) {
			observedNode = null;
		}
	}
	
	// Generate three lookup request to random nodes for random keys 
	@ScheduledMethod(start = 1, interval = 1, priority = 20)
	public void step() {
		if (Util.getTick() > 200) {
			for (int i = 0; i < 3; i++) {
				ChordNode randomNode = Util.extractRandom(nodes);
				UUID randomKey = UUID.randomUUID();
				randomNode.lookup(new Key(randomKey));
			}
		}
	}
	
	@ScheduledMethod(start = 1, interval = 100, priority = 30)
	public void step2() {
		if (Util.getTick() > 200 && observedNode != null) {
			observedNode.lookup(new Key(UUID.randomUUID()));
		}
	}

}
