package lpbcast;

public class Event {
	private String id;
	private int age;

	public Event(String id) {
		this.id = id;
		this.age = 0;
	}
	
	public Event cloneEvent() {
		Event clone = new Event(this.id);
		clone.setAge(this.getAge());
		return clone;
	}

	public String getId() {
		return id;
	}

	public int getAge() {
		return age;
	}

	public void incrementAge() {
		this.age++;
	}
	
	public void setAge(int age) {
		this.age = age;
	}
	
	public int getSource() {
		return Integer.parseInt(this.id.split(":")[0]);
	}
	
	public int getGenerationRound() {
		return Integer.parseInt(this.id.split(":")[1]);
	}
}
