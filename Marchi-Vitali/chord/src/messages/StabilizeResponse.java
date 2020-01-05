package messages;

import java.util.Optional;

import communication.Message;

public class StabilizeResponse extends Message {
	
	private Optional<Integer> predecessorAddr;
	private int requestId;

	public StabilizeResponse(int senderAddr, 
							 int destAddr, 
							 Optional<Integer> predecessorAddr, 
							 int requestId) 
	{
		super(senderAddr, destAddr);
		this.predecessorAddr = predecessorAddr;
		this.requestId = requestId;
	}

	public Optional<Integer> getPredecessorAddr() {
		return predecessorAddr;
	}

	public int getRequestId() {
		return requestId;
	}

	@Override
	public String toString() {
		return "StabilizeResponse [sender=" + senderAddr + ", dest=" + destAddr + ", pred=" + predecessorAddr
				+ ", requestId=" + requestId + "]";
	}
	
}
