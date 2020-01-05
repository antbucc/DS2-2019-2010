package communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import messages.SuccessorRequest;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import util.Logger;
import util.Util;
import util.Visualizer;

public class Channel {
	
	private int maxLatency;
	private int observedNodeAddress;
	
	// Internal state of the channel
	private HashMap<Integer, Node> activeNodes = new HashMap<>();
	private ArrayList<Message> messageBuffer = new ArrayList<>();
	private ArrayList<Message> newMessages = new ArrayList<>();
	
	public Channel(int maxLatency) {
		this.maxLatency = maxLatency;
		// Set the observed node to the first node created 
		// (with address equal to the seed of the simulation)
		this.observedNodeAddress = RandomHelper.getSeed();
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 1000)
	public void step() {
		// Add new messages (generated during last tick) to the buffer
		messageBuffer.addAll(newMessages);
		newMessages.clear();
		
		List<Message> msgsToDeliver = 
				messageBuffer.stream()
					 // Decrease the time left by 1
					 .map(Message::decreaseTimeToDest)
					 // Check if enough ticks have passed from the sending
					 .filter(Message::readyToDeliver)
					 .collect(Collectors.toList());
		
		msgsToDeliver.stream()
					 // Check if the destination is still active
		 			 .filter(msg -> activeNodes.containsKey(msg.getDestAddr()))
					 // Deliver the message to the destination
					 .forEach(msg -> {
						 activeNodes.get(msg.getDestAddr()).receiveMessage(msg);
					 });
		
		messageBuffer.removeAll(msgsToDeliver);
	}
	
	public void sendMessage(Message message) {
		
		Logger.getLogger().logMessage(message);
		
		if (message instanceof SuccessorRequest) {
			SuccessorRequest request = (SuccessorRequest) message;
			if (request.getSourceAddr() == observedNodeAddress && 
				activeNodes.containsKey(request.getDestAddr())) 
			{
				Visualizer.getVisualizer().addEdge(request.getSenderAddr(),
												   activeNodes.get(request.destAddr),
												   0, request.getRequestId());
			}
			
		}		
		
		message.setLatency(RandomHelper.nextIntFromTo(1, maxLatency));
		newMessages.add(message);
	}
	
	public void sendMessageToRandomNode(Message message) {
		for (int i = 0; i < 1000; i++) {
			
			// Extract a random node
			Node randomNode = Util.extractRandom(activeNodes.values());
			
			// Check that the random destination is not the sender 
			if (randomNode.getAddress() != message.getSenderAddr()) {
				message.setDestAddr(randomNode.getAddress());
				sendMessage(message);
				return;
			}
		}
		throw new RuntimeException("Too many tries");
	}
	
	public void registerNode(Node newNode) {
		activeNodes.put(newNode.getAddress(), newNode);
	}
	
	public void removeNode(Node crashedNode) {
		activeNodes.remove(crashedNode.getAddress());
	}
	
}
