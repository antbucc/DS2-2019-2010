package messages;

import communication.Message;

public class Ack extends Message {

	public Ack(int senderAddr, int destAddr) {
		super(senderAddr, destAddr);
	}

	@Override
	public String toString() {
		return "Ack [sender=" + senderAddr + ", dest=" + destAddr + "]";
	}

}
