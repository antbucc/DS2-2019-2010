package lpbcast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;

public class Node implements Comparable<Node> {
	private Network<Object> viewNet;
	private Random rnd;

	private Integer id;
	private boolean subscribed = true;
	private boolean crashed = false;
	private double crash_pr;
	private double recovery_interval;
	private double message_loss;

	private TreeMap<Node, Integer> view = new TreeMap<>();
	private int l;
	private int init_view_size;
	private int F;

	private ArrayList<Event> events = new ArrayList<>();
	private int events_size;
	private int long_ago;

	private ArrayList<String> eventIds = new ArrayList<>();
	private int eventIds_size;

	private TreeMap<Node, Integer> subs = new TreeMap<>();
	private int subs_size;
	private double k;

	private TreeMap<Node, Integer> unsubs = new TreeMap<>();
	private int unsubs_size;
	private int unsubs_max_age;
	private double unsubscribe_interval;

	private ArrayList<Element> retrieveBuf = new ArrayList<>();
	private int retrieve_delay;
	private int r;

	public Node(Network<Object> viewNet, Random rnd, int id, double crash_pr, double recovery_interval,
			double message_loss, int l, int init_view_size, int F, int events_size, int long_ago, int eventIds_size,
			int subs_size, double k, int unsubs_size, int unsubs_max_age, double unsubscribe_interval,
			int retrieve_delay, int r) {
		this.viewNet = viewNet;
		this.rnd = rnd;
		this.id = id;
		this.crash_pr = crash_pr;
		this.recovery_interval = recovery_interval;
		this.message_loss = message_loss;
		this.l = l;
		this.init_view_size = init_view_size;
		this.F = F;
		this.events_size = events_size;
		this.long_ago = long_ago;
		this.eventIds_size = eventIds_size;
		this.subs_size = subs_size;
		this.k = k;
		this.unsubs_size = unsubs_size;
		this.unsubs_max_age = unsubs_max_age;
		this.unsubscribe_interval = unsubscribe_interval;
		this.retrieve_delay = retrieve_delay;
		this.r = r;
	}

	public void initView(ArrayList<Node> neighbours) {
		for (Node neighbour : neighbours) {
			this.view.put(neighbour, 0);
			this.viewNet.addEdge(this, neighbour);
		}
	}

	public void broadcast(Double tick, Integer m_id) {
		Integer genRound = (int) Math.ceil(tick);
		this.events.add(new Event(this.id.toString() + ":" + genRound.toString() + ":" + m_id.toString()));
		// debug();
		this.remove_oldest_notifications();
		// debug();
	}

	private void send(Node target, Gossip gossip) {
		target.receive(gossip);
	}

	private void remove_oldest_notifications() {
		// out of date
		boolean found = true;
		while (this.events.size() > this.events_size && found) {
			found = false;
			for (int i = 0; i < this.events.size() - 1 && !found; i++) {
				for (int j = i + 1; j < this.events.size() && !found; j++) {
					Event e = this.events.get(i);
					Event e1 = this.events.get(j);
					if (e.getSource() == e1.getSource()
							&& Math.abs(e.getGenerationRound() - e1.getGenerationRound()) > this.long_ago) {
						if (e.getGenerationRound() > e1.getGenerationRound()) {
							this.events.remove(e1);
						} else {
							this.events.remove(e);
						}
						found = true;
					}
				}
			}
		}

		// age
		while (this.events.size() > this.events_size) {
			Event candidate = this.events.get(0);
			for (int i = 1; i < this.events.size(); i++) {
				if (candidate.getAge() < this.events.get(i).getAge()) {
					candidate = this.events.get(i);
				}
			}
			this.events.remove(candidate);
		}
	}

	private Node select_process(TreeMap<Node, Integer> node_list) {
		boolean found = false;

		double avg = 0;
		for (Integer freq : new ArrayList<Integer>(node_list.values())) {
			avg += freq;
		}
		avg /= (1.0 * node_list.size());

		Node node = null;
		while (!found) {
			Integer targetId = rnd.nextInt(node_list.size());

			node = (new ArrayList<Node>(node_list.keySet())).get(targetId);
			Integer freq = (Integer) node_list.get(node);
			if (freq > k * avg) {
				found = true;
			} else {
				node_list.put(node, freq + 1);
			}
		}

		return node;
	}

