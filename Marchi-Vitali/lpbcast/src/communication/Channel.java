package communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import repast.simphony.engine.schedule.ScheduledMethod;
import util.Logger;

public class Channel {
	
	private static Random random = new Random();
	
	// Parameters of the channel
	private int maxLatency;
	private int probNetFail;
	
	// Internal state of the channel
	private HashMap<Integer, Node> activeNodes = new HashMap<>();
	private ArrayList<Message> messageBuffer = new ArrayList<>();
	private ArrayList<Message> newMessages = new ArrayList<>();
	
	public Channel(int maxLatency, int probNetFail) {
		this.maxLatency = maxLatency;
		this.probNetFail = probNetFail;
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 100)
	public void step() {
		// Add new messages (generate during last tick) to the buffer
		messageBuffer.addAll(newMessages);
		newMessages.clear();
		
		messageBuffer.stream()
					 // Decrease the time left by 1
					 .map(Message::decreaseTimeToDest)
					 // Check if enough ticks have passed from the sending
					 .filter(Message::readyToDeliver)
					 // Check if the destination is still active
					 .filter(msg -> activeNodes.containsKey(msg.getDestId()))
					 // Deliver the message to the destination
					 .forEach(msg -> activeNodes.get(msg.getDestId())
				 								.receiveMessage(msg));
	}
	
	public void sendMessage(Message message) {
		
		if (message.getSenderId() == message.getDestId()) {
			System.out.println("Trying to send msg to itself: ABORT send");
			return;
		}
		
		// Check for random network failure
		if (random.nextInt(100) > probNetFail) {
			// Set a random latency
			message.setLatency(random.nextInt(maxLatency) + 2);
			newMessages.add(message);
			
			Logger.getLogger().logMessageSent(message);
		} else {
			// The message was lost by the network
		}
	}
	
	public void registerNode(Node newNode) {
		activeNodes.put(newNode.getId(), newNode);
	}
	
	public void removeNode(Node crashedNode) {
		activeNodes.remove(crashedNode.getId());
	}
	
}
