package lpbcast;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import lpbcast.utils.ProcessMetrics;
import lpbcast.utils.RoundConfiguration;
import lpbcast.utils.enums.States;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

/**
 * @author Simone Pirocca
 * 
 */
public class Process 
{
	private final String alphabet = "abcdefghjklmnopqrstuvwxyz";
	
	private String id;
	private Network<Process> eventNet;
	private SuperAgent superAgent;
	
	private States state; // 'sub' or 'unSub'
	private boolean crashed, gossipReceived;
	
	private Hashtable<String, Event> events, oldEvents;
	private Hashtable<String, String> eventIds;
	private Hashtable<String, Process> unSubs, subs, view;
	private Hashtable<String, RetrieveEvent> retrieveBuf;
	private Hashtable<String, Integer> subFrequencies;
	
	private int clock;
	private short round, subRoundTimeout;
	
	private RoundConfiguration roundConf;
	private ProcessMetrics procConf;
	private Process known;
	
	private double randomSubThreashold;
	private double randomUnSubThreashold;
	private int eventId;

	private String randomEventId;
	private String randomSubId;

	/**
	 * @param space the position in the space
	 * @param grid the position in the grid
	 * @param roundConf the parameters about the round
	 * @param procMetric the parameters about the process
	 */
	public Process(RoundConfiguration roundConf,
			ProcessMetrics procMetric)
	{
		//Context<Object> context = ContextUtils.getContext(this);
		//eventNet = (Network<Process>)context.getProjection("event network"); TODO
		this.state = States.SUBSCRIBED;
		this.crashed = false;
		this.gossipReceived = true;
		this.superAgent = null;
		
		this.events = new Hashtable<String, Event>();
		this.oldEvents = new Hashtable<String, Event>();
		this.eventIds = new Hashtable<String, String>();
		this.unSubs = new Hashtable<String, Process>();
		this.subs = new Hashtable<String, Process>();
		this.view = new Hashtable<String, Process>();
		this.retrieveBuf = new Hashtable<String, RetrieveEvent>();
		this.subFrequencies = new Hashtable<String, Integer>();

		this.randomEventId = "proc1-1";
		this.randomSubId = "proc1";
		
		this.round = 0;
		this.subRoundTimeout = (short) (this.round + roundConf.getMaxRoundsBeforeReSubscribe());
		this.clock = 1;
		this.procConf = procMetric;
		this.roundConf = roundConf;
		this.randomSubThreashold = 0;
		this.randomUnSubThreashold = 0;
		this.eventId = 0;
	}
	
	/**
	 * Check the value of the clock in order to know
	 * if a new round must be created, and then launch
	 * methods that work only in the beginning of the round
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void checkClock()
	{
		// every one change round a tick after
		if(clock > 0) 
		{
			// triggered only at the benigging of every round
			if(clock == (roundConf.getInterval() - 1))
				retrievingEvents();
			
			changeState();
			clock --;
		}
		else 
		{
			// restart clock and
			// begin a new round
			clock = roundConf.getInterval();
			round++;
			
			checkGossipReception();
			
			if(this.gossipReceived)
			{
				// create only the first event
				//if((this.id.equals("proc1")) && (this.eventId == 0))
				
				short simRound = this.procConf.getCurrentRound();
				short roundEvents = this.procConf.getCurrentEvents();
				
				if((this.round > simRound) && (roundEvents == this.procConf.getEventPerRound()))
				{
					this.procConf.incCurrentRound();
					this.procConf.setCurrentEvents((short)0);
				}
				else if((this.round == simRound) && (roundEvents < this.procConf.getEventPerRound()))
				{
					gossipCreation(); 
				}
				
				gossipEmission();
			}
		}
	}
	
	/**
	 * Sometime, randomly a process change the state
	 * from 'sub' to 'unSub' or viceversa.
	 * The more the time passes without changes,
	 * the more is probable that the process is going to change
	 */
	private void changeState()
	{
		// crash check
		if(!crashed)
		{
			// every tick increase the threashold
			double random = Math.random()*100;
			
			if(state == States.SUBSCRIBED)
			{
				randomUnSubThreashold += 0.00001;
				if(random < randomUnSubThreashold) 
				{
					randomUnSubThreashold = 0;
					unsubscribe();
				}
			}
			else
			{
				randomSubThreashold += 0.00005;
				if(random < randomSubThreashold) 
				{
					randomSubThreashold = 0;
					subscribe();
				}
			}
		}
	}
	
