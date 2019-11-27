package communication;

abstract public class Node {
	
	protected int id;
	protected Channel channel;

	public Node(int id, Channel channel) {
		this.id = id;
		this.channel = channel;
	}
	
	public int getId() {
		return id;
	}
	
	abstract public void receiveMessage(Message msg);
	
	public String toString() {
		return Integer.toString(id);
	}

}
