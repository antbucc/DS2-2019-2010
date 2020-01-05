package chord;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import communication.Channel;
import communication.Message;
import communication.Node;
import messages.Ack;
import messages.PingRequest;
import messages.PingResponse;
import messages.StabilizeRequest;
import messages.StabilizeResponse;
import messages.SuccessorRequest;
import messages.SuccessorResponse;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import util.FingerNodeVisual;
import util.Key;
import util.KeyRange;
import util.Logger;
import util.Visualizer;

public class ChordNode extends Node {
	
	public static final int M = 160;

	private final Key nodeKey;
	private FingerTable ft = new FingerTable();
	
	private int tickCounter = 0;
	
	private boolean joined = false;
	private boolean joining = false;
	
	private int checkPredecessorPeriod;
	private int fixFingerPeriod;
	private int stabilizePeriod;
	
	private int requestCounter = 0;
	private HashMap<Integer, Consumer<Message>> callbacks = new HashMap<>();
	
	private HashMap<Integer, Integer> timeouts = new HashMap<>();
	private HashMap<Integer, Runnable> timeoutCallbacks = new HashMap<>();
	
	private HashMap<Integer, Integer> nodesTimeouts = new HashMap<>();
	private boolean ackEnabled = true;
	
	private HashMap<Integer, Integer> fingerIndexes = new HashMap<>();
	
	
	public ChordNode(int address, 
					 Parameters params, 
					 Channel channel, 
					 boolean bootstrap) 
	{
		super(address, channel);
		this.nodeKey = new Key(address);		
		this.checkPredecessorPeriod = params.getInteger("checkPredecessorPeriod");
		this.fixFingerPeriod = params.getInteger("fixFingerPeriod");
		this.stabilizePeriod = params.getInteger("stabilizePeriod");
		
		Logger.getLogger().logNewNode(this);
		
		if (bootstrap) {  // Nodes seed+0 and seed+1
			
			ft.bootstrapInit();
			joined = true;
			Logger.getLogger().logNodeJoin(this);
		}
		
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 10)
	public void step() {
		if (!joined) {
			join();
		} else {
			if (tickCounter % stabilizePeriod == 0) {
				stabilize();
			}
			
			if (tickCounter % checkPredecessorPeriod == 0) {
				checkPredecessor();
			}
			
			ft.fixFingers();
		}
		updateTimeouts();
		tickCounter++;
	}

	@Override
	public void receiveMessage(Message msg) {
		if (msg instanceof SuccessorRequest) {
			receiveSuccessorRequest((SuccessorRequest) msg);
		} else if (msg instanceof SuccessorResponse) {
			receiveSuccessorResponse((SuccessorResponse) msg);
		} else if (msg instanceof StabilizeRequest) {
			receiveStabilizeRequest((StabilizeRequest) msg);
		} else if (msg instanceof StabilizeResponse) {
			receiveStabilizeResponse((StabilizeResponse) msg);
		} else if (msg instanceof PingRequest) {
			receivePingRequest((PingRequest) msg);
		} else if (msg instanceof PingResponse) {
			receivePingResponse((PingResponse) msg);
		} else if (msg instanceof Ack) {
			receiveAck((Ack) msg);
		} else {
			throw new UnsupportedOperationException("Message unknown");
		}
	}
	
