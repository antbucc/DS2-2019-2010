package visualization;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

import repast.simphony.visualizationOGL2D.StyleOGL2D;
import saf.v3d.ShapeFactory2D;
import saf.v3d.scene.Position;
import saf.v3d.scene.VSpatial;
import utils.ChordUtilities;
import visualization.Marker.MarkerType;

public class CustomMarker implements StyleOGL2D<Marker> {
	protected ShapeFactory2D shapeFactory;
	
	@Override
	public void init(ShapeFactory2D factory) {
		this.shapeFactory = factory;
		
	}

	@Override
	public VSpatial getVSpatial(Marker object, VSpatial spatial) {
		if (spatial == null) {
			try {
				spatial = shapeFactory.createImage("icons/target.png", 1);
			} catch (IOException e) {
				System.out.println("Image not found");
				spatial = shapeFactory.createCircle(1, 1);
			}
	    }
	    return spatial;
	}

	@Override
	public Color getColor(Marker object) {
		return Color.BLACK;
	}

	@Override
	public int getBorderSize(Marker object) {
		return 0;
	}

	@Override
	public Color getBorderColor(Marker object) {
		return Color.BLACK;
	}

	@Override
	public float getRotation(Marker object) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getScale(Marker object) {
		return 5;
	}

	@Override
	public String getLabel(Marker object) {
			return Long.toUnsignedString(object.getId());
	}

	@Override
	public Font getLabelFont(Marker object) {
		return new Font("Verdana", Font.PLAIN, 100);
	}

	@Override
	public float getLabelXOffset(Marker object) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getLabelYOffset(Marker object) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Position getLabelPosition(Marker object) {
		return Position.NORTH;
	}

	@Override
	public Color getLabelColor(Marker object) {
		return Color.black;
	}

}
