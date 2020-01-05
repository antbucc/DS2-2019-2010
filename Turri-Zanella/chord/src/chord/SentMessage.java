package chord;

import chord.messages.Message;

/**
 * Represents the value of the timeouts table.
 * 
 * @author zanel
 * @author coffee
 *
 */
public class SentMessage {
	
	/**
	 * The sent message.
	 */
	private Message message;
	
	/**
	 * The tick during which the message is sent.
	 */
	private double tick;
	
	/**
	 * Instantiates a new SentMessage.
	 * 
	 * @param message the sent message.
	 * @param tick the tick during which the message is sent.
	 */
	public SentMessage(Message message, double tick) {
		this.message = message;
		this.tick = tick;
	}
	
	/**
	 * @return the message
	 */
	public Message getMessage() {
		return message;
	}

	/**
	 * @return the tick
	 */
	public double getTick() {
		return tick;
	}
	
}
