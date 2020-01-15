package chord;

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
	 * that turn:
	 * 	- black: the node is idle - red: the node asks to its successor for the key
	 * 	- green: the node is the one responsible for the key
	 * 	- blue: the node sends back the result
	 */
	@Override
	public Color getColor(Object o) {
		if (o instanceof Node) {
			Node node = (Node) o;
			if (node.action == Node.ActionPerformed.SEARCH_SUCCESSOR) {
				return Color.RED;
			} else if (node.action == Node.ActionPerformed.FOUND_RESPONSE) {
				return Color.green;
			} else if (node.action == Node.ActionPerformed.RETURN_RESPONSE) {
				return Color.blue;
			} else {
				return Color.black;
			}
		}
		return Color.BLACK; // default color

	}

	/**
	 * Sets the scale of the node.
	 */
	@Override
	public float getScale(Object o) {
		return 3;
	}

	/**
	 * Sets the label of the node.
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
	 * Sets the font of the node's label.
	 * 
	 * @param o the object.
	 */
	@Override
	public Font getLabelFont(Object o) {
		return new Font("TimesRoman", Font.PLAIN, 25);
	}
}
