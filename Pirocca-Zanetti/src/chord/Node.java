package chord;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import chord.utils.NetworkConfiguration;
import repast.simphony.engine.schedule.ScheduledMethod;


/**
 * @author Simone Pirocca
 */
public class Node 
{
	// generaral data
	private String ip;
	private BigInteger id;
	private boolean joined, crashed, predHasNotified;
	private boolean firstNode, firstNotifyReceived;
	private int offset, periodClock, clock, next;
	private double randomJoinThreashold, randomDisjoinThreashold;
	
	// data realted with other nodes or keys
	private ArrayList<Node> fingerTable;
	private Hashtable<BigInteger, String> keySet;
	private Node [] successor;
	private Node predecessor;
	private Node known;
	
	// higher level parameters
	private NetworkConfiguration conf;
	private SuperAgent superAgent;
	
	/**
	 * @param conf
	 */
	public Node(NetworkConfiguration conf) 
	{
		this.joined = false;
		this.crashed = false;
		this.predHasNotified = false;
		this.conf = conf;
		this.fingerTable = new ArrayList<Node>();
		this.keySet = new Hashtable<BigInteger, String>();
		this.successor = new Node [conf.getR()];
		this.predecessor = null;
		this.known = null;

		this.randomJoinThreashold = (int) Math.random()*60;
		this.randomDisjoinThreashold = (int) Math.random()*20;
		this.offset = (int) Math.random()*conf.getPeriod();
		this.clock = offset;
		this.periodClock = clock;
		this.next = 0;
	}

	/**
	 * Check the value of the clock in order to know
	 * if the period must be reset, and then launch
	 * methods that work only in the beginning of the period
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void checkClock()
	{
		clock++;
		periodClock++;
		
		// start all periodically checks 
		// in case of a new period 
		if(periodClock == conf.getPeriod())
		{
			periodClock = 0;
			
			stabilize();
			fixFingers();
			checkPredecessor();
		}
		// otherwise change state
		else changeState();
		
	}
	
	/**
	 * The node responsible for the key will 
	 * store or update the information
	 * @param key
	 * @param value
	 */
	public boolean setValueOfKey(BigInteger key, String value)
	{
		// if this is the node responsible for that key
		if((predecessor != null) && (isWithinRange(predecessor.getId(), id, key, true)))
		{
			keySet.put(key, value);
			//System.out.println(id + " --> store (" + key + "," + value + ")");
			return true;
		}
		// otherwise ask to a finger
		else
		{
			Node dest = findSuccessor(key, null);
			// the node has crashed or is itself
			if((dest == null) || dest == this) return false;
			//System.out.println(id + " selected finger: " + dest.getId());
			else return dest.setValueOfKey(key, value);
		}
	}
	
	/**
	 * The node responsible for the key
	 * will answer with the associated value
	 * @param key
	 * @return
	 */
	public QueryResponse getValueOfKey(BigInteger key, int pathLength, Node sender)
	{
		QueryResponse response = new QueryResponse();
		Node dest = findSuccessor(key, response);
		
		// the node has crashed or is itself
		if((dest == null) || dest == this) return null;
		
		String tmpValue = dest.keySet.get(key);
		// if this is the node responsible for that key
		if(tmpValue != null) 
		{
			response.setKey(key);
			response.setValue(tmpValue);
			response.setSender(sender);
			response.setLiable(this);
		}
		
		return response;
	}
	
	/**
	 * 
	 * @param a the lower bound
	 * @param b the upper bound
	 * @param c the value to test
	 * @param lessOrEqual if we need to check if 'c' is less than of equal to 'b', 
	 * 			instead of onlt less than
	 * @return if 'c' is within 'a' and 'b' in modular arithmetic
	 */
	private boolean isWithinRange(BigInteger a, BigInteger b, BigInteger c, boolean lessOrEqual)
	{
		// if a < b (normal case)
		if(a.compareTo(b) == -1)
		{
			if(!lessOrEqual)
			{
				// return true only if a < c < b
				if((a.compareTo(c) == -1) && (c.compareTo(b) == -1)) return true;
				else return false;
			}
			else
			{
				// return true only if a < c <= b
				if((a.compareTo(c) == -1) && ((c.compareTo(b) == -1) || (c.compareTo(b) == 0))) return true;
				else return false;
			}
		}
		// if a > b (if the interval is crossing 0)
		else if(a.compareTo(b) == 1)
		{
			if(!lessOrEqual)
			{
				// return false if b <= c <= a (it is outside the interval in this case)
				if(((b.compareTo(c) == -1) || (b.compareTo(c) == 0)) && ((c.compareTo(a) == -1) || (c.compareTo(a) == 0))) return false;
				else return true;
			}
			else
			{
				// return false if b < c <= a
				if((b.compareTo(c) == -1) && ((c.compareTo(a) == -1) || (c.compareTo(a) == 0))) return false;
				else return true;
			}
		}
		// if a == b; return true if also a == c, otherwise false
		else if(a.compareTo(c) == 0) return true;
		else return false;
	}
	
