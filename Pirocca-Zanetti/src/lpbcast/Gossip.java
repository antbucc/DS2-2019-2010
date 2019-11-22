package lpbcast;

import java.util.ArrayList;

/**
 * @author Simone Pirocca
 * 
 */
public class Gossip 
{
	private ArrayList<Process> unSubs;
	private ArrayList<Process> subs;
	private ArrayList<Event> events;
	private ArrayList<String> eventIds;
	private Process sender;
	
	/**
	 * @param unSubs the list of unsubscriptions
	 * @param subs the list of subscriptions
	 * @param events the list of events
	 * @param eventIds the list of id of all events
	 * @param sender the sender of the gossip
	 */
	public Gossip(ArrayList<Process> unSubs, 
			ArrayList<Process> subs, 
			ArrayList<Event> events, 
			ArrayList<String> eventIds, 
			Process sender) 
	{
		super();
		this.unSubs = unSubs;
		this.subs = subs;
		this.events = events;
		this.eventIds = eventIds;
		this.sender = sender;
	}

	/**
	 * @return the unSubs
	 */
	public ArrayList<Process> getUnSubs() 
	{
		return unSubs;
	}

	/**
	 * @return the subs
	 */
	public ArrayList<Process> getSubs() 
	{
		return subs;
	}

	/**
	 * @return the events
	 */
	public ArrayList<Event> getEvents() 
	{
		return events;
	}

	/**
	 * @return the eventIds
	 */
	public ArrayList<String> getEventIds() 
	{
		return eventIds;
	}

	/**
	 * @return the sender
	 */
	public Process getSender() 
	{
		return sender;
	}
}
