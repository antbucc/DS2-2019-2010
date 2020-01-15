package chord;

/**
 * Message used to request the lookup to another node.
 */
public class SearchSuccessorMessage extends Message {
	/**
	 * ID of the search.
	 */
	String searchId;

	/**
	 * Key requested for the lookup.
	 */
	int key;

	/**
	 * Constructor of the message.
	 * 
	 * @param sender   the sender of the message.
	 * @param searchId the ID of the search.
	 * @param key      the key for which the lookup is requested.
	 */
	public SearchSuccessorMessage(int sender, String searchId, int key) {
		super(sender);

		this.searchId = searchId;
		this.key = key;
	}
}
