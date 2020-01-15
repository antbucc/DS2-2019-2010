package chord;

/**
 * Class that is used to define a generic message.
 * 
 * Each message sent inside the system will extend this class. A message has to
 * define a sender.
 */
public abstract class Message {
	/**
	 * Sender of the message.
	 */
	int sender;

	/**
	 * Constructor of the message.
	 * 
	 * @param sender the sender of the message.
	 */
	public Message(int sender) {
		this.sender = sender;
	}
}