	private void receiveSuccessorRequest(SuccessorRequest msg) {
		if (joined) {
			Key key = msg.getKey();
			
			if (key.equals(nodeKey)) {
				// We are the successor of ourself
				
				channel.sendMessage(
						new SuccessorResponse(address,
											  msg.getSourceAddr(),
										      address,
										      msg.getRequestId(),
										      msg.getHops() + 1));
				return;
				
			}
			// The node does not know its successor yet
			if (ft.getSuccessorKey() == null) {
				
				channel.sendMessage(
						new SuccessorResponse(address,
											  msg.getSourceAddr(),
										      -1,
										      msg.getRequestId(),
										      msg.getHops() + 1));
				return;
			}
			
			KeyRange range = new KeyRange(nodeKey, ft.getSuccessorKey());
			
			if (range.containsOpenClosed(key)) {
				// Successor of the key found (our successor)
				channel.sendMessage(
						new SuccessorResponse(address,
											  msg.getSourceAddr(),
										      ft.getSuccessor(),
										      msg.getRequestId(),
										      msg.getHops() + 1));
			} else {
				// Forward the request to the closest (preceding) finger to the key
				int destAddr = ft.getClosestPrecedingFinger(key);
				channel.sendMessage(
						new SuccessorRequest(address,
											 destAddr,
											 key,
											 msg.getSourceAddr(),
											 msg.getRequestId(),
											 msg.getHops() + 1));
				if (ackEnabled && !nodesTimeouts.containsKey(destAddr)) {
					nodesTimeouts.put(destAddr, 10);
				}
			}
			
			// Send an ack to the sender of the message
			if (ackEnabled) {
				channel.sendMessage(new Ack(address, msg.getSenderAddr()));
			}
			
		}
	}
	
	private void receiveSuccessorResponse(SuccessorResponse msg) {
		int requestId = msg.getRequestId();
		if (callbacks.containsKey(requestId)) {
			callbacks.get(requestId).accept(msg);
			callbacks.remove(requestId);
			timeouts.remove(requestId);
			timeoutCallbacks.remove(requestId);
		}
	}
	
	private void receiveStabilizeRequest(StabilizeRequest msg) {
		if (joined) {
			channel.sendMessage(
					new StabilizeResponse(address,
										  msg.getSenderAddr(),
										  ft.getPredecessor(),
										  msg.getRequestId()));
			
			// Update the predecessor
			Key senderKey = new Key(msg.getSenderAddr());
			if (ft.getPredecessor().isEmpty()) {
				ft.setPredecessor(Optional.of(msg.getSenderAddr()));
			} else {
				KeyRange range = new KeyRange(ft.getPredecessorKey(), nodeKey);
				if (range.containsOpen(senderKey)) {
					ft.setPredecessor(Optional.of(msg.getSenderAddr()));
				}
			}
		}
	}
	
	private void receiveStabilizeResponse(StabilizeResponse msg) {
		int requestId = msg.getRequestId();
		if (callbacks.containsKey(requestId)) {
			callbacks.get(requestId).accept(msg);
			callbacks.remove(requestId);
			timeouts.remove(requestId);
			timeoutCallbacks.remove(requestId);
		}
	}
	
	private void receivePingRequest(PingRequest msg) {
		channel.sendMessage(
				new PingResponse(address,
								 msg.getSenderAddr(),
								 msg.getRequestId()));
	}
	
	private void receivePingResponse(PingResponse msg) {
		int requestId = msg.getRequestId();
		timeouts.remove(requestId);
		timeoutCallbacks.remove(requestId);
	}
	
	private void receiveAck(Ack msg) {
		nodesTimeouts.remove(msg.getSenderAddr());
	}
	
	private void sendSuccessorRequest(Key key) {
		int destAddr = ft.getClosestPrecedingFinger(key);
		channel.sendMessage(
				new SuccessorRequest(address,
									 destAddr,
									 key,
									 address,
									 requestCounter,
									 0));
		
		if (ackEnabled && !nodesTimeouts.containsKey(destAddr)) {
			nodesTimeouts.put(destAddr, 10);
		}
	}
	
	public Key getNodeKey() {
		return nodeKey;
	}
	
	public int getSuccessorAddr() {
		return ft.getSuccessor();
	}
	
	public int getPredecessorAddr() {
		return ft.getPredecessor().orElse(-1);
	}
	
