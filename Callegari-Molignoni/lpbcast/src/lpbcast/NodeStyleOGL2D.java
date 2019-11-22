package lpbcast;

import java.awt.Color;
import java.awt.Font;

import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;

/**
 * Class used to customize the look of a node.
 */
public class NodeStyleOGL2D extends DefaultStyleOGL2D {
	/**
	 * Color assigned to the node for visualization.
	 * 
	 * The node will have a specific color depending on the action that performed in
	 * that turn: -BLUE: it generated a message; -BLACK: it sent gossip messages;
	 * -RED: it has unsubscribed.
	 */
	@Override
	public Color getColor(Object o) {
		if (o instanceof Node) {
			Node node = (Node) o;
			if (!node.isSubbed) { // RED if node is unsubbed
				return Color.RED;
			}

			if (node.sendingMessage) {
				return Color.BLUE; // BLUE if node generated a message
			} else if (node.deliveringMessage) {
				return Color.GREEN; // GREEN if node delivered at least one message
			} else {
				return Color.BLACK; // BLACK if node sends gossip (always).
			}
		}

		return Color.BLACK; // default color

	}

	/**
	 * Gets the scale of the object.
	 */
	@Override
	public float getScale(Object o) {
		return 3;
	}

	/**
	 * Gets the label of the object.
	 * 
	 * @param o the object.
	 */
	@Override
	public String getLabel(Object o) {
		if (o instanceof Node) {
			Node node = (Node) o;
			return "" + node.id;

		} else {
			return "";
		}
	}

	/**
	 * Gets the font of the object's label.
	 * 
	 * @param o the object.
	 */
	@Override
	public Font getLabelFont(Object o) {
		return new Font("TimesRoman", Font.PLAIN, 30);
	}
}