	/**
	 *  Verify successor, and tells the successor about him.
	 */
	private synchronized void stabilize()
	{
		if((joined) && (!crashed))
		{
			//System.out.println(id + " --> Stabilize");
			Node tmpSucc = successor[0].getPredecessor();
			
			// check the predecessor only if it not null
			if(tmpSucc != null)
			{
//				System.out.println("tmpSucc hash: " + tmpSucc.getId().toString(16));

				// if successor's predecesor's id is a highier than its id
				if(isWithinRange(id, successor[0].getId(), tmpSucc.getId(), false))
				{
					successor[0] = tmpSucc;
//					fingerTable[0] = tmpSucc;
					fingerTable.set(0, tmpSucc);
					
					//System.out.println(id + " changed the succ to: " + tmpSucc.getId());
				}

				// notify the successor that he's still alive
				successor[0].notify(this);
			}
		}
	}
	
	/**
	 * The list of successor's successors is given,
	 * to stabilize them
	 * @param succ
	 * @param succs
	 */
	public void stabilizeSuccessors(Node succ, Node[] succs)
	{
		successor[0] = succ;
		for(int i=0; i<(successor.length-1); i++)
		{
			successor[i+1] = succs[i];
		}
	}
	
	/**
	 * Refreshes finger table entries.
	 * next stores the index of the next finger to fix.
	 */
	private synchronized void fixFingers()
	{
//		System.out.println("\nfixFingers()");
		
		if((joined) && (!crashed))
		{
			next++;
			if(next == conf.getM()) next = 0;
			
			// find the right finger
			Node newFinger;
			newFinger = findSuccessor((id.add(BigInteger.valueOf((long) Math.pow(2, next)))), null);
			
			if(next <= fingerTable.size() -1)
			{
				//System.out.print(id + " --> SET to finger[" + next + "] " + newFinger.getId());
				fingerTable.set(next, newFinger);
			}else
			{
				//System.out.print(id + " --> ADD to finger[" + next + "] " + newFinger.getId());
				fingerTable.add(next, newFinger);
			}
			
			/*System.out.print("\nFINGER TABLE OF " + id + ": [");
			for(int i=0;i<fingerTable.size();i++) System.out.print(fingerTable.get(i).getId() + "-");
			System.out.print("]\n");*/
		}
	}
	
	/**
	 * Checks whether predecessor has failed
	 */
	private void checkPredecessor()
	{
		if((joined) && (!crashed))
		{
			if(!predHasNotified)
				predecessor = null;
			else if(predecessor != null)
				predecessor.stabilizeSuccessors(this, successor);
		}
	}
	
	/**
	 * Notification from its predecessor 
	 * that the predecessor may be changed 
	 * @param predecessor
	 */
	private void notify(Node pred)
	{
		if(!crashed)
		{
			//System.out.println(id + " --> " + pred.getId() + " notified me!");
			// if there is a new or higher predecessor
			if((predecessor == null) || (isWithinRange(predecessor.getId(), id, pred.getId(), false)))
			{
				if(firstNode && !firstNotifyReceived)
				{
					//System.out.println("FIRST NOTIFY RECEINVED BY: " + pred.getId());
					firstNotifyReceived = true;
					successor[0] = pred;
					//System.out.println(id + " changed the succ to: " + successor[0].getId());
					pred.notify(this);
				}
				
				predecessor = pred;
				
				//System.out.println(id + " changed the pred with: " + predecessor.getId());
				
				splitKeys(pred);
			}

			predHasNotified = true;
		}
	}
	