	public void join() {
		if (!joining) {
			// Send a SuccessorRequest to an arbitrary node
			channel.sendMessageToRandomNode(
					new SuccessorRequest(address,
										 -1,
										 ft.getStart(0),
										 address,
										 requestCounter,
										 0));
			
			// When the node receive a response, save the successor in the 
			// finger table (successor is an alias for ft[0])
			callbacks.put(requestCounter,
					(msg) -> {
						SuccessorResponse response = (SuccessorResponse) msg;
						int successor = response.getSuccessorAddr();
						
						if (successor != -1) {
							ft.setSuccessor(response.getSuccessorAddr());
							joined = true;
							joining = false;
							Logger.getLogger().logNodeJoin(this);
							stabilize();  // Notify our successor
						} else {
							// Retry joining through a different node
							Logger.getLogger().logJoinReTry(this, response.getSenderAddr(), false);
							joining = false;
							join();
						}
					
					});
			
			timeouts.put(requestCounter, 50);
			timeoutCallbacks.put(requestCounter, 
					() -> {
						// Retry joining 
						Logger.getLogger().logJoinReTry(this, -1, false);
						joining = false;
						join();
					});
			
			requestCounter++;
			joining = true;
		}
	}	
	
	private void stabilize() {
		
		// Ask to our successor who is its predecessor
		channel.sendMessage(
				new StabilizeRequest(address,
									   ft.getSuccessor(),
									   requestCounter));
		
		callbacks.put(requestCounter, 
				(msg) -> {
					StabilizeResponse response = (StabilizeResponse) msg;
					// predecessorSuccessor is the predecessor of our successor
					Optional<Integer> predecessorSuccessor = response.getPredecessorAddr();
					
					if (predecessorSuccessor.isPresent()) {
						Key predSuccKey = new Key(predecessorSuccessor.get());
						KeyRange range = new KeyRange(nodeKey, ft.getSuccessorKey());
						
						if (range.containsOpen(predSuccKey)) {
							ft.setSuccessor(predecessorSuccessor.get());
							stabilize();
						} else if (ft.getPredecessor().isEmpty() 
								&& predecessorSuccessor.get() != address) 
						{
							ft.setPredecessor(predecessorSuccessor);
						}
					}
				});
		
		timeouts.put(requestCounter, 20);
		timeoutCallbacks.put(requestCounter, ft::replaceSuccessor);
		requestCounter++;
	}
	
	private void checkPredecessor() {
		ft.getPredecessor().ifPresent(
				(predAddr) -> {
					channel.sendMessage(
							new PingRequest(address,
											predAddr,
											requestCounter));
					
					timeouts.put(requestCounter, 10);
					timeoutCallbacks.put(requestCounter,
							() -> {
								ft.setPredecessor(Optional.empty());
							});
					
					requestCounter++;	
				});
	}
	
	private void updateTimeouts() {
		ArrayList<Integer> timeoutsCompleted = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : timeouts.entrySet()) {
			entry.setValue(entry.getValue() - 1);			
			if (entry.getValue() == 0) {
				timeoutsCompleted.add(entry.getKey());
			}
		}
		
		for (int requestId : timeoutsCompleted) {
			timeoutCallbacks.get(requestId).run();
			timeouts.remove(requestId);
			timeoutCallbacks.remove(requestId);
			callbacks.remove(requestId);
			fingerIndexes.remove(requestId);
		}
		
