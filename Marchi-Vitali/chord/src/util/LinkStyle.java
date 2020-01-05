package util;

import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.EdgeStyleOGL2D;

public class LinkStyle implements EdgeStyleOGL2D {

	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		return 2;
	}

	@Override
	public Color getColor(RepastEdge<?> edge) {
		Color color = Color.gray;
		switch ((int) edge.getWeight()) {
		case 0: color = new Color(74, 149, 207); break;
		case 1: color = new Color(35, 171, 26); break;
		case 2: color = Color.red; break;
		}
		return color;
	}

}
