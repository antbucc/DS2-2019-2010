package lpbcast;

import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.EdgeStyleOGL2D;

public class Custom2DEdge implements EdgeStyleOGL2D{
	public static Color gossipEventEdgeColor = Color.green;
	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		if(edge.getWeight() == Visualization.EdgeType.RETRIEVE_REPLY.ordinal() || edge.getWeight() == Visualization.EdgeType.RETRIEVE_REQUEST.ordinal() || edge.getWeight() == Visualization.EdgeType.VIEW.ordinal()) {
			return 1;
		} else {
			return 3;
		}
	}

	@Override
	public Color getColor(RepastEdge<?> edge) {
		if(edge.getWeight() == Visualization.EdgeType.FANOUT.ordinal()) {
			return gossipEventEdgeColor;
		} else if(edge.getWeight() == Visualization.EdgeType.VIEW.ordinal()){
			return new Color(214, 214, 214);
		} else if(edge.getWeight() == Visualization.EdgeType.RETRIEVE_REQUEST.ordinal()){
			return Color.red;
		} else {
			return Color.cyan;
		}
	}

}