		if (ackEnabled) {
			HashSet<Integer> timedOutNodes = new HashSet<>();	
			for (Map.Entry<Integer, Integer> entry : nodesTimeouts.entrySet()) {
				entry.setValue(entry.getValue() - 1);
				if (entry.getValue() == 0) {
					timedOutNodes.add(entry.getKey());
				}
			}
			
			for (int i = 0; i < M; i++) {
				if (timedOutNodes.contains(ft.getFinger(i))) {
					ft.setFinger(i, -1);
					if (i == 0) {
						ft.replaceSuccessor();
					}
				}
			}
			
			for (int timedOutNode : timedOutNodes) {
				nodesTimeouts.remove(timedOutNode);
			}
		}

		
	}
	
	public class FingerTable {
		private int[] fingerTable = new int[160];
		private Key successorKey;
		private Optional<Integer> predecessor = Optional.empty();
		private Key predecessorKey;
		
		private boolean initCompleted = false;
		private int fixFingersIndex = 1;
		
		private HashSet<FingerNodeVisual> visualFingers = new HashSet<>();
		
		public FingerTable() {
			for (int i = 0; i < M; i++) {
				fingerTable[i] = -1;
			}
		}
		
		public int getFinger(int index) {
			return fingerTable[index];
		}
		
		public void setFinger(int index, int nodeAddr) {
			fingerTable[index] = nodeAddr;
			if (index == 0) successorKey = new Key(nodeAddr);
			
			if (getSuccessor() == address) {
				throw new RuntimeException("A node can't be the successor of itself");
			}
		}
		
		public int getSuccessor() {  // Syntactic sugar
			return fingerTable[0];
		}
		
		public Key getSuccessorKey() {
			return successorKey;
		}
		
		public void setSuccessor(int successorAddr) {  // Syntactic sugar
			setFinger(0, successorAddr);
		}
		
		public Optional<Integer> getPredecessor() {
			return predecessor;
		}
		
		public Key getPredecessorKey() {
			if (predecessor.isEmpty()) {
				throw new RuntimeException("The predecessor is unknown");
			}
			return predecessorKey;
		}
		
		public void setPredecessor(Optional<Integer> predecessorAddr) {
			predecessor = predecessorAddr;
			
			
			predecessor.ifPresent((predAddr) -> {
				predecessorKey = new Key(predAddr);
				
				if (predAddr == address) {
					throw new RuntimeException("A node can't be the predecessor of itself");
				}
			});
		}
		
		public int getClosestPrecedingFinger(Key key) {			
			for (int i = M-1; i >= 0; i--) {
				if (fingerTable[i] == -1) {
					continue;
				}
				Key fingerNodeKey = new Key(fingerTable[i]);				
				KeyRange range = new KeyRange(nodeKey, key);
				if (range.containsOpen(fingerNodeKey)) {
					return fingerTable[i];
				}
			}
			return fingerTable[0];
		}
		
		public Key getStart(int index) {
			return new Key(nodeKey.toBigInteger()
								  .add(BigInteger.valueOf(2).pow(index))
								  .mod(BigInteger.valueOf(2).pow(M)));
		}
		
		public void fixFingers() {
			if (!initCompleted || tickCounter % fixFingerPeriod == 0) {
				
				Key fingerStart = getStart(fixFingersIndex);
				
				if (!initCompleted) {
					KeyRange range = new KeyRange(nodeKey, getSuccessorKey());
					while (range.containsOpen(fingerStart)) {
						setFinger(fixFingersIndex, getSuccessor());
						
						fixFingersIndex++;						
						if (fixFingersIndex == 160) {
							fixFingersIndex = 1;
							initCompleted = true;
							return;
						}
						
						fingerStart = getStart(fixFingersIndex);						
					}
				}
				
				Logger.getLogger().logFixFinger(ChordNode.this, fingerStart, requestCounter);
				sendSuccessorRequest(fingerStart);
				
				fingerIndexes.put(requestCounter, fixFingersIndex);
				callbacks.put(requestCounter, 
						(msg) -> {
							SuccessorResponse response = (SuccessorResponse) msg;
							setFinger(fingerIndexes.get(response.getRequestId()),
									  response.getSuccessorAddr());							
							fingerIndexes.remove(response.getRequestId());
						});
				
				timeouts.put(requestCounter, 50);
				timeoutCallbacks.put(requestCounter, 
						() -> {
							//System.out.println(this + "Fix finger timeout (ignored)");
						});
				
				requestCounter++;
				fixFingersIndex++;
				if (fixFingersIndex == 160) {
					fixFingersIndex = 1;
					initCompleted = true;
				}
				
				// Check if we are the observed node (the first created)
				if (address == RandomHelper.getSeed()) {
					Context<Object> context = ContextUtils.getContext(ChordNode.this);
					for (FingerNodeVisual node : visualFingers) {
						context.remove(node);
					}
					visualFingers.clear();
					HashSet<Integer> distinctFingers = new HashSet<>();
					for (int i = 0; i < 160; i++) {
						int finger = getFinger(i);
						if (finger != -1 && !distinctFingers.contains(finger)) {
							distinctFingers.add(finger);
							FingerNodeVisual visualNode = new FingerNodeVisual(finger, new Key(finger));
							visualFingers.add(visualNode);
							context.add(visualNode);
							Visualizer.getVisualizer().addFinger(visualNode, new Key(finger));
						}
					}
					
				}

				
			}
			
		}
		
		public void replaceSuccessor() {
			// Our successor has crashed, search for a new successor
			try {
				setSuccessor(IntStream.range(1, 160)
						 			  .map(i -> ft.getFinger(i))
						 			  .filter(finger -> 
						 			      finger != ft.getSuccessor() &&
								 		  finger != -1 &&
								 		  finger != address)
						 			  .findFirst()
						 			  .orElseThrow());
				stabilize();
				
			} catch (NoSuchElementException e) {
				// We did not know any other nodes besides the failed successor
				System.out.println(ChordNode.this + " Rejoin (lost successor)");
				Logger.getLogger().logJoinReTry(ChordNode.this, -1, true);
				joined = false;
				join();
			}
		}
		
		public void bootstrapInit() {
			
			int firstNodeAddress = RandomHelper.getSeed();

			setSuccessor(2*firstNodeAddress + 1 - address);
			setPredecessor(Optional.of(ft.getSuccessor()));
			
			// Initialize the the finger table
			KeyRange range = new KeyRange(nodeKey, getSuccessorKey());			
			for (int i = 1; i < 160; i++) {
				if (range.containsOpenClosed(getStart(i))) {
					fingerTable[i] = ft.getSuccessor();
				} else {
					fingerTable[i] = address;
				}
			}
			
			initCompleted = true;
		}
		
		public void log() {
			Logger.getLogger().logFingerTable(ChordNode.this, this);
		}
		
	}
	
	
	//////////////////////////////////////////////////////////////////////////
	// Hash table API
	
	// Find the node that is responsible for the specified key
	public void lookup(final Key key) {
		if (joined) {
			Logger.getLogger().logStartLookup(this, key, requestCounter);
			
			// Before sending a SuccessorRequest, check if our successor is the successor
			// of the key requested
			KeyRange range = new KeyRange(nodeKey, ft.getSuccessorKey());
			if (range.containsOpenClosed(key)) {
				Logger.getLogger().logLookup(this, key, address, ft.getSuccessor(), 0, requestCounter);
				requestCounter++;
				return;
			}
			
			// Check if we are the observed node (the first created),
			// if add our lookup request to the visualization
			if (address == RandomHelper.getSeed()) {
				Visualizer.getVisualizer().addRequest(requestCounter);
			}
			
			sendSuccessorRequest(key);
			callbacks.put(requestCounter, 
					(msg) -> {
						SuccessorResponse response = (SuccessorResponse) msg;
						// Log the result of the lookup
						Logger.getLogger().logLookup(this, key, response.getSenderAddr(),
								response.getSuccessorAddr(), response.getHops(), response.getRequestId());
					});
			
			timeouts.put(requestCounter, 50);
			timeoutCallbacks.put(requestCounter, 
					() -> {
						Logger.getLogger().logLookupFailed(this, key);
					});
			
			requestCounter++;
		}
	}

}
