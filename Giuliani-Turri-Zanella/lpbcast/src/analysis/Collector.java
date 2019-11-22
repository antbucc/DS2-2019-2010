package analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import lpbcast.Event;
import repast.simphony.engine.environment.RunEnvironment;

/**
 * Agent that supports the operation of data collection needed to perform analysis of the protocol
 * 
 * @author danie
 * 
 */
public class Collector {
	
	private HashMap<UUID, Integer> messagePropagationData;
	private HashMap<Integer, Double> subscriptionData;
	private HashMap<UUID, Integer> recoveriesPerEventData;
	private int deliveredEvents;
	private int redundancies;
	
	/**
	 * Instantiates a new collector, the collector should only be a single one (per run).
	 * 
	 */
	public Collector() {
		// initialize structures needed to store data
		messagePropagationData = new HashMap<UUID, Integer>();
		setSubscriptionData(new HashMap<Integer, Double>());
		recoveriesPerEventData = new HashMap<UUID, Integer>();
		deliveredEvents = 0;
		redundancies = 0;
	}
	
	/**
	 * GETTER AND SETTER TO ACCESS THE STRUCTURES
	 *
	 * method: notifyEventDelivered() or something like this to see when event delivered by node
	 * method: reset[NameOfStructure]() or can we do a single one? that is called by the
	 * reset() method of the CustomDataSource in order to prepare for next tick (if needed, so probably
	 * is a different method for each structure... or maybe not, think about it...)
	 */
	
	/**
	 * Notify delivery of message, used to collect data about message propagation during a single tick
	 * @param e event delivered
	 */
	public void notifyMessagePropagation(Event e) {
		UUID uuid = e.eventId.id;
		int nDeliveries = this.messagePropagationData.getOrDefault(uuid, 0);
		this.messagePropagationData.put(uuid, nDeliveries + 1);
	}
	
	public void notifyDeliveredEvent(int processId) {
		this.deliveredEvents++;
	}
	
	public void notifyRedundancy() {
		this.redundancies ++;
	}
	
	public void resetDeliveredEvents() {
		this.deliveredEvents = 0;
	}
	
	public void resetRedundancies() {
		this.redundancies = 0;
	}
	
	
	/**
	 * Reset messagePropagationData to prepare for next tick
	 */
	public void resetMessagePropagationData() {
		// we don't want to reset this structure since we want cumulative sum each new tick
	}
	
	/**
	 * Getter for the message propagation data of the current tick
	 * @return
	 */
	public String getTickMessagePropagationData() {
		return messagePropagationData.toString();
	}

	public HashMap<Integer, Double> getSubscriptionData() {
		return subscriptionData;
	}

	public void setSubscriptionData(HashMap<Integer, Double> subscriptionData) {
		this.subscriptionData = subscriptionData;
	}
	
	/**
	 * Gets the current tick of the simulation.
	 * 
	 * @return the current tick
	 */
	public double getCurrentTick() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}
	
	
	public void notifyRecovery(UUID uuid) {
		int nRecoveries = this.recoveriesPerEventData.getOrDefault(uuid, 0);
		this.recoveriesPerEventData.put(uuid, nRecoveries + 1);
	}
	public int getRecoveryData() {
		Object[] values = this.recoveriesPerEventData.values().toArray();
		int sum = 0;
		//compute average of recoveries per event
		for(Object v : values) {
			int el = (Integer) v;
			sum = sum + el;
		}
		int avg = sum / values.length;
		return avg;
	}
	
	
	public String getDeliveredEvents() {
		return Integer.toString(deliveredEvents);
	}
	
	public String getRedundancies() {
		return Integer.toString(redundancies);
	}
	
	
}
