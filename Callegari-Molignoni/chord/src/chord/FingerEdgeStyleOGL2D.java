package chord;

import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.EdgeStyleOGL2D;

/**
 * Class used to dynamically change the color of edges.
 */
public class FingerEdgeStyleOGL2D implements EdgeStyleOGL2D {
	/**
	 * Makes the edge is thicker if there is communication between the two nodes.
	 */
	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		Node source = (Node) edge.getSource();
		Node target = (Node) edge.getTarget();

		if (source.askedTo.indexOf(Integer.valueOf(target.id)) != -1) {
			return 4;
		}

		return 0;
	}

	/**
	 * Sets the edge to red if there is communication between the two nodes.
	 */
	@Override
	public Color getColor(RepastEdge<?> edge) {
		Node source = (Node) edge.getSource();
		Node target = (Node) edge.getTarget();

		if (source.askedTo.indexOf(Integer.valueOf(target.id)) != -1) {
			return Color.RED;
		}

		return Color.gray;
	}
}
