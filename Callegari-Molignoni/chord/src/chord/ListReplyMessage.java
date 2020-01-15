package chord;

import java.util.List;

public class ListReplyMessage extends Message {
	/**
	 * The successor list of the current node.
	 */
	List<Integer> successorList;

	public ListReplyMessage(int sender, List<Integer> successorList) {
		super(sender);

		this.successorList = successorList;
	}
}
