package pbcast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import analysis.DeliveryDistribution;
import message.GossipHandler;
import message.Message;
import message.MessageHandler;
import message.SolicitationHandler;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import repast.simphony.util.collections.IndexedIterable;
import tree.Tree;

public class Process {
	ArrayList<MessageHandler> history;
	Queue<MessageHandler> in_messages;
	Queue<MessageHandler> out_messages;
	Queue<GossipHandler> gossip_messages;
	Queue<SolicitationHandler> solicitation_messages;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;
	private ArrayList<Tree> spanning_trees;
	private ArrayList< RepastEdge<Object> > my_edges;
	private int my_round_number;
	private int retransmission_count;
	private int throughput;
	
	// Provare ad implementarlo con una macchina a stati (?)
	public enum ProcessType {
		QUIETE,
		SENDER,
		RECIEVER,
		CHILD
	}
	ProcessType type;

	public Process(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.my_round_number = 0;
		this.retransmission_count = 0;
		this.history = new ArrayList<MessageHandler>();
		this.in_messages = new LinkedList<MessageHandler>();
		this.out_messages = new LinkedList<MessageHandler>();
		this.gossip_messages = new LinkedList<GossipHandler>();
		this.solicitation_messages = new LinkedList<SolicitationHandler>();
		this.type = ProcessType.QUIETE;
		this.my_edges = new ArrayList< RepastEdge<Object> >();
		this.throughput = 0;
	}
	
	public ProcessType getCurrentType() {
		return this.type;
	}
	
	public int getHistorySize() {
		return this.history.size();
	}
	
	public int getGossipSize() {
		return this.gossip_messages.size();
	}
	
