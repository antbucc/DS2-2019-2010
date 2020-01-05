package communication;

public class Message {
	
	protected int senderAddr;
	protected int destAddr;
	private int timeToDest;
	
	public Message(int senderAddr, int destAddr) {
		this.senderAddr = senderAddr;
		this.destAddr = destAddr;
	}
	
	public int getSenderAddr() {
		return senderAddr;
	}
	
	public int getDestAddr() {
		return destAddr;
	}
	
	void setDestAddr(int destAddr) {
		this.destAddr = destAddr;
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

	@Override
	public String toString() {
		return "Message [sender=" + senderAddr + ", dest=" + destAddr + "]";
	}
	
	
}
