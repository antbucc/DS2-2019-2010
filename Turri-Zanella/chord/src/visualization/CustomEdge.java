package visualization;

import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.EdgeStyleOGL2D;

public class CustomEdge implements EdgeStyleOGL2D{

	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		if(edge.getWeight() == Visualizer.EdgeType.SUCCESSOR.ordinal()) {
			return 2;
		} else if(edge.getWeight() == Visualizer.EdgeType.REQUEST.ordinal()) {
			return 2;
		} else if (edge.getWeight() == Visualizer.EdgeType.REPLY_SUCCESSOR.ordinal())  {
			return 2;
		} else if(edge.getWeight() == Visualizer.EdgeType.REPLY.ordinal()) {
			return 2;
		} else if(edge.getWeight() == Visualizer.EdgeType.ACK.ordinal())  {
			return 1;
		}
		
		return 0;
	}

	@Override
	public Color getColor(RepastEdge<?> edge) {
		if(edge.getWeight() == Visualizer.EdgeType.SUCCESSOR.ordinal()) {
			return Color.LIGHT_GRAY;
		} else if (edge.getWeight() == Visualizer.EdgeType.REPLY_SUCCESSOR.ordinal())  {
			return Color.CYAN;
		} if(edge.getWeight() == Visualizer.EdgeType.REQUEST.ordinal()) {
			return Color.DARK_GRAY;
		} else if(edge.getWeight() == Visualizer.EdgeType.REPLY.ordinal()) {
			return Color.MAGENTA;
		} else if(edge.getWeight() == Visualizer.EdgeType.ACK.ordinal())  {
			return Color.RED;
		}
		
		return null;
	}

}
