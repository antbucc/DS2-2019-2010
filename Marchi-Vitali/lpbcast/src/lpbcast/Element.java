package lpbcast;

public class Element {
	
	private int round;
	private EventId eventId;
	private int gossipSenderId;
	private int roundFirstRequest = -1;
	private int roundSecondRequest = -1;
	private int roundThirdRequest = -1;

	public Element(EventId eventId, int round, int gossipSenderId) {
		this.eventId = eventId;
		this.round = round;
		this.gossipSenderId = gossipSenderId;
	}
	
	public int getRound() {
		return round;
	}
	
	public EventId getEventId() {
		return eventId;
	}
	
	public int getGossipSenderId() {
		return gossipSenderId;
	}
	
	public void setRoundFirstRequest(int roundFirstRequest) {
		this.roundFirstRequest = roundFirstRequest;
	}
	
	public int getRoundFirstRequest() {
		return roundFirstRequest;
	}
	
	public void setRoundSecondRequest(int roundSecondRequest) {
		this.roundSecondRequest = roundSecondRequest;
	}
	
	public int getRoundSecondRequest() {
		return roundSecondRequest;
	}
	
	public void setRoundThirdRequest(int roundThirdRequest) {
		this.roundThirdRequest = roundThirdRequest;
	}
	
	public int getRoundThirdRequest() {
		return roundThirdRequest;
	}
}