	/**
	 * After a new predecessor has been discovered,
	 * delete wrong keys and give them to the new node
	 * @param succ
	 */
	private void splitKeys(Node pred)
	{
		//System.out.print("\n" + id + " --> keys given to " + pred.getId() + ": ");
		Hashtable<BigInteger, String> givenKeys = new Hashtable<BigInteger, String>();
		Enumeration<BigInteger> enumKeys = keySet.keys();
		while(enumKeys.hasMoreElements())
		{
			BigInteger tmpKey = enumKeys.nextElement();
			BigInteger keyValue = tmpKey;
			
			// if this key should be managed by the new node,
			// add it to the new list and remove from the own one
			if(isWithinRange(predecessor.getId(), pred.getId(), keyValue, true))
			{
				//System.out.print(keyValue + ",");
				// remove the key and put in the new set
				givenKeys.put(keyValue, keySet.remove(keyValue));
			}
		}
		
		// and finally give the right keys to the new predecessor
		if(!givenKeys.isEmpty())
			predecessor.giveKeysToMe(givenKeys);
	}
	
	/**
	 * New keys are given from the successor
	 * in case it is a new node
	 * @param newKeys
	 */
	public void giveKeysToMe(Hashtable<BigInteger, String> newKeys)
	{
		keySet.putAll(newKeys);
	}
	
	/**
	 * Sometime, randomly a node join or disjoin the network.
	 * The more the time passes without changes,
	 * the more is probable that the node is going to change state
	 */
	private void changeState()
	{
//		System.out.println("\nchangeState()");
//		System.out.println("node isJoined ->" + this.isJoined());
		
		// crash check
		if(!crashed)
		{
			// every tick increase the threashold
			double random = Math.random()*100;
			
			if(joined)
			{
				//randomDisjoinThreashold += 0.000005;
				// after increasing the threashold,
				// if the random value is lower than the threashold
				// set it to 0 and join the network
				if(random < randomDisjoinThreashold) 
				{
					randomDisjoinThreashold = 0;
					disjoin();
				}
			}
			else
			{
				randomJoinThreashold += 0.0001;
				// after increasing the threashold,
				// if the random value is lower than the threashold
				// set it to 0 and disjoin the network
				if(random < randomJoinThreashold) 
				{
					randomJoinThreashold = 0;
					join();
				}
			}
		}
	}
	
	/**
	 * Join the network
	 */
	private synchronized void join()
	{
//		System.out.println("\njoin()");
		
		if((!joined) && (!crashed))
		{
						
			// ask a node already joined
			known = superAgent.getRandomNode();
			
			// create the Chord ring
			if(known == null && !superAgent.isChordRingCreated()) 
			{
				superAgent.setChordRingCreated(true);
				
				//System.out.println(id + " created the Chord ring");
				
				predecessor = null;
				successor[0] = this;
				fingerTable.add(this);
				
				firstNode = true;
				
				//joined = true;
			}
			// join the Chord ring 
			else
			{
				predecessor = null;
				successor[0] = known.findSuccessorForMe(id, this, null);
				
				// in case the selected node has crashed,
				// ask for a new node to contact
				while(successor[0] == null)
				{
					do
						known = superAgent.getRandomNode();
					while(known == null);
					successor[0] = known.findSuccessorForMe(this.id, this, null);
				}
				
				fingerTable.add(successor[0]);
				//System.out.println("\n" + id + " joined the Chord ring, successor: " + successor[0].getId());			
				
				// notify immediately the successor
				// to get the keys
				successor[0].notify(this);
			}
			
			joined = true;
			superAgent.incJoinedNodes();
		}
	}
	
	/**
	 * Disjoin the network
	 */
	private void disjoin()
	{
		
//		System.out.println("disjoin()");
		
		if((joined) && (!crashed))
		{
			// let successor and predecessor know 
			// that he's going to leave
			// and give to him all the keys
			successor[0].giveKeysToMe(keySet);
			successor[0].notifyLeaving(this, predecessor);
			if(predecessor != null)
				predecessor.notifyLeaving(this, successor[(successor.length - 1)]);
			
			// clear useless hashtables
			keySet.clear();
			fingerTable.clear();
			predecessor = null;
			next = 0;

			firstNode = false;
			firstNotifyReceived = false;
			joined = false;
		}
	}
	
