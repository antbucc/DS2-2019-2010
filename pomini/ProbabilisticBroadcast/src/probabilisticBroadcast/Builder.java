package probabilisticBroadcast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;

public class Builder implements ContextBuilder<Object> {
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("ProbabilisticBroadcast");
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(
				"gossip network", context, true);
		netBuilder.buildNetwork();

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 50,
				50);
		
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		// reading and setting parameters
		int maxViewSize = params.getInteger("max_view_size");
		int maxSubs = params.getInteger("max_subs");
		int maxUnsubs = params.getInteger("max_unsubs");
		int maxEvents = params.getInteger("max_events");
		int maxEventIds = params.getInteger("max_event_ids");
		int gossipSize = params.getInteger("gossip_size");
		int kRounds = params.getInteger("k_rounds");
		int totalRounds = params.getInteger("rounds");
		int joinProb = params.getInteger("joinprob");
		int unJoinProb = params.getInteger("unjoinprob");
		assert (joinProb + unJoinProb <= 100);	// the sum must not be over 100%
		Process.setParameters(maxViewSize, maxSubs, maxUnsubs, maxEvents, maxEventIds, gossipSize, kRounds);
		
		int nodeCount = params.getInteger("node_counts");
		HashMap<String, Process> allNodes = new HashMap<String, Process>();
		System.out.println("Starting " + nodeCount + " nodes");
		ActionManager manager = new ActionManager(joinProb, unJoinProb, nodeCount, space, allNodes, context);
		context.add(manager);
		for (int i = 0; i < nodeCount; i++) {
			Process node = new Process();
			allNodes.put(node.getProcessId(), node);
			context.add(node);
		}
		// setting view
		String neighbourhoodSelection = params.getString("neighbourhood_selection");
		
		// setting views
		if (neighbourhoodSelection.equals("Random")) {
			System.out.println("Using random view creation");
			for (Process p: allNodes.values()) {
				p.setAllProcesses(allNodes);
				// random number of processes and randomly selected
				p.setView(this.createRandomView(RandomHelper.nextIntFromTo(1, nodeCount < maxViewSize ? nodeCount : maxViewSize), p, allNodes.values()));
			}
		} else {
			System.out.println("Creating view with the nearest nodes");
			for (Process p: allNodes.values()) {
				p.setAllProcesses(allNodes);
				p.setView(this.createKNNView(RandomHelper.nextIntFromTo(1, nodeCount < maxViewSize ? nodeCount : maxViewSize), p, allNodes.values(), space));
			}
		}
		RunEnvironment.getInstance().endAt(totalRounds);
		
		return context;	
	}
	
	/**
	 * Generate a random set of nodes
	 * @param size number of nodes in the view
	 * @param main the process whose view is meant to
	 * @param allProcesses the list of all available processes
	 * @return a map string -> process
	 */
	private Map<String, Process> createRandomView(int size, Process main, Collection<Process> allProcesses) {
		Map<String, Process> view = new HashMap<>();
		ArrayList<Process> processes = new ArrayList<>(allProcesses);
		if (processes.contains(main))
			processes.remove(main);
		if (size > processes.size())
			size = processes.size();
		Collections.shuffle(processes);
		for (int i = 0; i < size; i++) {
			Process item = processes.get(i);
			view.put(item.getProcessId(), item);
		}
		return view;
	}
	
	/**
	 * Generate a view made by the nearest n nodes
	 * @param size number of nodes in the view
	 * @param main the considered process
	 * @param allProcesses list of all the processes
	 * @param space Continuous space where nodes are placed
	 * @return
	 */
	private Map<String, Process> createKNNView(int size, Process main, Collection<Process> allProcesses, ContinuousSpace<Object> space) {
		HashMap<String, Process> view = new HashMap<>();
		ArrayList<Process> processes = new ArrayList<>(allProcesses);
		Comparator<Process> c = new ProcessComparator(main, space);
		if (processes.contains(main))
			processes.remove(main);
		if (size > processes.size())
			size = processes.size();
		processes.sort(c);
		for (int i = 0; i < size; i++)
			view.put(processes.get(i).getProcessId(), processes.get(i));
		return view;
	}
	
	private class ProcessComparator implements Comparator<Process>{
		private Process main;
		private ContinuousSpace<Object> space;
		
		public ProcessComparator(Process main, ContinuousSpace<Object> space) {
			this.main = main;
			this.space = space;
		}
		@Override
		public int compare(Process o1, Process o2) {
			double dist1 = space.getDistance(space.getLocation(main), space.getLocation(o1));
			double dist2 = space.getDistance(space.getLocation(main), space.getLocation(o2));
			return dist1 < dist2 ? -1 : dist1 == dist2 ? 0 : 1;
		}
	}
}
