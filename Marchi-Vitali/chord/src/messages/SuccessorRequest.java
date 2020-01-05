package messages;

import communication.Message;
import util.Key;

public class SuccessorRequest extends Message {
	
	private Key key;
	private int sourceAddr;
	private int requestId;
	private int hops;
	
	public SuccessorRequest(int senderAddr,
							int destAddr,
							Key key,
							int sourceAddr,
							int requestId,
							int hops) 
	{
		super(senderAddr, destAddr);
		this.key = key;
		this.sourceAddr = sourceAddr;
		this.requestId = requestId;
		this.hops = hops;
	}
	
	public Key getKey() {
		return key;
	}

	public int getSourceAddr() {
		return sourceAddr;
	}

	public int getRequestId() {
		return requestId;
	}
	
	public int getHops() {
		return hops;
	}

	@Override
	public String toString() {
		return "SuccessorRequest [sender=" + senderAddr + ", dest=" + destAddr + ", key=" + key + ", source="
				+ sourceAddr + ", requestId=" + requestId + ", hops=" + hops + "]";
	}

}
