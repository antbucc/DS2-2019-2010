package chord;

/**
 * Message used to send back the result of a lookup to another node.
 */
public class LookupResultMessage extends Message {
	/**
	 * ID of the search, used in order to handle multiple searches at the same time.
	 */
	String searchId;

	/**
	 * Node responsible for the key.
	 */
	int resultNode;

	/**
	 * Key for which the lookup was requested.
	 */
	int key;

	/**
	 * Constructor of the message.
	 * 
	 * @param sender     the sender of the message.
	 * @param searchId   the ID of the search.
	 * @param resultNode the node responsible for the key.
	 * @param key        the requested key for the lookup.
	 */
	public LookupResultMessage(int sender, String searchId, int resultNode, int key) {
		super(sender);
		
		this.searchId = searchId;
		this.resultNode = resultNode;
		this.key = key;
	}

}
