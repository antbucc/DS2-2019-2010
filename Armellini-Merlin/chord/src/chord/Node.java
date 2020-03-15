package chord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.util.ContextUtils;

public class Node {
	
	private int hashId;
	private ArrayList<Node> successor;
	private Node predecessor;
	private ArrayList<Element> myElements;
	private HashMap<Integer, Node> fingers;
	private Context<Object> context;
	private int maxHashCount = 0;
	private Parameters params;
	private ContinuousSpace<Object> space;
	private HashMap<SuccMsg, Integer> pendingRequests;

	
	public Node(int hash, ContinuousSpace<Object> space) {
		hashId = hash;
		myElements = new ArrayList<Element>();
		fingers = new HashMap<Integer, Node>();
		successor = new ArrayList<Node>();
		predecessor = null;
		params = RunEnvironment.getInstance().getParameters();
		maxHashCount = params.getInteger("max_hash_count");
		pendingRequests = new HashMap<SuccMsg, Integer>();
		this.space = space;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String fingersString = "";
		for(Integer c : fingers.keySet()) {
			fingersString+="\n\t\t"+c+"  --  "+fingers.get(c).hashId;
		}
		String succString = "";
		for(int i = 0;i<getSuccessors().size();i++) {
			succString+="\n\t\t"+i+"  --  "+successor.get(i).getHashId();
		}
		return "NODE "+getHashId()+"\n\tpredecessor:"+((predecessor==null)?"null":predecessor.hashId)+"\n\tsuccessor:"+succString+"\n\tFingers"+fingersString;
	}
	
