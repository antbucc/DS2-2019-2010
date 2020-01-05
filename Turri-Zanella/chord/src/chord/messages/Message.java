package chord.messages;

import java.util.UUID;

/**
 * Represents an application message.
 * 
 * @author zanel
 * @author coffee
 * 
 */
public abstract class Message {

	/**
	 * The unique identifier of the message.
	 */
	private UUID msgId;
	
	/**
	 * The Chord identifier of the sender node.
	 */
	private Long sender;
	
	/**
	 * The tick during which the message has to be processed.
	 */
	private double tick;
	
	/**
	 * Instantiates a new Message.
	 * 
	 * @param sender the Chord identifier of the sender node.
	 * @param tick the tick during which the message has to be processed.
	 */
	public Message(Long sender) {
		this.msgId = UUID.randomUUID();
		this.sender = sender;
		this.tick = -1;
	}
	
	/**
	 * @param tick the tick to set
	 */
	public void setTick(double tick) {
		this.tick = tick;
	}

	/**
	 * @return the msgId
	 */
	public UUID getMsgId() {
		return msgId;
	}

	/**
	 * @return the sender
	 */
	public Long getSender() {
		return sender;
	}

	/**
	 * @return the tick
	 */
	public double getTick() {
		return tick;
	}
}
