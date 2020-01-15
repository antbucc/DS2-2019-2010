package lpb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import lpb.Utils.*;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;

public class GossipMsg {
	private HashMap<Node, Integer> subs;
	private HashMap<Node, Integer> unsubs;
	private HashMap<Event, Integer> events;
	private HashArrayList<EventId> eventIds;
	private int senderId;
	private Node target;
	private ContinuousSpace<Object> space;
	public boolean moved = false;
	private Parameters params;
	private int gossipinterval = 2;
	private NdPoint otherPoint;
	
	public GossipMsg(int senderId, Node target, ContinuousSpace<Object> space, HashMap<Node, Integer> subs, HashMap<Node, Integer> unsubs, HashMap<Event, Integer> events, HashArrayList<EventId> eventIds) {
		params = RunEnvironment.getInstance().getParameters();
		this.senderId = senderId;
		this.gossipinterval = params.getInteger("gossip_interval");
		this.subs = new HashMap<Node,Integer>(subs);
		this.unsubs = new HashMap<Node, Integer>(unsubs);
		this.events = new HashMap<Event, Integer>(events);
		this.eventIds = new HashArrayList<EventId>(eventIds);
		this.target = target;
		this.space = space;
		otherPoint = new NdPoint(target.getLocation().getX(), target.getLocation().getY());
	}
	
	// Step function, move towards target
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		NdPoint myPoint = space.getLocation(this);
		double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
		space.moveByVector(this, Math.min((5+spaceDiagonal(space))/gossipinterval, space.getDistance(myPoint,otherPoint) ) , angle, 0);
		moved = true;
	}
	
	private static double spaceDiagonal(ContinuousSpace<Object> space) {
		return Math.sqrt(Math.pow(space.getDimensions().getHeight(),2)+Math.pow(space.getDimensions().getWidth(),2));
	}
	
	public int getSenderId() {
		return senderId;
	}
	
	public Node getTarget() {
		return target;
	}
	
	public int getTargetId() {
		return target.getNodeId();
	}
	
	public HashMap<Node, Integer> getUnsubs() {
		return unsubs;
	}
	
	public HashMap<Node, Integer> getSubs() {
		return subs;
	}
	
	public Set<Event> getEvents() {
		return events.keySet();
	}

	public int getEventAge(Event e) {
		return events.get(e);
	}
	
	public HashArrayList<EventId> getEventIds() {
		return eventIds;
	}
	
	public double distanceFromTarget() {
		NdPoint myPoint = space.getLocation(this);
		double distance = Math.sqrt(Math.pow(myPoint.getX() - otherPoint.getX(), 2.0) + Math.pow(myPoint.getY() - otherPoint.getY(), 2.0));
		return distance;
	}
	
	}
