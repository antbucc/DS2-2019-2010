package lpbcast;

public class Event {
	
	private EventId id;
	private int age;
	
	public Event(EventId id) {
		this.id = id;
		age = 0;
	}
	
	public Event(Event original) {
		this.id = original.id;
		this.age = original.age;
	}
		
	public EventId getId() {
		return id;
	}
	
	public int getAge() {
		return age;
	}
	
	public void increaseAge() {
		++age;
	}
	
	public void updateAge(int newAge) {
		this.age = newAge;
	}
}
