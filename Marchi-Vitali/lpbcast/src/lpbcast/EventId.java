package lpbcast;

public class EventId {
	
	private int id;
	private int sourceId;
	
	public EventId(int id, int sourceId) {
		this.id = id;
		this.sourceId = sourceId;
	}
	
	public int getId() {
		return id;
	}
	
	public int getSourceId() {
		return sourceId;
	}
	
	public String toString() {
		return "(" + id + ", " + sourceId + ")";
	}
}
