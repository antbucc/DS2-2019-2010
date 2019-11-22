package lpbcast.messages;

import communication.Message;

public class Unsubscription extends Message {
	
	public Unsubscription(int senderId, int destId) {
		super(senderId, destId);
	}
}
