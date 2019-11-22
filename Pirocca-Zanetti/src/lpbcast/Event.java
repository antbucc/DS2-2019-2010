package lpbcast;

/**
 * @author Simone Pirocca
 * 
 */
public class Event 
{
	private String id;
	private String description;
	private int age;
	
	/**
	 * @param originator the id of the originator of the event
	 * @param id the progressive id of the event for that originator
	 * @param description the description of the event
	 */
	public Event(String originator, String id, String description) 
	{
		super();
		this.id = originator + "-" + id;
		this.description = description;
		this.age = 0;
	}
	
	/**
	 * Increment the age of the event
	 */
	public void incAge()
	{
		this.age++;
	}

	/**
	 * @return the age
	 */
	public int getAge() 
	{
		return age;
	}

	/**
	 * @return the id
	 */
	public String getId() 
	{
		return id;
	}

	/**
	 * @return the description
	 */
	public String getDescription() 
	{
		return description;
	}
}
