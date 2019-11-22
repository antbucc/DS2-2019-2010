package message;

import java.util.ArrayList;

import pbcast.Process;

public class SolicitationHandler extends Handler {
	protected int round_number;
	protected ArrayList<String> requested_messages;

	public SolicitationHandler(Message msg, Process from, Process to, int round_number, ArrayList<String> requested_messages) {
		super(msg, from, to);
		this.round_number = round_number;
		this.requested_messages = requested_messages;
	}
	
	public ArrayList<String> getRequestedMessages() {
		return this.requested_messages;
	}
	
	public int getRoundNumber() {
		return this.round_number;
	}

}
