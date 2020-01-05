package visualization;

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;

import chord.Node;
import repast.simphony.util.ContextUtils;
import repast.simphony.visualizationOGL2D.StyleOGL2D;
import saf.v3d.ShapeFactory2D;
import saf.v3d.scene.Position;
import saf.v3d.scene.VSpatial;

public class CustomNode implements StyleOGL2D<Node>{

	protected ShapeFactory2D shapeFactory;
	
	@Override
	public void init(ShapeFactory2D factory) {
		this.shapeFactory = factory;
	}

	@Override
	public VSpatial getVSpatial(Node object, VSpatial spatial) {
		if (spatial == null) {
	      spatial = shapeFactory.createCircle(40, 160);
	    }
	    return spatial;
	}

	@Override
	public Color getColor(Node object) {
		Iterator<Visualizer> it =ContextUtils.getContext(object).getObjects(Visualizer.class).iterator();
		if(!it.hasNext()) 
			assert(false);
		Visualizer visualizer = it.next();
		if(object.isNewNode()) {
			return Color.YELLOW;
		}
		if(visualizer.getTargetId() != null) {
			if(visualizer.getInitiator()!= null && visualizer.getInitiator() == object.getId()){
				return Color.cyan;
			}

			if(visualizer.getSuccessors().contains(object.getId())){
				return Color.red;
			}
			if(visualizer.getFingers().contains(object.getId())){
				return Color.GREEN;
			}
			if(visualizer.getIntermediators().contains(object.getId())){
				return Color.ORANGE;
			}

		}
		return Color.GRAY;
	}

	@Override
	public int getBorderSize(Node object) {
		return 4;
	}

	@Override
	public Color getBorderColor(Node object) {
		Iterator<Visualizer> it =ContextUtils.getContext(object).getObjects(Visualizer.class).iterator();
		if(!it.hasNext()) 
			assert(false);
		Visualizer visualizer = it.next();
		if(object.isNewNode()) {
			return Color.BLACK;
		}
		if(visualizer.getTargetId() != null) {
			if(visualizer.getInitiator() != null && visualizer.getInitiator() == object.getId()){
				return Color.cyan;
			}
			if(visualizer.getFingers().contains(object.getId())){
				return Color.GREEN;
			}

			if(visualizer.getSuccessors().contains(object.getId())){
				return Color.red;
			}
			if(visualizer.getIntermediators().contains(object.getId())){
				return Color.ORANGE;
			}
		}
		return Color.GRAY;
	}

	@Override
	public float getRotation(Node object) {
		return 0f;
	}

	@Override
	public float getScale(Node object) {
		Iterator<Visualizer> it =ContextUtils.getContext(object).getObjects(Visualizer.class).iterator();
		if(!it.hasNext()) 
			assert(false);
		Visualizer visualizer = it.next();
		if(visualizer.getTargetId() != null) {
			if(visualizer.getInitiator() != null && visualizer.getInitiator() == object.getId()){
				return 1.5f;
			}
			if(visualizer.getFingers().contains(object.getId())){
				return 1.5f;
			}

			if(visualizer.getSuccessors().contains(object.getId())){
				return 1.5f;
			}
			if(visualizer.getIntermediators().contains(object.getId())){
				return 1.5f;
			}
		}
		return 0.5f;
	}

	@Override
	public String getLabel(Node object) {
		return null;
	}

	@Override
	public Font getLabelFont(Node object) {
		return null;
	}

	@Override
	public float getLabelXOffset(Node object) {
		return 0;
	}

	@Override
	public float getLabelYOffset(Node object) {
		return 0;
	}

	@Override
	public Position getLabelPosition(Node object) {
		return Position.NORTH;
	}

	@Override
	public Color getLabelColor(Node object) {
		return Color.black;
	}

}
