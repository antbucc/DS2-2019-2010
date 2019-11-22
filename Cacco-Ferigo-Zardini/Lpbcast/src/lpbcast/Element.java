package lpbcast;

public class Element {

	private String elementId;
	private int round;
	private Node gossipSender;
	private boolean askedGossipSender = false;
	private boolean askedRandomNode = false;

	public Element(String elementId, int round, Node gossipSender) {
		this.elementId = new String(elementId);
		this.round = round;
		this.gossipSender = gossipSender;
	}

	public String getElementId() {
		return elementId;
	}

	public int getRound() {
		return round;
	}

	public Node getGossipSender() {
		return gossipSender;
	}

	public boolean getAskedGossipSender() {
		return askedGossipSender;
	}

	public void setAskedGossipSender(boolean askedGossipSender) {
		this.askedGossipSender = askedGossipSender;
	}

	public boolean getAskedRandomNode() {
		return askedRandomNode;
	}

	public void setAskedRandomNode(boolean askedRandomNode) {
		this.askedRandomNode = askedRandomNode;
	}
}
