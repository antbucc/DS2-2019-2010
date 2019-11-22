/**
 * 
 */
package lpbcast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import analysis.Collector;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;

/**
 * Represents the context builder of the application.
 * 
 * @author zanel
 * @author danie
 * @author coffee
 * 
 */
public class LpbCastBuilder implements ContextBuilder<Object> {
	
	public Context<Object> context;
	public Visualization visual;
	public Collector collector;
	public int currentProcessId;
	public HashMap<Process, Double> unsubscribedProcesses;

	@Override
	public Context build(Context<Object> context) {
		context.setId("lpbcast");
		//load parameters from repast simulator
		Configuration.load();
				
		unsubscribedProcesses = new HashMap<>();
		
		this.context = context;
		// Create projections
		NetworkBuilder<Object> builder = new NetworkBuilder("process_network", context, true);
		Network<Object> network = builder.buildNetwork();
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, new RandomCartesianAdder<Object>(), new repast.simphony.space.continuous.WrapAroundBorders(), 100, 100);
				
		// Instantiate and add to context a new Visualization agent
		this.visual = new Visualization(network, context);
		context.add(visual);
		
		// add collector meta-actor in order to perfom analysis of the protocol
		this.collector = new Collector();
		context.add(collector);
				

		// create processes
		for(int i = 0; i < Configuration.INITIAL_NODES_NUMBER; i++) {
			HashMap<Integer, Integer> view = new HashMap<>(Configuration.VIEW_MAX_SIZE);
			// Create a chain of nodes to ensure no partitioning
			if(i != Configuration.INITIAL_NODES_NUMBER - 1) {
				view.put(i + 1, 0);
			}
			while(view.size() < Configuration.VIEW_MAX_SIZE) {
				int targetId = RandomHelper.nextIntFromTo(0, Configuration.INITIAL_NODES_NUMBER - 1);
				if(targetId != i) {
					// the target process is put in the view only if it is not already contained
					view.putIfAbsent(targetId, 0);
				}	
			}
			
			context.add(new Process(i, view, visual, collector));
		}
		
		RunEnvironment.getInstance().getCurrentSchedule().schedule(ScheduleParameters.createRepeating(1, 1, ScheduleParameters.LAST_PRIORITY), ()-> step());
		
		/*
		 * Generate initial events randomly if required
		 */
		for(int i = 0; i < Configuration.INITIAL_EVENTS_IN_SYSTEM; i++) {
			// pick a random process
			Iterator<Object> it = context.getRandomObjects(Process.class, 1).iterator();
			if(it.hasNext()) {
				Process p = (Process) it.next();
				// lpb cast an event
				p.lpbCast();
			}
		}
		
		return context;
	}
	
	/**
	 * Gets the current tick of the simulation
	 * @return the current tick
	 */
	public double getCurrentTick() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}
	
	
	public void step() {
		
		Network<Object> c = ((Network<Object>)context.getProjection("process_network"));
		
		//Generate new nodes with a certain  probability
		if(RandomHelper.nextDouble() < Configuration.SUBSCRIBE_PROBABILITY) {
			//take a random neighbor
			Iterator<Object> it = context.getRandomObjects(Process.class, 1).iterator();
			//the only case in which the iterator is empty is if all nodes have unsubmitted
			if(it.hasNext()) {
				Process neighbor =  (Process)it.next();
				HashMap<Integer, Integer> processView = new HashMap<>();
				processView.put(neighbor.processId, 0);
				this.currentProcessId++;
				Process newProcess = new Process(this.currentProcessId, processView, this.visual, this.collector);
				context.add(newProcess);
				Iterator<Object> ite = context.getAgentLayer(Object.class).iterator();
				newProcess.subscribe(neighbor.processId);
			} 
			
		}
		
		// Unsubmit some node with a certain probability
		if(RandomHelper.nextDouble() < Configuration.UNSUBSCRIBE_PROBABILITY) {
			//take a random node
			Iterator<Object> it = context.getRandomObjects(Process.class, 1).iterator();
			if(it.hasNext()) {
				Process target =  (Process)it.next();
				target.unsubscribe();
				this.unsubscribedProcesses.put(target, this.getCurrentTick());
				
			} 
			
		}
		
		// after a constant amount of tick delete the unsubmitted nodes from the context
		Iterator<Map.Entry<Process, Double>> it = this.unsubscribedProcesses.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Process, Double> entry = it.next();
			if(this.getCurrentTick() - entry.getValue() > Configuration.UNSUB_VISUAL_TIME) {
				Process p = entry.getKey();
				context.remove(p);
				it.remove();
			}
		}
		/**
		 * subscription-analysis
		 * 
		 if(getCurrentTick() == 100) {
			// Simulate node subscription (Remember to set parameters accordingly)
			currentProcessId = Configuration.INITIAL_NODES_NUMBER;
			int targetId = RandomHelper.nextIntFromTo(0, Configuration.INITIAL_NODES_NUMBER - 1);
			HashMap<Integer, Integer> processView = new HashMap<>();
			processView.put(targetId, 0);
			Process subscriber = new Process(currentProcessId, processView, visual, collector);
			context.add(subscriber);
			subscriber.subscribe(targetId);
			currentProcessId += 1;
		}
		*/
		//take random processes and lpbcast an event
		for(int i = 0; i < Configuration.EVENTS_GENERATED_PER_ROUND; i++) {
			Iterator<Object> prit = context.getRandomObjects(Process.class, 1).iterator();
			if(prit.hasNext()) {
				Process p = (Process)prit.next();
				if(!p.isUnsubscribed) {
					p.lpbCast();
				}
			}
		}
	}
}
