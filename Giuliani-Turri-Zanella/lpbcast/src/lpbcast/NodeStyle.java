package lpbcast;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.gis.styleEditor.SimpleMarkFactory;
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import saf.v3d.ShapeFactory2D;
import saf.v3d.scene.VSpatial;

public class NodeStyle extends DefaultStyleOGL2D{

	public static Color deliveredNodeColor = Color.green;
	public static Color notDeliveredNodeColor = Color.black;
	public static Color submittedNodeColor = Color.red;
	public static Color unsubmittedNodeColor = Color.lightGray;
	
	@Override
	public Color getColor(Object agent){
		Color color = notDeliveredNodeColor;
        if(agent instanceof Process){
        	Process p = (Process)agent;
        	if(p.isNewNode) {
        		color = submittedNodeColor;
        	} else if(p.isUnsubscribed) {
        		color = unsubmittedNodeColor;
        	} else if(p.deliveredCurrentVisualEvent) {
        		color = deliveredNodeColor;	
        	}
        }
        return color;

	}
	
	@Override
    public float getScale(Object object) {
        return 3;
    }
	
	@Override
	public Color getBorderColor(Object agent) {
		return Color.black;
	}
	
	@Override
	public int getBorderSize(Object agent) {
		return 0;
	}

	
	public static void changeColor() {
		Random rand = new Random();
		NodeStyle.deliveredNodeColor = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));		
	}
	

}
