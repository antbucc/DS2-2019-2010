package lpbcast;

/**
 * @author Simone Pirocca
 * 
 */
public class RetrieveEvent 
{
	private String id;
	private short round;
	private Process sender;
	
	/**
	 * @param id the id of the retrieved event
	 * @param round the round in which the event was retrieves
	 * @param sender the sender of the gossip about that event
	 */
	public RetrieveEvent(String id, short round, Process sender) 
	{
		super();
		this.id = id;
		this.round = round;
		this.sender = sender;
	}

	/**
	 * @return the id
	 */
	public String getId() 
	{
		return id;
	}

	/**
	 * @return the round
	 */
	public short getRound() 
	{
		return round;
	}

	/**
	 * @return the sender
	 */
	public Process getSender() 
	{
		return sender;
	}
}
