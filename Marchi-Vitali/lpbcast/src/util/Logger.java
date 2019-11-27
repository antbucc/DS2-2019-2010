package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import communication.Message;
import communication.Node;
import lpbcast.Event;
import lpbcast.LPBCastNode;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.context.Context;

public class Logger {
	
	private static Logger instance = null;
	
	private FileWriter fileWriter;
    private PrintWriter printWriter;
    private Context<Object> context;

	private Logger() {
		try {
			fileWriter = new FileWriter("LPBCast.log");
			printWriter = new PrintWriter(fileWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Logger getLogger() {
		if (instance == null) {
			instance = new Logger();
		}
		return instance;
	}
	
	public void setContext(Context<Object> context) {
		this.context = context;
	}
	
	public void logEventCreation(Event event) {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		ArrayList<Integer> activeNodesIds = new ArrayList<>();
		for (Object node : context.getObjects(LPBCastNode.class)) {
			activeNodesIds.add(((LPBCastNode) node).getId());
		}
		printWriter.println(tick + ";NewEvent;" 
							+ event.getId().getId() + ";" 
							+ event.getId().getSourceId() + ";"
							+ activeNodesIds + ";"
							+ activeNodesIds.size());
	}
	
	public void logEventDelivery(Event event, int receiverId, int senderId) {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		printWriter.println(tick + ";EventDelivery;" 
							+ event.getId().getId() + ";" 
							+ receiverId + ";"
							+ event.getId().getSourceId() + ";"
							+ senderId);
	}
	
	public void logNodeCreation(Node node) {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		printWriter.println(tick + ";NewNode;" 
							+ node.getId());
	}
	
	public void logNodeDelete(Node node) {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		printWriter.println(tick + ";NodeRemoved;" 
							+ node.getId());
	}
	
	public void logPersistency(int eventId, int persistency) {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		printWriter.println(tick + ";Persistency;" + eventId + ";" + persistency);
	}
	
	// TODO Disabled
	public void logMessageSent(Message message) {
//		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
//		printWriter.println(tick + " MessageSent " 
//							+ message.getSenderId() + " " 
//							+ message.getDestId());
	}

}
