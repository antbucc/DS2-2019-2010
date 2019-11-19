package probabilisticBroadcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;

public class ActionManager {
	private int joinProb, unjoinProb, totalProcesses;
	private ContinuousSpace<Object> space;
	private HashMap<String, Process> allNodes;
	private Context<Object> context;
	private Set<Event> allEvents;
	
	public ActionManager(int joinProb, int unjoinProb, int totalProcesses, ContinuousSpace<Object> space, HashMap<String, Process> allNodes, Context<Object> context) {
		this.joinProb = joinProb;
		this.unjoinProb = unjoinProb;
		this.space = space;
		this.allNodes = allNodes;
		this.context = context;
		this.allEvents = new HashSet<>();
		this.totalProcesses = totalProcesses;
	}
	
	@ScheduledMethod ( start = 1 , interval = 1)
	public void createNectAction() {
		ArrayList<Process> processes = new ArrayList<>(this.allNodes.values());
		if (!processes.isEmpty()) {
			int candidate = RandomHelper.nextIntFromTo(0, processes.size() - 1);
			Process chosen = processes.get(candidate);
			int action = RandomHelper.nextIntFromTo(0, 99);
			if (action < this.joinProb) {
				// new node
				HashMap<String, Process> view = new HashMap<>();
				view.put(chosen.getProcessId(), chosen);
				Process p = new Process(view);
				this.allNodes.put(p.getProcessId(), p);
				p.setAllProcesses(this.allNodes);
				System.out.println("Process " + p.getProcessId() + " created");
				this.context.add(p);
				this.totalProcesses++;
			} else if (action < this.joinProb + this.unjoinProb && allNodes.size() > 0) {
				System.out.println("Process " + chosen.getProcessId() + " deleted");
				chosen.suicide();
				this.allNodes.remove(chosen.getProcessId());
				this.context.remove(chosen);
			} else {
				Event event = new Event(chosen.getProcessId(), (int) RepastEssentials.GetTickCount());
				this.context.add(event);
				System.out.println("New event created");
				this.allEvents.add(event);
				chosen.newEvent(event);
			}
		}
	}
	
	public double clusteringCoefficient() {
		double total = 0.0;
		for (Process p: this.allNodes.values()) {
			int k = 0, sum = 0;
			for (Process q: p.getView().values()) {
				if (q != null) {
					k++;
					for (String n: q.getView().keySet()) {
						if (p.getView().containsKey(n) && !n.equals(p.getProcessId()))
							sum++;
					}
				}
			}
			if (k > 1)
				total += (sum * 1.0 / (k * (k - 1)));
		}
		return total / this.allNodes.size();
	}
	
	public double avgPathLength() {
		ArrayList<String> allIds = new ArrayList<>(this.allNodes.keySet());
		int totalLength = 0, totalConnected = 0;
		for (int i = 0; i < allIds.size(); i++) {
			for (int j = 0; j < allIds.size(); j++) {
				if (i != j) {
					int dist = this.nodeDistance(allIds.get(i), allIds.get(j));
					if (dist >= 0) {
						totalLength += dist;
						totalConnected += 1;
					}
				}
			}
		}
		return totalConnected > 0 ? totalLength * 1.0 / totalConnected : 0.0;
	}
	
	public double avgInDegree() {
		HashMap<String, Integer> pointed = new HashMap<>(this.allNodes.size());
		for (String s: this.allNodes.keySet())
			pointed.put(s, 0);
		for (Process p: this.allNodes.values()) {
			for (String node: p.getView().keySet()) {
				if (pointed.containsKey(node)) {
					int value = pointed.get(node);
					pointed.put(node, value + 1); 
				}
			}
		}
		int total = 0;
		for (Integer i: pointed.values()) 
			total += i;
		return total * 1.0 / pointed.size();
	}
	
	private int nodeDistance(String node1, String node2) {
		HashMap<String, Boolean> visited = new HashMap<>(this.allNodes.size());
		for (String p: this.allNodes.keySet())
			visited.put(p, false);
		LinkedList<String> queue = new LinkedList<>();
		LinkedList<Integer> distances = new LinkedList<>();
		queue.add(node1);
		distances.add(0);
		visited.put(node1, true);
		while (!queue.isEmpty()) {
			String item = queue.pollFirst();
			int distance = distances.pollFirst();
			Process n = this.allNodes.get(item);
			if (n != null) {
				for (String neighbour: n.getView().keySet()) {
					if (visited.containsKey(neighbour) && !visited.get(neighbour)) {
						visited.put(neighbour, true);
						if (neighbour.equals(node2))
							return distance + 1;
						else {
							queue.add(neighbour);
							distances.add(distance + 1);
						}
					}
				}
			} else {
				visited.put(item, true);
			}
		}
		return -1; // nodes not connected!!
	}
	
	public int getTotalProcesses() {
		return totalProcesses;
	}
	
	public int getCurrentProcessCount() {
		return this.allNodes.size();
	}
	
	public int getClusters() {
		HashMap<String, Boolean> visited = new HashMap<>(this.allNodes.size());
		for (String p: this.allNodes.keySet())
			visited.put(p, false);
		int total = 0;
		for (Process p: this.allNodes.values()) {
			if (!visited.get(p.getProcessId())) {
				total++;
				LinkedList<String> queue = new LinkedList<>();
				queue.add(p.getProcessId());
				visited.put(p.getProcessId(), true);
				while(!queue.isEmpty()) {
					String id = queue.pollFirst();
					Process q = this.allNodes.get(id);
					for (Process n: q.getView().values()) {
						if (n != null && visited.containsKey(n.getProcessId()) && !visited.get(n.getProcessId())) {
							visited.put(n.getProcessId(), true); 
							queue.add(n.getProcessId());
						}
					}
				}
			}
		}
		return total;
	}

}
