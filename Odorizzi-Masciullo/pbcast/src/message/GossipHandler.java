package message;

import java.util.ArrayList;

import pbcast.Process;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class GossipHandler extends Handler {
	protected int round_id;
	protected ArrayList<String> digest;
	
	public GossipHandler(Process from, Process to, int round_id, ArrayList<String> digest) {
		super(null, from, to);
		this.round_id = round_id;
		this.digest = digest;
	}
	
	public ArrayList<String> compareDigest(ArrayList<String> digest_of_member) {
		ArrayList<String> missed_messages = new ArrayList<String>();
		for(String summary : this.digest) {
			if (!digest_of_member.contains(summary)) 
				missed_messages.add(summary);
		}
		return missed_messages;
	}
	
	public int getRoundId() {
		return this.round_id;
	}

}
