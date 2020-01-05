package communication;

// Generic node in a distributed system, identified by a (unique) address
// It can send messages through the channel (channel.sendMessage(Message) method)
// and receive messages from the channel (receiveMessage(Message) method)
abstract public class Node {
	
	protected final int address;
	protected final Channel channel;

	public Node(int address, Channel channel) {
		this.address = address;
		this.channel = channel;
	}
	
	public int getAddress() {
		return address;
	}
	
	abstract public void receiveMessage(Message msg);
	
	public String toString() {
		return "Node(" + address + ")";
	}

}