	synchronized private void receive(Gossip gossip) {
		if (!this.crashed && this.subscribed) {
			// debugGossip(gossip);
			// debug();
			// update view, subs, unsubs with unsubscriptions
			for (Node node : gossip.getUnsubs().keySet()) {
				this.view.remove(node);
				if (this.viewNet.getEdge(this, node) != null) {
					this.viewNet.removeEdge(this.viewNet.getEdge(this, node));
				}
				this.subs.remove(node);
				if (!this.unsubs.containsKey(node)) {
					this.unsubs.put(node, gossip.getUnsubs().get(node));
				}
			}

			// remove random elements from unsubs
			HashSet<Integer> targetsId = new HashSet<>();
			while (targetsId.size() < this.unsubs.size() - this.unsubs_size) {
				targetsId.add(rnd.nextInt(this.unsubs.size()));
			}
			ArrayList<Node> nodes = new ArrayList<>(this.unsubs.keySet());
			for (Integer targetId : targetsId) {
				this.unsubs.remove(nodes.get(targetId));
			}

			// update view and subs with new subscritptions
			for (Node node : gossip.getSubs()) {
				if (!node.equals(this) && !this.unsubs.containsKey(node)) {
					if (!this.view.containsKey(node)) {
						this.view.put(node, 1);
						this.viewNet.addEdge(this, node);
					} else {
						this.view.put(node, this.view.get(node) + 1);
					}
					if (!this.subs.containsKey(node)) {
						this.subs.put(node, 1);
					} else {
						this.subs.put(node, this.subs.get(node) + 1);
					}
				}
			}

			// remove elements in view and subs based on frequency
			while (this.view.size() > this.l) {
				Node node = this.select_process(this.view);
				int frequency = this.view.get(node);
				this.view.remove(node);
				this.viewNet.removeEdge(this.viewNet.getEdge(this, node));
				this.subs.put(node, Math.max(frequency, (this.subs.get(node) == null ? 0 : this.subs.get(node))));
			}
			while (this.subs.size() > this.subs_size) {
				Node node = this.select_process(this.subs);
				this.subs.remove(node);
			}

			// update events with new notification
			for (Event event : gossip.getEvents()) {
				for (Event event1 : this.events) {
					if (event.getId().equals(event1.getId()) && event1.getAge() < event.getAge()) {
						event1.setAge(event.getAge());
					}
				}

				if (!this.eventIds.contains(event.getId())) {
					this.events.add(event);
					// deliver the event
					this.eventIds.add(event.getId());
				}
			}

			// update eventIds
			for (String eventId : gossip.getEventIds()) {
				if (!this.eventIds.contains(eventId)) {
					ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
					this.retrieveBuf
							.add(new Element(eventId, (int) Math.ceil(schedule.getTickCount()), gossip.getSender()));
				}
			}

			// remove events based on age
			this.remove_oldest_notifications();

			// remove oldest eventIds
			while (this.eventIds.size() > this.eventIds_size) {
				this.eventIds.remove(0);
			}
		}
		debug();
	}

	synchronized public Event retrieveEvent(String eventId) {
		Event event = null;
		if (!this.crashed && this.subscribed && this.eventIds.contains(eventId)) {
			if (rnd.nextDouble() < 1 - this.message_loss) {
				// reply delivered
				ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
				int currentRound = (int) Math.ceil(schedule.getTickCount());

				event = new Event(eventId);
				event.setAge(currentRound - event.getGenerationRound());
			}
		}
		return event;
	}

	public boolean unicastRetrieve(String eventId, Node target) {
		if (rnd.nextDouble() < 1 - this.message_loss) {
			// request delivered
			Event event = target.retrieveEvent(eventId);
			if (event != null) {
				this.events.add(event);
				// Event delivery
				this.eventIds.add(event.getId());
				return true;
			}
		}
		return false;
	}

