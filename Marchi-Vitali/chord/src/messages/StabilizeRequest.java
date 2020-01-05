package messages;

import communication.Message;

public class StabilizeRequest extends Message {
	
	private int requestId;
	
	public StabilizeRequest(int senderAddr, 
							int destAddr, 
							int requestId) 
	{
		super(senderAddr, destAddr);
		this.requestId = requestId;
	}

	public int getRequestId() {
		return requestId;
	}

	@Override
	public String toString() {
		return "StabilizeRequest [sender=" + senderAddr + ", dest=" + destAddr + ", requestId=" + requestId + "]";
	}
	
}
