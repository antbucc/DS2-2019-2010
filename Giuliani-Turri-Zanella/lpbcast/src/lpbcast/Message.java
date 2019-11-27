/**
 * 
 */
package lpbcast;

/**
 * Represents a message.
 * 
 * @author zanel
 * @author danie
 *
 */
public abstract class Message {
	
	/**
	 * Types of messages that can be sent by a process.
	 */
	public enum MessageType {GOSSIP, RETRIEVE_REQUEST, RETRIEVE_REPLY}
	
	/**
	 * The tick during which the message must be processed.
	 */
	public double tick;
	/**
	 * The type of the message.
	 */
	public MessageType type;
	/**
	 * The sender of the message.
	 */
	public int sender;
	
	/**
	 * Instantiates a new message.
	 * 
	 * @param tick the tick during which the message is created
	 * @param type the type of the message
	 * @param sender the sender of the message
	 */
	public Message(double tick, MessageType type, int sender) {
		this.tick = tick;
		this.type = type;
		this.sender = sender;
	}
	
	/**
	 * Instantiates a new message.
	 * 
	 * @param type the type of the message
	 * @param sender the sender of the message
	 */
	public Message(MessageType type, int sender) {
		this.tick = -1; // -1 means that the tick will be set during the sending of the message
		this.type = type;
		this.sender = sender;
	}
}