	@ScheduledMethod(start = 1.75, interval = 1)
	private void processRetrieveBuf() {
		if (!this.crashed && this.subscribed) {
			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
			int currentRound = (int) Math.ceil(schedule.getTickCount());
			ArrayList<Element> obtained = new ArrayList<>();

			for (Element e : this.retrieveBuf) {
				if (currentRound - e.getRound() > this.retrieve_delay) { // waited retrieve_delay (k) rounds
					if (!this.eventIds.contains(e.getElementId())) { // the event has not been received in the meanwhile
						if (!e.getAskedGossipSender()) { // not asked to the gossip sender yet
							e.setAskedGossipSender(true);
							boolean found = unicastRetrieve(e.getElementId(), e.getGossipSender());
							if (found) { // event obtained
								obtained.add(e);
							}
						} else { // asked to the gossip sender, but event not received
							if (currentRound - e.getRound() > this.retrieve_delay + this.r) { // waited r rounds, after
																								// asking for the
								// event to gossip sender
								if (!e.getAskedRandomNode()) { // not asked to a random node yet
									int targetId = this.rnd.nextInt(this.view.size());
									Node randomNode = new ArrayList<Node>(this.view.keySet()).get(targetId);
									boolean found = unicastRetrieve(e.getElementId(), randomNode);
									if (found) { // event obtained
										obtained.add(e);
									}
								} else { // asked to a random node, but event not received
									if (currentRound - e.getRound() > this.retrieve_delay + 2 * this.r) { // waited
																											// another r
																											// rounds,
										// after asking for the event to
										// a random node
										// event retrieved from the source, assuming that source replies always
										Event event = new Event(e.getElementId());
										event.setAge(currentRound - event.getGenerationRound());
										this.events.add(event);
										// Event delivery
										this.eventIds.add(event.getId());
										obtained.add(e);
									}
								}
							}
						}
					} else { // element received in the meanwhile
						obtained.add(e);
					}
				}
			}

			this.retrieveBuf.removeAll(obtained);
		}
	}

	public void recover() {
		this.crashed = false;
	}

	private void clearOldUnsubs() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ArrayList<Node> toBeRemoved = new ArrayList<>();
		for (Node node : this.unsubs.keySet()) {
			if (((int) Math.ceil(schedule.getTickCount())) - this.unsubs.get(node) > this.unsubs_max_age) {
				toBeRemoved.add(node);
			}
		}

		for (Node node : toBeRemoved) {
			this.unsubs.remove(node);
		}
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void gossip() {
		if (!this.crashed && this.subscribed) {

			if (rnd.nextDouble() < 1 - this.crash_pr) {

				// update the unsubs age and remove obsolete ones
				this.clearOldUnsubs();

				HashSet<Integer> targetsId = new HashSet<>();
				if (this.F < this.view.size()) {
					while (targetsId.size() < this.F) {
						targetsId.add(rnd.nextInt(this.view.size()));
					}
				} else {
					for (int i = 0; i < this.view.size(); i++) {
						targetsId.add(i);
					}
				}

				for (Event e : this.events) {
					e.incrementAge();
				}
				ArrayList<Node> nodes = new ArrayList<>(this.view.keySet());
				for (Integer targetId : targetsId) {
					Gossip gossip = new Gossip(this, this.events, this.eventIds, this.subs, this.unsubs);
					this.send(nodes.get(targetId), gossip);
				}

				this.events.clear();
			} else {
				this.crashed = true;
				ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
				ScheduleParameters scheduleParameters = ScheduleParameters
						.createOneTime(schedule.getTickCount() + this.recovery_interval);
				schedule.schedule(scheduleParameters, this, "recover");
			}
		}
	}

	public void subscribe() {
		this.subscribed = true;
		for (Node neighbour : this.view.keySet()) {
			this.viewNet.addEdge(this, neighbour);
		}
	}

	public void processUnsub(Node node, int round) {
		if (!this.crashed && this.subscribed) {
			this.view.remove(node);
			if (this.viewNet.getEdge(this, node) != null) {
				this.viewNet.removeEdge(this.viewNet.getEdge(this, node));
			}

			this.subs.remove(node);
			
			if (!this.unsubs.containsKey(node)) {
				if (this.unsubs.size() == this.unsubs_size) {// remove random elements from unsubs in case it exceeds the maximum size
					Integer targetId = this.rnd.nextInt(this.unsubs_size);
					this.unsubs.remove((new ArrayList<>(this.unsubs.keySet())).get(targetId));
				}
				// add the node to unsubs if it not present already. 
				this.unsubs.put(node, round);
			}
		}
	}

	private void sendUnsub(int round) {
		HashSet<Integer> targetsId = new HashSet<>();
		if (this.F < this.view.size()) {
			while (targetsId.size() < this.F) {
				targetsId.add(rnd.nextInt(this.view.size()));
			}
		} else {
			for (int i = 0; i < this.view.size(); i++) {
				targetsId.add(i);
			}
		}

		ArrayList<Node> nodes = new ArrayList<>(this.view.keySet());
		for (Integer targetId : targetsId) {
			nodes.get(targetId).processUnsub(this, round);
		}
	}

