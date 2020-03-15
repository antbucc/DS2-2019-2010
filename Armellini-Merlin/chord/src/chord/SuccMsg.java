package chord;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.util.ContextUtils;


/*
 * Class used to ask another node if he has a certain key
 */
public class SuccMsg {
	
	private Node target;
	private Node sender;
	private Node succ;
	private NdPoint targetPos;
	private NdPoint senderPos;
	private int key;
	private boolean request;
	private boolean owner;
	private ContinuousSpace<Object> space;
	public boolean moved = false;
	
	public SuccMsg(int key, Node target, Node sender, ContinuousSpace<Object> space, boolean request) {
		this.space = space;
		this.key = key;
		this.targetPos = space.getLocation(target);
		this.senderPos = space.getLocation(sender);
		this.request = request;
		this.target = target;
		this.sender = sender;
		succ = null;
		owner = false;
	}
	
	public SuccMsg(int key, Node target, Node sender, ContinuousSpace<Object> space, boolean request, Node succ) {
		this.space = space;
		this.key = key;
		this.targetPos = space.getLocation(target);
		this.senderPos = space.getLocation(sender);
		this.request = request;
		this.target = target;
		this.sender = sender;
		this.succ = succ;
		owner = false;
	}
	
	public SuccMsg(int key, Node target, Node sender, ContinuousSpace<Object> space, boolean request, Node succ, boolean owner) {
		this.space = space;
		this.key = key;
		this.targetPos = space.getLocation(target);
		this.senderPos = space.getLocation(sender);
		this.request = request;
		this.target = target;
		this.sender = sender;
		this.succ = succ;
		this.owner = owner;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		if(!ChordManager.hasCrashed(target)) {
			NdPoint myPoint = space.getLocation(this);
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, targetPos);
			space.moveByVector(this, 1.0, angle, 0);
			moved  = true;
		} else {
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(this);
		}
	}
	
	public double distanceFromTarget() {
		NdPoint myPoint = space.getLocation(this);
		double distance = Math.sqrt(Math.pow(myPoint.getX() - targetPos.getX(), 2.0) + Math.pow(myPoint.getY() - targetPos.getY(), 2.0));
		return distance;
	}
	
	public Node getTarget() {
		return target;
	}
	
	public Node getSender() {
		return sender;
	}
	
	public Node getSucc() {
		return succ;
	}
	
	public boolean getRequest() {
		return request;
	}
	
	public boolean getOwner() {
		return owner;
	}
	
	public int getKey() {
		return key;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj.getClass() == this.getClass()) {
			return key == ((SuccMsg)(obj)).getKey();
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return key;
	}
}
