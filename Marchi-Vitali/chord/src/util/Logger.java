package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import chord.ChordNode;
import chord.ChordNode.FingerTable;
import communication.Message;
import messages.SuccessorResponse;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;

public class Logger {
	
	private static Logger instance = null;
	
	private FileWriter fileWriter1;
    private PrintWriter printWriter1;
	private FileWriter fileWriter2;
    private PrintWriter printWriter2;
    private FileWriter fileWriter3;
    private PrintWriter printWriter3;
    private FileWriter fileWriter4;
    private PrintWriter printWriter4;
    
    // All the nodes created
    private HashMap<Key, ChordNode> nodes = new HashMap<>();
    // Only the nodes that completed the joining
    private TreeSet<Key> orderedNodeKeys = new TreeSet<>();
    
    private int observedNodeAddress;

	private Logger() {
		try {
			fileWriter1 = new FileWriter("chord1.log");
			printWriter1 = new PrintWriter(fileWriter1);
			fileWriter2 = new FileWriter("chord2.log");
			printWriter2 = new PrintWriter(fileWriter2);
			fileWriter3 = new FileWriter("chord3.log");
			printWriter3 = new PrintWriter(fileWriter3);
			fileWriter4 = new FileWriter("chord4.log");
			printWriter4 = new PrintWriter(fileWriter4);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		observedNodeAddress = RandomHelper.getSeed();
	}
	
	public static Logger getLogger() {
		if (instance == null) {
			instance = new Logger();
		}
		return instance;
	}
	
	@ScheduledMethod(start = 1, interval = 100, priority = -10)
	public void stepLogNodes() {
		logNodes();
	}
	
	public void logNewNode(ChordNode node) {
		nodes.put(node.getNodeKey(), node);
		
		printWriter1.println(Util.getTick() + ";NewNode;" 
							 + node.getAddress());
	}
	
	public void logNodeCrash(ChordNode node) {
		nodes.remove(node.getNodeKey(), node);
		orderedNodeKeys.remove(node.getNodeKey());
		
		printWriter1.println(Util.getTick() + ";NodeCrash;"
							 + node.getAddress());
	}
	
	public void logNodeJoin(ChordNode node) {
		orderedNodeKeys.add(node.getNodeKey());
		
		printWriter1.println(Util.getTick() + ";NodeJoin;"
							 + node.getAddress() + ";"
							 + node.getSuccessorAddr());
	}
	
	public void logJoinReTry(ChordNode node, int randomNode, boolean lostSucc) {
		printWriter1.println(Util.getTick() + ";NodeJoinReTry;" 
							 + node.getAddress() + ";" 
							 + randomNode + ";"
							 + lostSucc);
	}
	
	public void logStartLookup(ChordNode source, Key targetKey, int request) {
		printWriter1.println(Util.getTick() + ";LookupStart;" 
							 + source.getAddress() + ";" 
							 + targetKey + ";" 
							 + request);
	}
	
	public void logLookup(ChordNode source, Key targetKey, int senderId, int nodeAddr, int op, int requestId) {
		Key realSuccessor = orderedNodeKeys.first();
		for (Key key : orderedNodeKeys) {
			if (key.compareTo(targetKey) >= 0) {
				realSuccessor = key;
				break;
			}
		}
		
		if (new Key(nodeAddr).equals(realSuccessor)) {
			printWriter1.println(Util.getTick() + ";LookupOK;" 
								 + source.getAddress() + ";" 
								 + nodeAddr + ";" 
								 + targetKey + ";"
								 + op);
			
			if (source.getAddress() == observedNodeAddress &&
				nodes.containsKey(source.getNodeKey())) 
			{
					Visualizer.getVisualizer().addEdge(senderId,
													   nodes.get(source.getNodeKey()),
													   1, requestId);
			}
			
		} else {
			printWriter1.println(Util.getTick() + ";LookupERROR;" 
								 + source.getAddress() + ";"
								 + nodeAddr + ";" 
								 + targetKey + ";"
								 + nodes.get(realSuccessor).getAddress() + ";"
								 + op);
			
			if (source.getAddress() == observedNodeAddress &&
				nodes.containsKey(source.getNodeKey())) 
			{
				Visualizer.getVisualizer().addEdge(senderId,
												   nodes.get(source.getNodeKey()),
												   2, requestId);
			}
				
		}
	}
	
	public void logLookupFailed(ChordNode source, Key targetKey) {
		printWriter1.println(Util.getTick() + ";LookupFAILED;" 
							 + source.getAddress() + ";"
							 + targetKey);
	}
	
	// DEBUG
	public void logFixFinger(ChordNode node, Key start, int requestId) {
//		printWriter1.println(Util.getTick() + ";FixFinger;" 
//				 + node.getAddress() + ";"
//				 + start);
	}
	
	// DEBUG
	public void logMessage(Message msg) {
//		printWriter4.println(Util.getTick() + ";MSG;" + msg);
	}
	
	// DEBUG
	private void logNodes() {		
//		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
//		
//		printWriter2.println(tick + " Joined nodes" 
//							 + orderedNodeKeys.size() + ";" 
//							 + nodes.size());
//		
//		for (Key key : orderedNodeKeys) {
//			ChordNode node = nodes.get(key);
//			printWriter2.println(node.getAddress() + "," 
//								 + node.getSuccessorAddr() + ","
//								 + node.getPredecessorAddr() + ",\t"
//								 + key);
//		}
//		
//		printWriter2.println("");
//		printWriter2.println("NonJoined nodes");
//		
//		for (Key key : nodes.keySet()) {
//			if (!orderedNodeKeys.contains(key)) {
//				ChordNode node = nodes.get(key);
//				printWriter2.println(node.getAddress() + "," 
//						 + node.getSuccessorAddr() + ","
//						 + node.getPredecessorAddr() + ",\t"
//						 + key);
//			}
//		}
//		printWriter2.println("");
	}
	
	// DEBUG
	public void logFingerTable(ChordNode node, FingerTable ft) {
//		printWriter3.println(Util.getTick() + " FingerTable " + node);
//		for (int i = 0; i < 160; i++) {
//			printWriter3.println(i + ": " + ft.getFinger(i) + " " + ft.getStart(i));
//		}
//		printWriter3.println("");
	}

}
