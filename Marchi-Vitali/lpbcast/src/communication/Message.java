package communication;

public class Message {
	
	private int senderId;
	private int destId;
	private int timeToDest;
	
	public Message(int senderId, int destId) {
		this.senderId = senderId;
		this.destId = destId;
	}
	
	public int getSenderId() {
		return senderId;
	}
	
	public int getDestId() {
		return destId;
	}
	
	public void setLatency(int latency) {
		this.timeToDest = latency;
	}
	
	public Message decreaseTimeToDest() {
		--timeToDest;
		// Return the message to allow using this method in a Stream.map()
		return this;
	}
	
	public boolean readyToDeliver() {
		return (timeToDest == 0);
	}
}