	public void unsubscribe() {
		if (!this.crashed && this.subscribed) {
			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

			this.sendUnsub((int) Math.ceil(schedule.getTickCount()));
			this.subscribed = false;
			// debug();
			TreeMap<Node, Integer> temp_view = new TreeMap<>();
			// remove all outgoing edges
			for (Node node : this.view.keySet()) {
				this.viewNet.removeEdge(this.viewNet.getEdge(this, node));
			}

			// save init_view_size more frequent nodes to re-subscribe then
			for (int i = 0; i < init_view_size; i++) {
				int max = -1;
				Node frequent_node = null;
				for (Node node : this.view.keySet()) {
					if (this.view.get(node) > max) {
						frequent_node = node;
						max = this.view.get(node);
					}
				}
				temp_view.put(frequent_node, 0);
				this.view.remove(frequent_node);
			}

			// clear all the lists and reset the view
			this.view.clear();
			this.view.putAll(temp_view);
			this.subs.clear();
			this.events.clear();
			this.eventIds.clear();
			this.retrieveBuf.clear();
			this.unsubs.clear();

			// schedule the re-subscribing of this node
			ScheduleParameters scheduleParameters = ScheduleParameters
					.createOneTime(schedule.getTickCount() + this.unsubscribe_interval);
			schedule.schedule(scheduleParameters, this, "subscribe");
		}
	}

	public boolean isAlive() {
		return (!this.crashed && this.subscribed);
	}

	@Override
	public int compareTo(Node other) {
		return Integer.compare(this.id, other.id);
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getView() {
		ArrayList<Integer> view = new ArrayList<>();
		for (Node node : this.view.keySet()) {
			view.add(node.id);
		}
		
		return view.toString();
	}
	
	public String getSubs() {
		ArrayList<Integer> subs = new ArrayList<>();
		for (Node node : this.subs.keySet()) {
			subs.add(node.id);
		}
		
		return subs.toString();
	}
	
	public String getUnsubs() {
		ArrayList<Integer> unsubs = new ArrayList<>();
		for (Node node : this.unsubs.keySet()) {
			unsubs.add(node.id);
		}
		
		return unsubs.toString();
	}

	public String getEvents() {
		ArrayList<String> events = new ArrayList<>();
		for (Event event : this.events) {
			events.add(event.getId());
		}
		
		return events.toString();
	}
	
	public String getEventIds() {
		return this.eventIds.toString();
	}
	
	public String getRetrieveBufElems(){
		ArrayList<String> retrieve_events = new ArrayList<>();
		for (Element element : this.retrieveBuf) {
			retrieve_events.add(element.getElementId());
		}
		
		return retrieve_events.toString();
	}
	
	public boolean isCrashed() {
		return this.crashed;
	}
	
	public boolean isSubscribed() {
		return this.subscribed;
	}
	
	public void debug() {
		ArrayList<Integer> viewId = new ArrayList<>();
		for (Node node : this.view.keySet()) {
			viewId.add(node.id);
		}
		ArrayList<Integer> subsId = new ArrayList<>();
		for (Node node : this.subs.keySet()) {
			subsId.add(node.id);
		}
		ArrayList<Integer> unsubsId = new ArrayList<>();
		for (Node node : this.unsubs.keySet()) {
			unsubsId.add(node.id);
		}
		System.out.println("\n----------------------------------------------------");
		System.out.println("Node " + id + " Tick " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
		System.out.println("id: " + id + " subsribed: " + subscribed);
		System.out.println("id: " + id + " view: " + view);
		System.out.println("id: " + id + " viewId: " + viewId);
		System.out.println("id: " + id + " events: " + events);
		System.out.println("id: " + id + " eventIds: " + eventIds);
		System.out.println("id: " + id + " subs: " + subs);
		System.out.println("id: " + id + " subsId: " + subsId);
		System.out.println("id: " + id + " unsubs: " + unsubs);
		System.out.println("id: " + id + " unsubsId: " + unsubsId);
		System.out.println("----------------------------------------------------\n");
	}

	public void debugGossip(Gossip gossip) {
		ArrayList<Integer> subsId = new ArrayList<>();
		for (Node node : gossip.getSubs()) {
			subsId.add(node.id);
		}
		System.out.println("\n-----------------------gossip-----------------------------");
		System.out.println("Node " + id + " Tick " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
		System.out.println("sender " + gossip.getSender().id);
		System.out.println("sender status " + gossip.getSender().subscribed);
		System.out.println("id: " + id + " events: " + gossip.getEvents());
		System.out.println("id: " + id + " eventIds: " + gossip.getEventIds());
		System.out.println("id: " + id + " subs: " + gossip.getSubs());
		System.out.println("id: " + id + " subsId: " + subsId);
		System.out.println("id: " + id + " unsubs: " + gossip.getUnsubs());
		System.out.println("----------------------------------------------------\n");
	}

}