	/**
	 * Notification from successor or predecessor
	 * that he's going to leave
	 * @param node
	 */
	public void notifyLeaving(Node node, Node update)
	{
		// if the notification comes from the successor
		if(node == successor[0])
		{
			for(int i=0; i<fingerTable.size(); i++)
			{
				// find the node in the fingertable corresponding to that node,
				if(fingerTable.get(i) != null)
				{
					if(fingerTable.get(i).getId() == node.getId())
					{
						int j;
						// than remove the element filling the empty space
						for(j=i+1; j<fingerTable.size(); j++)
						{
							fingerTable.set(j-1, fingerTable.get(j));
						}
						// and inserting as the last element
						// the lst finger of the successor
						if(j <= fingerTable.size() -1)
						{
							fingerTable.set(j, update);
						}else
						{
							fingerTable.add(j, update);
						}
						
						break;
					}
				}
			}
		}
		else if(node == predecessor)
		{
			// otherwise, if the notification
			// comes from the predecessor,
			// update the predecessor with the predecessor
			// or the predecessor that sent the notification
			predecessor = update;
		}
	}
	
	/**
	 * Return the successor of a certain node,
	 * as a request asked by another node
	 * @param nodeId
	 * @return successor
	 */
	public Node findSuccessorForMe(BigInteger nodeId, Node sender, QueryResponse response)
	{
		if((joined) && (!crashed))
		{
			//System.out.println(sender.getId() + " asked successor of " + nodeId + " to me:" + id);
			// if the sender was itself, return itself
			// (valid for all the nodes except for the first one)
			if(sender != this)
					return findSuccessor(nodeId, response);
			else return null;
		}
		else return null;
	}
	
	/**
	 * Return the first successor node
	 * @param nodeId
	 * @return successor
	 */
	private synchronized Node findSuccessor(BigInteger nodeId, QueryResponse response)
	{
		// if its successor is also the successor of the 'nodeId', return it
		
		if (response != null) 
		{
			response.setPathLength(response.getPathLength() + 1);
		}		
		
		if(isWithinRange(id, successor[0].getId(), nodeId, true))
		{
			return successor[0];
		}
		// otherwise forward the query to the closest preceding node
		else
		{
			// if it is te first node without any other one, return itself
			if(!firstNode || firstNotifyReceived)
			{
				Node tmpNode = closestPrecedingNode(nodeId);
				return tmpNode.findSuccessorForMe(nodeId, this, response);
			}
			else return this;
		}
	}
	
	/**
	 * Return the closest preceding node
	 * @param nodeId
	 * @return finger
	 */
	private synchronized Node closestPrecedingNode(BigInteger nodeId)
	{
		// search within the finger table,
		// from the last element to the first
		for(int i=fingerTable.size()-1 ; i>= 0; i--)
		{
			Node finger = fingerTable.get(i);
			
			if(finger != null)
			{
				BigInteger fingerId = finger.getId();
				// until found a finder whose id is between 
				// its id and the id of the asked node
				if(isWithinRange(id, nodeId, fingerId, false)) return finger;
			}
		}

		return this;
	}

	/**
	 * @return the id
	 */
	public BigInteger getId() 
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(BigInteger id) 
	{
		this.id = id;
	}

	/**
	 * @return the predecessor
	 */
	public Node getPredecessor() 
	{
		return predecessor;
	}

	/**
	 * @param superAgent the superAgent to set
	 */
	public void setSuperAgent(SuperAgent superAgent) 
	{
		this.superAgent = superAgent;
	}
	
	/**
	 * 
	 * @return if the process is crashed
	 */
	public boolean isCrashed() 
	{
		return crashed;
	}

	/**
	 * 
	 * @param crashed
	 */
	public void setCrashed(boolean crashed) 
	{
		this.crashed = crashed;
	}

	
	public boolean isJoined() 
	{
		return joined;
	}

	public void setJoined(boolean joined) 
	{
		this.joined = joined;
	}

	public String getIp() 
	{
		return ip;
	}

	public void setIp(String ip) 
	{
		this.ip = ip;
	}

	public ArrayList<Node> getFingerTable() 
	{
		return fingerTable;
	}
	
	public int getFingersNumber() 
	{
		return fingerTable.size();
	}
	
	public int getKeysNumber() 
	{
		return keySet.size();
	}
}