	/**
	 * The subscription of the process
	 */
	private void subscribe()
	{
		if((state == States.UNSUBSCRIBED) && (!crashed))
		{
			this.setState(States.SUBSCRIBED);
			
			known = (Process) Util.getRandomElement(view.values());
			known.subNotification(this);
			
			this.subRoundTimeout = (short) (this.round + roundConf.getMaxRoundsBeforeReSubscribe());
			this.gossipReceived = false;
		}
	}
	
	/**
	 * The unsubscription of the process
	 */
	private void unsubscribe()
	{
		if((state == States.SUBSCRIBED) && (!crashed))
		{
			this.setState(States.UNSUBSCRIBED);
			
			// clean useless hashtables
			this.events = new Hashtable<String, Event>();
			this.unSubs = new Hashtable<String, Process>();
			this.subs = new Hashtable<String, Process>();
			this.subFrequencies = new Hashtable<String, Integer>();
			this.retrieveBuf = new Hashtable<String, RetrieveEvent>();
		}
	}
	
	/**
	 * The notification of a subscription
	 */
	public void subNotification(Process newSub)
	{
		if((state == States.SUBSCRIBED) && (!crashed))
		{
			// insert the new sub into 'subs' and 'view', then purge them
			subs.put(newSub.getId(), newSub);
			subFrequencies.put(newSub.getId(), Integer.valueOf(1));
			view.put(newSub.getId(), newSub);
			Util.purgeOldSubs(subs, subFrequencies, procConf.getSubsSize());
			Util.purgeRandomElements(view, procConf.getViewSize());
		}
	}
	
	/**
	 * Check if other processes has received his subscription notification
	 */
	private void checkGossipReception()
	{
		if((this.state == States.SUBSCRIBED) && (!this.gossipReceived) && (this.round > subRoundTimeout))
			if(!this.crashed)
				subscribe();
	}
	
	/**
	 * The creation of 'eventPerRound' events
	 */
	private void gossipCreation()
	{
		if((state == States.SUBSCRIBED) && (!crashed))
		{
			//for(int i=0; i<procConf.getEventPerRound(); i++) 
			//{
				eventId++;
				String tmpId = Integer.toString(eventId);
				
				// select a random set of characters for event description
				String tmpDescr = "";
				for(int j=0; j<50; j++)
				{
					int tmpRandom = (int)Math.random()*26;
					tmpDescr += alphabet.charAt(tmpRandom);
				}
				
				// create the event and put it within 'events'
				Event tmpEvent = new Event (id, tmpId, tmpDescr);
				events.put(tmpEvent.getId(), tmpEvent);
				eventIds.put(tmpEvent.getId(), tmpEvent.getId());
				oldEvents.put(tmpEvent.getId(), tmpEvent);
				
				this.procConf.incCurrentEvents();
			//}
			
			// purge the list 'events'
			Util.purgeOldEvents(events, procConf.getEventsSize());
		}
	}

