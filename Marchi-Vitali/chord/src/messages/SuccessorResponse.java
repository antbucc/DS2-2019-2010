package messages;

import communication.Message;

public class SuccessorResponse extends Message {
	
	private int successorAddr;
	private int requestId;
	private int hops;

	public SuccessorResponse(int senderAddr, 
							 int destAddr, 
							 int successorAddr, 
							 int requestId,
							 int hops) 
	{
		super(senderAddr, destAddr);
		this.successorAddr = successorAddr;
		this.requestId = requestId;
		this.hops = hops;
	}
	
	public int getSuccessorAddr() {
		return successorAddr;
	}

	public int getRequestId() {
		return requestId;
	}
	
	public int getHops() {
		return hops;
	}

	@Override
	public String toString() {
		return "SuccessorResponse [sender=" + senderAddr + ", dest=" + destAddr + ", succ=" + successorAddr
				+ ", requestId=" + requestId + ", hops=" + hops + "]";
	}
		
}
