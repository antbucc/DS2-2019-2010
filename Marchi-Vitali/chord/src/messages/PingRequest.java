package messages;

import communication.Message;

public class PingRequest extends Message {

	private int requestId;
	
	public PingRequest(int senderAddr, int destAddr, int requestId) {
		super(senderAddr, destAddr);
		this.requestId = requestId;
	}
	
	public int getRequestId() {
		return requestId;
	}

	@Override
	public String toString() {
		return "PingRequest [sender=" + senderAddr + ", dest=" + destAddr + ", requestId=" + requestId + "]";
	}

}
