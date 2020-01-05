package messages;

import communication.Message;

public class PingResponse extends Message {

	private int requestId;
	
	public PingResponse(int senderAddr, int destAddr, int requestId) {
		super(senderAddr, destAddr);
		this.requestId = requestId;
	}
	
	public int getRequestId() {
		return requestId;
	}

	@Override
	public String toString() {
		return "PingResponse [sender=" + senderAddr + ", dest=" + destAddr + ", requestId=" + requestId + "]";
	}

}