	/**
	 * In the end of each round, the clock is restored, 
	 * the round is incremented and a gossip message is sent
	 * to 'fanout' processes whith the view chosen randomly
	 */
	private void gossipEmission()
	{		
		if((state == States.SUBSCRIBED) && (!crashed))
		{
			// create the list of 'subs'
			ArrayList<Process> gossipSubs = new ArrayList<Process>();
			Enumeration<Process> enumProcess = subs.elements();
			while(enumProcess.hasMoreElements())
				gossipSubs.add((Process)enumProcess.nextElement());
			
			// insert itself to list of 'subs'
			gossipSubs.add(this);
			
			// create the list of 'unSubs'
			ArrayList<Process> gossipUnSubs = new ArrayList<Process>();
			enumProcess = unSubs.elements();
			while(enumProcess.hasMoreElements())
				gossipUnSubs.add((Process)enumProcess.nextElement());
			
			// create the list of 'events'
			ArrayList<Event> gossipEvents = new ArrayList<Event>();
			Enumeration<Event> enumEvent = events.elements();
			while(enumEvent.hasMoreElements())
			{
				Event tmpEvent = enumEvent.nextElement();
				// increment the 'age' of the event
				tmpEvent.incAge();
				
				gossipEvents.add((Event)tmpEvent); 
			}
			
			// create the list of 'eventIds'
			ArrayList<String> gossipEventIds = new ArrayList<String>();
			Enumeration<String> enumString = eventIds.elements();
			while(enumString.hasMoreElements())
				gossipEventIds.add((String)enumString.nextElement());
			
			// prepare gossip
			Gossip gossip = new Gossip(gossipUnSubs, gossipSubs, gossipEvents, gossipEventIds, this);
			
			// choose 'fanout' random destination within the view
			ArrayList<Process> destinations = Util.getRandomValueSubset(procConf.getFanout(), view);
			
			// send the gossip to every process
			for(Process tmpDest : destinations) 
			{
				// crash check
				if(!crashed)
				{
					tmpDest.gossipReception(gossip);
	
					// network creation
					//eventNet.addEdge(this, tmpDest); TODO
				}
			}
			
			// add current events to 'oldEvents', then delete them from 'events'
			enumEvent = events.elements();
			while(enumEvent.hasMoreElements())
			{
				Event oldEvent = enumEvent.nextElement();
				events.remove(oldEvent.getId());
			}
		}
	}
	
	/**
	 * @param gossip the gossip received
	 * 
	 * The reception of the gossip is handled:
	 * unsupscriptions are deleted from 'view' and 'subs',
	 * (and then inserted into 'unSubs'),
	 * new subscriptions are inserted in 'view' and 'subs',
	 * new events are inserted in 'events' and delivered 
	 * (and relative id in 'eventIds').
	 * Finally, every array is purged based on the relative policy 
	 */
	public synchronized void gossipReception(Gossip gossip)
	{
		if((state == States.SUBSCRIBED) && (!crashed))
		{
			// notify that the first gossip was received
			// (needed for the subscription phase)
			if(this.gossipReceived == false)
				this.gossipReceived = true;
			
			ArrayList<Process> gossipUnSubs = gossip.getUnSubs();
			ArrayList<Process> gossipSubs = gossip.getSubs();
			ArrayList<Event> gossipEvents = gossip.getEvents();
			ArrayList<String> gossipEventIds = gossip.getEventIds();
			Process sender = gossip.getSender();
			String tmpId;
			Process tmpProcess;
			Event tmpEvent;
			
			// update unsubscriptions 
			for(int i=0; i<gossipUnSubs.size(); i++)
			{
				tmpProcess = gossipUnSubs.get(i);
				tmpId = tmpProcess.getId();
				
				// delete element from 'view'
				view.remove(tmpId);
				
				// delete element from 'subs'
				subs.remove(tmpId);
				subFrequencies.remove(tmpId);
				
				// add element to 'unSubs'
				unSubs.put(tmpId, tmpProcess);
			}
			
			// purge 'unSubs'
			Util.purgeRandomElements(unSubs, procConf.getUnSubsSize());
			
			// update subscriptions
			for(int i=0; i<gossipSubs.size(); i++)
			{
				tmpProcess = gossipSubs.get(i);
				tmpId = tmpProcess.getId();
				
				// add element to 'view' and 'subs' if new and not itself
				if(!tmpId.equals(this.id))
				{
					if(view.get(tmpId) == null)
						view.put(tmpId, tmpProcess);
					
					if(subFrequencies.get(tmpId) == null)
					{
						subs.put(tmpId, tmpProcess);
						subFrequencies.put(tmpId, Integer.valueOf(1));
					}
					// otherwise increment the frequency of the sub
					else
					{
						Integer tmpInt = Integer.valueOf(subFrequencies.get(tmpId).intValue() + 1);
						subFrequencies.put(tmpId, tmpInt);
					}
				}
			}

			// purge 'view' and 'subs'
			Util.purgeRandomElements(view, procConf.getViewSize());
			Util.purgeOldSubs(subs, subFrequencies, procConf.getSubsSize());
			
			// crash check
			if(!crashed)
			{
				// update events
				for(int i=0; i<gossipEvents.size(); i++)
				{
					tmpEvent = gossipEvents.get(i);
					tmpId = tmpEvent.getId();
					
					// add element to 'events' and 'eventIds' if new
					if(eventIds.get(tmpId) == null)
					{
						events.put(tmpId, tmpEvent);
						deliverEvent(tmpEvent);
						eventIds.put(tmpId, tmpId);
						if(oldEvents.get(tmpId) == null)
							oldEvents.put(tmpId, tmpEvent);
						if(retrieveBuf.get(tmpId) != null)
							retrieveBuf.remove(tmpId);
					}
				}
				
				// update eventIds
				for(int i=0; i<gossipEventIds.size(); i++)
				{
					tmpId = gossipEventIds.get(i);
					
					// add element to 'retrieveBuf' if new
					if(eventIds.get(tmpId) == null)
					{
						if(retrieveBuf.get(tmpId) == null)
						{
							retrieveBuf.put(tmpId, new RetrieveEvent(tmpId, round, sender));
						}
					}
				}
				
				// purge 'eventIds' and 'events'
				Util.purgeOldEvents(events, procConf.getEventsSize());
				Util.purgeRandomElements(eventIds, procConf.getEventIdsSize());
			}
		}
	}
	
