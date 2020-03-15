package chord;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;

public class ChordManager {
	
	private ContinuousSpace<Object> space;
	private Context<Object> context;
	private Parameters params;
	private int maxHashCount = 0;
	private int initNodeCount = 0;
	private static ArrayList<Node> nodes;
	private int deltaNodes = 10;
	private int maxNodes = 0;
	private int hashCount = 0;
	private static ArrayList<Element> elements;
	private boolean singleReq = false;
	
	public ChordManager(ContinuousSpace<Object> space, Context<Object> context) {
		this.space = space;
		this.context = context;
		params = RunEnvironment.getInstance().getParameters();
		maxHashCount = params.getInteger("max_hash_count");
		maxNodes = maxHashCount;
		hashCount = params.getInteger("hash_count");
		initNodeCount = params.getInteger("node_count");
		nodes = new ArrayList<Node>();
		elements = new ArrayList<Element>();
		initElements();
		initNodes();
	}
	
	//function to initiate the keys already in the chord ring
	private void initElements() {
		while(elements.size() < hashCount) {
			addElement();
		}
	}
	
	//function to see if a node has crashed
	public static boolean hasCrashed(Node n) {
		return !nodes.contains(n);
	}
	
	//function that adds an element to the right node
	private void addElement() {
		boolean add = false;
		while(!add) {
			Element toAdd = new Element(RandomHelper.nextIntFromTo(1, maxHashCount), "" + RandomHelper.nextIntFromTo(1, 1000000000));
			if(!elements.contains(toAdd)) {
				elements.add(toAdd);
				add = true;
			}
		}
	}
	
	
	//function to create the chord ring and add the right number of nodes, stabilizing the whole ring
	private void initNodes() {
		addNode(null);
		while(nodes.size() < initNodeCount) {
			addNode(nodes.get(0));
			for(Node n : nodes) {
				System.out.println("-- Routine stabilize");
				n.stabilize();
				n.fixfingers();
			}
		}
		for(Node n : nodes) {
			n.stabilize();
			n.fixfingers();
		}
		for(Node n : nodes) {
		System.out.println(n);
		}
		for(Node n : nodes) {
			ArrayList<Element> toAdd = new ArrayList<Element>();
			Node prec = n.getPredecessor();
			for(Element e : elements) {
				if((e.getKey() > prec.getHashId() && e.getKey() <= n.getHashId()) 
						|| (prec.getHashId() > n.getHashId() && (e.getKey() > prec.getHashId() || e.getKey() <= n.getHashId()))) {
					toAdd.add(e);
				}
			}
			n.setElements(toAdd);
		}
	}
	
	private void addNode(Node ref) {
		double angleInterval = Math.toRadians(360.0 / maxHashCount);
		double angleOffset = Math.toRadians(90.0);
		double radius = 20;
		double x = 0;
		double y = 0;
		boolean add = false;
		while(!add) {
			Node toAdd = new Node(RandomHelper.nextIntFromTo(1, maxHashCount), space);
			if(!nodes.contains(toAdd)) {
				nodes.add(toAdd);
				context.add(toAdd);
				x = Math.cos((angleInterval * (-toAdd.getHashId())) + angleOffset) * radius;
				y = Math.sin((angleInterval * (-toAdd.getHashId())) + angleOffset) * radius;
				space.moveTo(toAdd, x  + 25.0, y + 25.0);
				if(ref!=null){
					toAdd.join(ref);
				}else {
					toAdd.create();
				}
				add = true;
			}
		}
	}
	
	//removes a node from the network
	private void removeNode() {
		Node toRemove = nodes.remove(RandomHelper.nextIntFromTo(0, nodes.size()-1));
		toRemove.notifyRemove();
		context.remove(toRemove);
	}

	//method to select a random node and make it crash
	private void crashNode() {
		Node toRemove = nodes.remove(RandomHelper.nextIntFromTo(0, nodes.size()-1));
		context.remove(toRemove);
	}
	//method to make a specific node crash
	private void crashNode(Node toRemove) {
		context.remove(toRemove);
	}
	
	//get number of nodes currently in the network
	public int getNodesInChord() {
		return nodes.size();
	}
	
	/*function called each tick
	 * 	each time there's a probability a node will crash, a new node joins and a node will leave the network
	 * 
	 * 	every 50 ticks calls checkpredecessor, stabilize and fixfingers on each node, stabilizing the network
	 * 	
	 * every 200 ticks will make a new lookup rquest
	 */
	@ScheduledMethod(start = 0, interval = 1)
	public void step() {
		int ticks = (int)RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		if(nodes.size() > 1 && RandomHelper.nextIntFromTo(1, 100000)<=5) {
			crashNode();
		}
		if(ticks % 50 == 0) {
			for(Node n: nodes) {
				n.checkpredecessor();
			}
		}
		if(ticks % 50 == 0) {
			for(Node n: nodes) {
				n.stabilize();
			}
		}
		if(ticks % 50 == 0) {
			for(Node n: nodes) {
				n.fixfingers();
			}
		}
		
		for(Node n: nodes) {
			n.updatePending();
		}
		if(nodes.size() < maxNodes && RandomHelper.nextIntFromTo(1, 100000)<=14) {
			addNode(nodes.get(0));
		}
		if(nodes.size() > 1 && RandomHelper.nextIntFromTo(1, 100000)<=5) {
			removeNode();
		}
		if(ticks % 200 == 0 && ticks !=0) {
			if(!singleReq) {
				if(isStable()) {
					Node chosen = nodes.get(RandomHelper.nextIntFromTo(0, nodes.size()-1));
					System.out.println(chosen.getHashId()+" is asking who owns a element");
					System.out.println(chosen);
					chosen.lookup(RandomHelper.nextIntFromTo(0, maxHashCount));
					System.out.println("DONE");
				}
			}
		}
	}

	//function that checks if all nodes are stable
	private boolean isStable() {
		boolean toRtn = true;
		for(Node n : nodes) {
			toRtn = toRtn && (n.getPredecessor()!=null && n.getSuccessor()!=null);
		}
		return toRtn;
	}
}