	public double getProbabilityPbcast() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int processesCount = (Integer) params.getValue("processes_count");
		int count_pbcast = 0;
		for (String msg : DeliveryDistribution.performance.keySet())
			if (DeliveryDistribution.performance.get(msg) == processesCount)
				count_pbcast++;
		return (double) count_pbcast / DeliveryDistribution.performance.size()*100;		
	}

	public double getPerformance() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int processesCount = (Integer) params.getValue("processes_count");
		return (DeliveryDistribution.getReceivedAvarage()/processesCount) * 100;
	}
	
	public double getMyFanoutAverage() {
		int s = 0;
		for (int f : DeliveryDistribution.fanout.get(this.getId()).values())
			s += f;
		System.out.println("S: " + s + " size: " + DeliveryDistribution.fanout.size());
		return (double) s / DeliveryDistribution.fanout.size();
	}
	
	public int getThroughput() {
		return this.throughput;
	}
	
	public void setSpanningTree(ArrayList<Tree> spanning_trees) {
		this.spanning_trees = spanning_trees;
	}
	
	public int getId() {
		return this.id;
	}
	
	private int getRoundNumber() {
		return this.my_round_number;
	}

	private int getRoundNumberAndIncrease() {
		this.retransmission_count = 0;
		return ++this.my_round_number;
	}

	private void send() {
		MessageHandler msg_hnd = this.out_messages.remove();
		Process to = msg_hnd.getTo();
		
		// Draw the current passage
		Context<Object> context = ContextUtils.getContext(this);
		Parameters params = RunEnvironment.getInstance().getParameters();
		int process_id = (int) params.getValue("originator_view");
		Tree tree = msg_hnd.getTree();
				
		// Send the message
		double prob_loss = (double) params.getValue("loss_prob");
		if (RandomHelper.nextDouble() < prob_loss) {
			Network<Object> loss_net = (Network<Object>) context.getProjection("loss network");
			if (tree.getId() == process_id) {
				this.type = ProcessType.SENDER;
				to.type = ProcessType.RECIEVER;
				for (Process child : tree.getChildrenOf(to))
					child.type = ProcessType.CHILD;
				loss_net.addEdge(this, to);
			}
		} else {
			this.throughput+=1;
			Network<Object> send_net = (Network<Object>) context.getProjection("send network");
			Network<Object> exchange_net = (Network<Object>) context.getProjection("exchange");
			if (tree.getId() == process_id) {
				this.type = ProcessType.SENDER;
				to.type = ProcessType.RECIEVER;
				for (Process child : tree.getChildrenOf(to))
					child.type = ProcessType.CHILD;
				RepastEdge<Object> new_edge = exchange_net.addEdge(this, to);
			}
			RepastEdge<Object> new_edge = send_net.addEdge(this, to);
			this.my_edges.add(new_edge);
			to.in_messages.add(msg_hnd);
		}
	}
	
	private void receive() {
		// Preliminary operations
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> exchange_net = (Network<Object>) context.getProjection("exchange");
		Parameters params = RunEnvironment.getInstance().getParameters();
		int process_id = (int) params.getValue("originator_view");
		MessageHandler msg_hnd = (MessageHandler) this.in_messages.remove();
		
		// Add to my history
		this.add_to_history(msg_hnd);
		
		// Forwarding operations
		Tree tree = this.spanning_trees.get(msg_hnd.getTree().getId());
		ArrayList<Process> children = tree.getChildrenOf(this);
		for (Process child : children) {
			// Instantiate new message
			MessageHandler forward_msg_hnd = new MessageHandler(msg_hnd.getMsg(), this, child, tree, 0);
			this.out_messages.add(forward_msg_hnd);
		}
		
		// Statistics
		String key = msg_hnd.summary();
		HashMap<String, Integer> received_pbcast = DeliveryDistribution.received.get(this.getId());
		if (received_pbcast.get(key) == null) {
			received_pbcast.put(key, 1);
			
			int received = DeliveryDistribution.performance.get(key);
			received++;
			DeliveryDistribution.performance.put(key, received);
		}
	}
	
	private void add_to_history(MessageHandler msg_hnd) {
		// add if not already received
		if (!this.history.contains(msg_hnd.summary()))
			history.add(msg_hnd);
	}
	
	private void pbcast(Message msg) {
		// Perform multicast
		unreliably_multicast(msg); 
	}

	private Tree search_my_tree() {
		for (Tree tree : this.spanning_trees)
			if (tree.getId() == this.getId())
				return tree;
		return null;
	}

	private void unreliably_multicast(Message msg) {
		Tree my_tree = this.search_my_tree();
		MessageHandler msg_hnd_multicast = new MessageHandler(msg, this, null, my_tree, 0);
		
		// Add to my history
		this.add_to_history(msg_hnd_multicast);
		
		// Instantiate new message and enqueue it in the out process queue
		for (Process child : my_tree.getChildrenOf(this)) {
			MessageHandler msg_hnd = new MessageHandler(msg, this, child, my_tree, 0);
			this.out_messages.add(msg_hnd);
		}
		
		// Statistics
		DeliveryDistribution.performance.put(msg_hnd_multicast.summary(), 1);
		DeliveryDistribution.received.get(this.getId()).put(msg_hnd_multicast.summary(), 1);
	}
	
	private void gossip_round() {
		Context<Object> context = ContextUtils.getContext(this);
		Parameters params = RunEnvironment.getInstance().getParameters();
		int processes_count = (int) params.getValue("processes_count");
		int round_members = (int) params.getValue("round_members");
		
		//Select randomly n node (no workload is used)
		ArrayList<Process> other_processes = new ArrayList<Process>();
		
		IndexedIterable collection = context.getObjects(Process.class);
		for (Object obj : collection)
			if (obj instanceof Process && obj != this) {
				Process p = (Process) obj;
				other_processes.add(p);
			}
		SimUtilities.shuffle(other_processes, RandomHelper.getUniform());
		
		// Create digest of my history
		ArrayList<String> digest = this.digest_of_my_history();

		// Send gossip messages
		int n = (round_members > processes_count) ? processes_count : round_members;
		int round = this.getRoundNumberAndIncrease();
		for (int i = 0; i < n - 1; i++) {
			Process to = other_processes.get(i);
			to.gossip_messages.add(new GossipHandler(this, to, round, digest));
		}
		
		// Increase gossip count of my history
		for (MessageHandler msg_hnd : this.history) {
			msg_hnd.keep_gossip();
			if (DeliveryDistribution.fanout.get(this.getId()).get(msg_hnd.summary()) == null) {
				DeliveryDistribution.fanout.get(this.getId()).put(msg_hnd.summary(), 1);
			} else {
				int fanout = DeliveryDistribution.fanout.get(this.getId()).get(msg_hnd.summary());
				fanout++;
			}
		}
		
		// delete older messages from history
		clearHistory();
	}
	
	private void receive_gossip() {
		// Reconstruct my history digest
		ArrayList<String> digest = this.digest_of_my_history();
		
		// Empty my gossip message queue (no workload)
		while (!this.gossip_messages.isEmpty()) {
			GossipHandler msg = this.gossip_messages.remove();
			Process to = msg.getFrom();
			int round_number = msg.getRoundId();
			ArrayList<String> requested_message = msg.compareDigest(digest);
			
			// I want to retrieve messages that I don't have
			if (!requested_message.isEmpty()) {
				// Send requested messages in a solicitation message using the same gossip_originator and round_number
				Parameters params = RunEnvironment.getInstance().getParameters();
				int round_limit = (int) params.getValue("round_limit");
				boolean no_limit = (round_limit == -1) ? true : false;
				SolicitationHandler solicitation = new SolicitationHandler(msg.getMsg(), this, to, round_number, requested_message);
				if (this.retransmission_count < round_limit || no_limit) {
					to.solicitation_messages.add(solicitation);
					this.retransmission_count++;
				}
			}
		}
	}
	
	private ArrayList<String> digest_of_my_history() {
		ArrayList<String> digest = new ArrayList<String>();
		for (MessageHandler msg_hnd : this.history)
			digest.add(msg_hnd.summary());
		Collections.sort(digest);
		return digest;
	}
	
	private void answer_to_solicitation() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int process_id = (int) params.getValue("originator_view");
		int round_limit = (int) params.getValue("round_limit");
		boolean no_limit = (round_limit == -1) ? true : false;
		SolicitationHandler solicitation_request = this.solicitation_messages.remove();
		Process to = solicitation_request.getFrom();
		ArrayList<String> requested_messages = solicitation_request.getRequestedMessages();
		if (this.getRoundNumber() <= solicitation_request.getRoundNumber())
			for (String requested_msg : requested_messages) {
				// search message from mine
				Tree tree = this.spanning_trees.get(Integer.parseInt(requested_msg.split(" ; ")[1]));
				MessageHandler retrievied_msg = searchFromMyHistory(requested_msg);
				// add gossip count
				if (retrievied_msg != null && (this.retransmission_count < round_limit || no_limit)) {
					// reply
					if (tree.getId() == process_id) {
						Context<Object> context = ContextUtils.getContext(this);
						Network<Object> ret_net = (Network<Object>) context.getProjection("retrieved network");
						ret_net.addEdge(this, to);
					}
					this.retransmission_count++;
					this.throughput+=1;
					// Simulate sending retrieved message
					//to.in_messages.add(retrievied_msg); // NOT add to in_messagge queue since they will forwarded
					
					// Add to the statistics
					String key = retrievied_msg.summary();
					HashMap<String, Integer> received_pbcast = DeliveryDistribution.received.get(to.getId());
					if (received_pbcast.get(key) == null) {
						received_pbcast.put(key, 1);
						
						int received = DeliveryDistribution.performance.get(key);
						received++;
						DeliveryDistribution.performance.put(key, received);
					}
				}
			}
	}

	private MessageHandler searchFromMyHistory(String summary) {
		for (MessageHandler msg_hnd : this.history)
			if (msg_hnd.summary().equals(summary))
				return msg_hnd;
		return null;
	}
	
	private void clearHistory() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int gossip_limit = (int) params.getValue("gossip_limit");
		for (int i = 0; i < this.history.size(); i++)
			if (this.history.get(i).getGossipCount() >= gossip_limit) {	
				// Statistics
				DeliveryDistribution.fanout.get(this.getId()).remove(this.history.get(i).summary());
				
				this.history.remove(i);
			}
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void dissimination() {
		this.throughput = 0;
		final int ACTIVITIES = 2;
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> send_net = (Network<Object>) context.getProjection("send network");
		
		// How many work this process can do in this step
		Parameters params = RunEnvironment.getInstance().getParameters();
		int min_workload = (Integer) params.getValue("min_workload");
		int max_workload = (Integer) params.getValue("max_workload");
		int workload = RandomHelper.nextIntFromTo(min_workload, max_workload);
		
		// Decide if I can send
		int single_sender = (int) params.getValue("single_pbcast");
		boolean all_send = (single_sender == -1) ? true : false;
		
		// Send a random string in pbcast
		double pbcast_freq = (double) params.getValue("pbcast_frequency");
		if((all_send || this.getId() == single_sender) && RandomHelper.nextDouble() < pbcast_freq) {
			byte[] array = new byte[7];
		    new Random().nextBytes(array);
		    String generatedString = new String(array, Charset.forName("UTF-8"));
			this.pbcast(new Message(generatedString));
		}
		
		while(workload > 0 && (!this.out_messages.isEmpty() || !this.in_messages.isEmpty())) {			
			// Remove edges from previous step
			for (RepastEdge<Object> edge : this.my_edges)
				send_net.removeEdge(edge);
			this.my_edges.clear();
			
			// select the type of work to do
			boolean done = false;
			int work = RandomHelper.nextIntFromTo(1, ACTIVITIES);
			if(work == 1 && !this.in_messages.isEmpty()) {
				this.receive();
				done = true;
			} else if(work == 2 && !this.out_messages.isEmpty()) {
				this.send();
				done = true;
			}
			if(done)
				workload--;
		}
	}

	@ScheduledMethod(start = 1, interval = 10)
	public void clear_exchange_networks() {
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> exchange_net = (Network<Object>) context.getProjection("exchange");
		Network<Object> loss_net = (Network<Object>) context.getProjection("loss network");
		Network<Object> ret_net = (Network<Object>) context.getProjection("retrieved network");
		exchange_net.removeEdges();
		loss_net.removeEdges();
		ret_net.removeEdges();
	}
	
	@ScheduledMethod(start = 2, interval = 1)
	public void anti_entropy() {
		Parameters params = RunEnvironment.getInstance().getParameters();

		// Check incoming gossip messages
		while (!this.gossip_messages.isEmpty())
			this.receive_gossip();
		
		// Answer incoming solicitate messages
		while (!this.solicitation_messages.isEmpty())
			this.answer_to_solicitation();
		
		// Start anti-entropy protocol
		double gossip_prob = (double) params.getValue("gossip_prob");
		if (!this.history.isEmpty() && RandomHelper.nextDouble() < gossip_prob)
			gossip_round();
	}
}