	/**
	 * Try to retrieve events for which the id is known,
	 * but not the event itself
	 */
	private void retrievingEvents()
	{
		if((state == States.SUBSCRIBED) && (!crashed))
		{
			Enumeration<RetrieveEvent> enumRetrieveEvent = retrieveBuf.elements();
			
			while(enumRetrieveEvent.hasMoreElements())
			{
				RetrieveEvent tmp = enumRetrieveEvent.nextElement();
				
				// check if an event did arrive... 
				if((this.round - tmp.getRound()) > roundConf.getK())
				{
					// ... or not in 'k' rounds
					if(eventIds.get(tmp.getId()) == null)
					{
						// if so, ask for the event from the gossip sender,
						// but only if it is the 'k+1' round
						if((this.round - tmp.getRound()) == (roundConf.getK() + 1))
						{
							// crash check
							if(!crashed)
							{
								tmp.getSender().requestEvent(tmp.getId(), this); 
							}
						}
						// otherwise, check if the event didn't arrive in 'r' rounds
						else if((this.round - tmp.getRound()) > roundConf.getR())
						{
							// if so, ask for the event from the gossip sender,
							// but only if it is the 'r+1' round
							if((this.round - tmp.getRound()) == (roundConf.getR() + 1))
							{
								/*int randomInt = (int)Math.random()*(view.size() - 1);
								
								Hashtable<String, Process> cast = (Hashtable<String, Process>) view;
								String [] keys = (String[]) cast.keySet().toArray();
								
								Process randomProcess = view.get(keys[randomInt]);*/
								Process randomProcess = (Process) Util.getRandomElement(view.values());
								
								// crash check
								if(!crashed)
								{
									randomProcess.requestEvent(tmp.getId(), this); 
								}
							}
							// otherwise, check if the event didn't arrive in 'rr' rounds
							else if((this.round - tmp.getRound()) > roundConf.getRr())
							{
								// if so, ask for the event from the gossip sender,
								// but only if it is the 'rr+1' round
								if((this.round - tmp.getRound()) == (roundConf.getRr() + 1))
								{
									// get the id of the source
									String [] idSplit = tmp.getId().split("-");
									String sourceId = idSplit[0]; 
									
									// check if the relative process is known
									Process source = view.get(sourceId);
									
									// if not known, ask for the link or the process
									if(source == null)
									{
										source = superAgent.getProcessById(sourceId);
									}
									// and then, ask for the event from the source
									// crash check
									if(!crashed)
									{
										source.requestEvent(tmp.getId(), this);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void deliverEvent(Event e) {} // it doesn't do anything
	
	/**
	 * @param id the id of the event
	 * @param sender the process who sent the request
	 * 
	 * A request for a particular event notification
	 */
	public synchronized void requestEvent(String id, Process sender)
	{
		// crash check
		if(!crashed)
		{
			Event e = oldEvents.get(id);
			if(e != null) sender.retrieveEvent(e);
		}
	}
	
	/**
	 * @param e the event requested
	 * 
	 * The response for a particular event notification request
	 * to store with other events, removing the id from 'retrieveBuf'
	 */
	public synchronized void retrieveEvent(Event e)
	{
		if((state == States.SUBSCRIBED) && (!crashed))
		{
			if(events.get(e.getId()) == null)
			{
				events.put(e.getId(), e);
				deliverEvent(e);
				eventIds.put(e.getId(), e.getId());
				
				if(oldEvents.get(e.getId()) == null)
					oldEvents.put(e.getId(), e);
				
				retrieveBuf.remove(e.getId());
			}
		}
	}

	/**
	 * @param subs the subs to add
	 */
	public void addSubs(ArrayList<Process> subs) 
	{
		Process tmpSub;
		String tmpSubId;
		
		for(int i=0; i<subs.size(); i++)
		{
			tmpSub = subs.get(i);
			tmpSubId = tmpSub.getId();
			
			this.subs.put(tmpSubId, tmpSub);
			this.subFrequencies.put(tmpSubId, Integer.valueOf(1));
		}
	}

	/**
	 * @param view the neighbours to add
	 */
	public void addView(ArrayList<Process> view) 
	{
		String tmpNeighbourId;
		
		for (Process tmpNeighbour : view) 
		{
			tmpNeighbourId = tmpNeighbour.getId();
			
			if(!(tmpNeighbourId.equals(this.id)))
			{
				this.view.put(tmpNeighbourId, tmpNeighbour);
				this.subs.put(tmpNeighbourId, tmpNeighbour);
			}
		}
	}
	
	/**
	 * @param superAgent the superAgent to set
	 */
	public void setSuperAgent(SuperAgent superAgent) 
	{
		this.superAgent = superAgent;
	}

	/**
	 * 
	 * @return if the process is crashed
	 */
	public boolean isCrashed() 
	{
		return crashed;
	}

	/**
	 * 
	 * @param crashed
	 */
	public void setCrashed(boolean crashed) 
	{
		this.crashed = crashed;
	}
	
	/**
	 * 
	 * @param id
	 */
	public void setId(String id) 
	{
		this.id = id;
	}
	
	/**
	 * @return the id to set
	 */
	public String getId() 
	{
		return id;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(States state) 
	{
		this.state = state;
	}
	
	/**
	 * 
	 * @return the state
	 */
	public States getState() 
	{
		return state;
	}
	
	/**
	 * 
	 * @return if the process is subscribed
	 */
	public boolean isSub()
	{
		return this.state == States.SUBSCRIBED;
	}
	
	/**
	 * 
	 * @return if the process is unsubscribed
	 */
	public boolean isUnSub()
	{
		return this.state == States.UNSUBSCRIBED;
	}
	
	/**
	 * 
	 * @return if the process have a certain event
	 */
	public boolean hasRandomEventId()
	{
		String eventId = this.randomEventId;
		boolean hasEvent = false;
		if (oldEvents.get(eventId) != null)
			hasEvent = true;
		return hasEvent;
	}
	
	/**
	 * 
	 * @return if the process have a certain event
	 */
	public boolean hasRandomProcessInSubs()
	{
		boolean has = false;
		if (subs.get(this.randomSubId) != null)
			has = true;
		return has;
	}

	/**
	 * @return the randomEventId
	 */
	public String getRandomEventId() 
	{
		return randomEventId;
	}
}
