package lpb;

import lpb.Utils.*;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;

public class EventFetchMsg {
	private EventId toFetch;
	private Event reply;
	private ContinuousSpace<Object> space;
	public boolean moved = false;
	private Parameters params;
	private int gossipinterval = 2;
	private NdPoint sender;
	private NdPoint receiver;
	private int senderId;
	private int receiverId;
	
	public EventFetchMsg(ContinuousSpace<Object> space, NdPoint sender,int senderId, NdPoint receiver, int receiverId, EventId toFetch, Event reply) {
		params = RunEnvironment.getInstance().getParameters();
		this.gossipinterval = params.getInteger("gossip_interval");
		this.space = space;
		this.sender = new NdPoint(sender.getX(), sender.getY());
		this.receiver = new NdPoint(receiver.getX(), receiver.getY());
		this.toFetch = toFetch;
		this.reply = reply;
		this.senderId = senderId;
		this.receiverId = receiverId;
	}
	
	public EventId getToFetch() {
		return toFetch;
	}
	
	public Event getReply() {
		return reply;
	}
	
	public int getSenderId() {
		return senderId;
	}
	
	public int getReceiverId() {
		return receiverId;
	}
	
	public boolean isReply() {
		return reply!=null;
	}
	
	public EventFetchMsg createReply(Event reply) {
		return new EventFetchMsg(space, receiver, receiverId, sender, senderId, toFetch, reply);
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		NdPoint myPoint = space.getLocation(this);
		double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, receiver);
		space.moveByVector(this, Math.min((5+spaceDiagonal(space))/gossipinterval, space.getDistance(myPoint,receiver) ) , angle, 0);
		moved = true;
	}
	
	private static double spaceDiagonal(ContinuousSpace<Object> space) {
		return Math.sqrt(Math.pow(space.getDimensions().getHeight(),2)+Math.pow(space.getDimensions().getWidth(),2));
	}
	
	public double distanceFromTarget() {
		NdPoint myPoint = space.getLocation(this);
		double distance = Math.sqrt(Math.pow(myPoint.getX() - receiver.getX(), 2.0) + Math.pow(myPoint.getY() - receiver.getY(), 2.0));
		return distance;
	}
}