	@Watch(watcheeClassName = "chord.SuccMsg",
			watcheeFieldNames = "moved",
			triggerCondition = "$watchee.getTarget() == $watcher && $watchee.distanceFromTarget() < 1.0",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void receiveSucc() {
		context = ContextUtils.getContext(this);
		HashSet<SuccMsg> toReceive = new HashSet<SuccMsg>();
		for(Object msg : context.getObjects(SuccMsg.class)) {
			SuccMsg tmp = (SuccMsg)msg;
			if(tmp.getTarget() == this && tmp.distanceFromTarget() < 1.0) {
				toReceive.add(tmp);
			}
		}
		handleMsg(toReceive);
	}
	
	public void create() {
		System.out.println("Node "+getHashId()+" creating chord ring");
		predecessor=null;
		successor.add(this);
		fixfingers();
		System.out.println(this);
	}
	
	public void join(Node ref) {
		System.out.println("node "+getHashId()+" joining with ref: "+ref.getHashId());
		predecessor = null;
		Node suggestedSucc = ref.findSuccessorStab(hashId, true);
		successor.add(suggestedSucc);
		System.out.println("My successor is node " + suggestedSucc.getHashId());
		System.out.println("-- Join stabilize");
		stabilize();
		fixfingers();
		System.out.println(this);
	}
	
	public void stabilize() {
		while (ChordManager.hasCrashed(getSuccessor())) {
			System.out.println("Node "+getSuccessor().getHashId()+" crashed, replacing him");
			successor.remove(0);
			if (successor.size() == 0) {
				successor.add(this);
			}
		}
		Node x = successor.get(0).getPredecessor();
		if(x != null) {
			if (Utils.computeCDistance(getHashId(), x.getHashId(), maxHashCount) > 0 && Utils.computeCDistance(getHashId(), x.getHashId(), maxHashCount) <Utils.computeCDistance(getHashId(), successor.get(0).getHashId(),maxHashCount)) {
				successor.clear();
				successor.add(x);
			} else {
				Node tmp = successor.get(0);
				successor.clear();
				successor.add(tmp);
			}
		}
		ArrayList<Node> toAdd = (ArrayList<Node>)getSuccessor().getSuccessors();
		for(int i = toAdd.size()-1; i>=0;i--) {
			if(toAdd.get(i) != successor.get(0) && toAdd.get(i) != this) {
				successor.add(1,toAdd.get(i));
			}
		}
		while(successor.size() > getM()) {
			successor.remove(toAdd.size()-1);
		}
		successor.get(0).notify(this);
	}
	
	public void notify(Node n) {
		if ((predecessor==null || Utils.computeCDistance(predecessor.getHashId(), getHashId(), maxHashCount) == 0) || Utils.computeCDistance(predecessor.getHashId(), n.getHashId(), maxHashCount)< Utils.computeCDistance(predecessor.getHashId(), getHashId(), maxHashCount)) {
			Node tmp = predecessor;
			predecessor = n;
			
			ArrayList<Element> toRmv = new ArrayList<Element>();
			for(Element e : getElements()) {
				if(e.hashCode()<predecessor.getHashId()) {
					toRmv.add(e);
				}																																																								
			}
			getElements().removeAll(toRmv);
			predecessor.addElement(toRmv);
			
			if(successor.get(0) == this) {
				successor.clear();
				successor.add(n);
			}
		}
	}
	
	private void addElement(ArrayList<Element> el) {
		getElements().addAll(el);
		
	}
	private void addElement(Element el) {
		getElements().add(el);
	}

	public void fixfingers() {
		for(int i = 1; i<=getM(); i++) {
			int key = (getHashId()+(int)(Math.pow(2, i-1))) % maxHashCount;
			Node n = findSuccessorStab(key, false);
			Node prev = fingers.put(key, n);
		}
	}
	
	public static boolean hasfailed(Node n) {
		return ChordManager.hasCrashed(n);
	}
	
	public void checkpredecessor() {
		if (predecessor!= null && hasfailed(predecessor))
			predecessor = null;
	}

	
	
	private void handleMsg(HashSet<SuccMsg> toReceive) {
		context = ContextUtils.getContext(this);
		for(SuccMsg msg : toReceive) {
			context.remove(msg);
			if(msg.getRequest()) {
				SuccMsg reply;
				System.out.println("Request received for id " + msg.getKey() + " from " + msg.getSender().getHashId());
				System.out.println(this);
				if(!ownKey(msg.getKey())) {
					Node suggested = findSuccessor(msg.getKey());
					System.out.println("Suggesting " + suggested.hashId);
					reply = new SuccMsg(msg.getKey(), msg.getSender(), msg.getTarget(), space, false, suggested);
				} else {
					System.out.println("I am the owner");
					reply = new SuccMsg(msg.getKey(), msg.getSender(), msg.getTarget(), space, false, this, true);
				}
				NdPoint spacePt = space.getLocation(this);
				context.add(reply);
				space.moveTo(reply, spacePt.getX(), spacePt.getY());
			} else if(pendingRequests.containsKey(msg)) {
				Node succ = msg.getSucc();
				if(msg.getOwner()) {
					System.out.println("Query complete, Node " + succ.getHashId() + " owns " + msg.getKey());
				} else {
					pendingRequests.remove(msg);
					askSuccessor(msg.getKey(), succ);
				}
			}
		}
	}
	
	public void updatePending() {
		ArrayList<SuccMsg> toRemove = new ArrayList<SuccMsg>();
		for(SuccMsg m : pendingRequests.keySet()) {
			pendingRequests.put(m, pendingRequests.get(m)+1);
			if(pendingRequests.get(m) > 100) {
				toRemove.add(m);
			}
		}
		for(SuccMsg m : toRemove) {
			pendingRequests.remove(m);
		}
	}
	
	public void lookup(int key) {
		System.out.println("NODE "+getHashId()+" lookup for key "+ key);
		if(!ownKey(key)) {
			Node n = findSuccessor(key);
			System.out.println("\tNOT MINE, ASKING "+n.getHashId());
			askSuccessor(key, n);
		} else {
			System.out.println("Query complete, Node " + this.getHashId() + " owns " + key);
		}
	}
	
	private boolean ownKey(int key) {
		boolean toRtn = false;
		if(Utils.computeCDistance(predecessor.getHashId(), getHashId(), maxHashCount) == 0 || Utils.computeCDistance(predecessor.getHashId(), key, maxHashCount) <= Utils.computeCDistance(predecessor.getHashId(), getHashId(), maxHashCount)) {
			toRtn = true; 
		} else {
			toRtn = false;
		}
		return toRtn;
	}
	
	private void askSuccessor(int key, Node target) {
		context = ContextUtils.getContext(this);
		NdPoint targetPos = space.getLocation(target);
		if(targetPos != null) {
			NdPoint spacePt = space.getLocation(this);
			SuccMsg toSend = new SuccMsg(key, target, this, space, true);
			context.add(toSend);
			space.moveTo(toSend, spacePt.getX(), spacePt.getY());
			pendingRequests.put(toSend, 0);
		}
	}

	//Find the successor sending a message and waiting for response
	private Node findSuccessor(int key) {
		System.out.println("\townkey "+getHashId()+"\n\tkey "+key+"\n\tsuccessor "+successor.get(0).getHashId());
		if(Utils.computeCDistance(getHashId(), key, maxHashCount)<=Utils.computeCDistance(getHashId(), successor.get(0).getHashId(), maxHashCount)) {
			return successor.get(0);
		} else {
			Node cpn = closestPreceedingNode(key);
			if(cpn.equals(this)) {
				return cpn;
			}else {
				return closestPreceedingNode(key);
			}
		}
	}
	
	//Find the successor without sending a message and waiting for response, used only in stabilize
	private Node findSuccessorStab(int key, boolean debug) {
		if(debug)
			System.out.println(this);
		if(Utils.computeCDistance(getHashId(), key, maxHashCount)<=Utils.computeCDistance(getHashId(), successor.get(0).getHashId(), maxHashCount)) {
			if(debug)
				System.out.println("Succ is my succ");
			return successor.get(0);
		} else {
			Node cpn = closestPreceedingNode(key);
			if(cpn.equals(this)) {
				if(debug)
					System.out.println("Succ is myself");
				return cpn;
			}else {
				if(debug) {
					System.out.println("Ask next for succ");
					return closestPreceedingNode(key).findSuccessorStab(key, true);
				} else {
					return closestPreceedingNode(key).findSuccessorStab(key, false);
				}
			}
		}
	}
	
	private int getM() {
		return (int)(Math.log(maxHashCount)/Math.log(2));
	}
	
	private Node closestPreceedingNode(int key) {
		Node toRtn = this;
		int m = fingers.keySet().size();
		int min = maxHashCount;
		for(int i = m-1; i >= 0; i--) {
			int currentFinger = (hashId + (int)Math.pow(2, i)) % maxHashCount;
			int distanceFinger = Utils.computeCDistance(getHashId(), fingers.get(currentFinger).getHashId(), maxHashCount);
			int distanceId = Utils.computeCDistance(getHashId(), key, maxHashCount);
			if(distanceFinger < distanceId && distanceId - distanceFinger < min && !ChordManager.hasCrashed(fingers.get(currentFinger))) {
				min = distanceId - distanceFinger;
				toRtn = fingers.get(currentFinger);
			}
		}
		return toRtn;
	}
	
	public int getHashId() {
		return hashId;
	}
	
	public void setElements(ArrayList<Element> toSet) {
		myElements = new ArrayList<Element>(toSet);
	}
	
	public void setFingers(HashMap<Integer, Node> toSet) {
		fingers = new HashMap<Integer, Node>(toSet);
	}
	
	public Node getSuccessor() {
		return successor.get(0);
	}
	
	public ArrayList<Node> getSuccessors() {
		return successor;
	}
	
	public void setPredecessor(Node pred) {
		predecessor = pred;
	}
	
	public Node getPredecessor() {
		return predecessor;
	}
	
	public ArrayList<Element> getElements() {
		return myElements;
	}
	
	public HashMap<Integer, Node> getFingers() {
		return fingers;
	}
	
	@Override
	public boolean equals(Object toCompare) {
		if(this.getClass() == toCompare.getClass()) {
			return this.hashId == ((Node)toCompare).getHashId();
		}
		return super.equals(toCompare);
	}
	@Override
	public int hashCode() {
		return hashId;
	}

	public void notifyRemove() {
		Node succ = successor.get(0);
		succ.addElement(myElements);
		myElements.clear();
		if (predecessor != null) {
			succ.notify(predecessor);
		}
	}
}
