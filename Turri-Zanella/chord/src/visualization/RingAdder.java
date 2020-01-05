package visualization;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;

import chord.Node;
import repast.simphony.space.continuous.ContinuousAdder;
import repast.simphony.space.continuous.ContinuousSpace;
import utils.ChordUtilities;

public class RingAdder implements ContinuousAdder<Object>{
	
	/**
	 * The parameters provided by the user during the simulation.
	 */
	private HashMap<String, Object> parameters = ChordUtilities.getParameters(); 
	
	@Override
	public void add(ContinuousSpace<Object> space, Object agent) {
		// Get needed runtime parameters
		int keySize = (Integer) parameters.get(ChordUtilities.KEY_SIZE);
		
		if(agent instanceof Node | agent instanceof Marker) {
			Long id;
			if(agent instanceof Node) {
				Node node = (Node) agent;
				id = node.getId();
			} else {
				Marker marker = (Marker) agent;
				id = marker.getId();
			}
			
			// Get the radius of the ring
			double r = BigDecimal.valueOf(2)
					.pow(keySize)
					.divide(Visualizer.getNormalizeIdParameter(), RoundingMode.HALF_UP)
					.divide(BigDecimal.valueOf(Math.PI), RoundingMode.HALF_UP)
					.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP).doubleValue();
			
			// Convert the id normalising it
			double convertedId = new BigDecimal(Long.toUnsignedString(id))
					.divide(Visualizer.getNormalizeIdParameter(), RoundingMode.HALF_UP).doubleValue();
			
			// Get the coordinates of the centre
			double centerY = space.getDimensions().getHeight() / 2;
			double centerX = space.getDimensions().getWidth() / 2;
			
			// Get the coordinates of the node w.r.t the centre
			double nodeY = Math.cos(convertedId / r) * r;	
			double nodeX = Math.sin(convertedId / r) * r;
			
			// Place the node in the correct position inside the continuous space
			if(agent instanceof Node) {
				space.moveTo((Node)agent, centerX + nodeX, centerY + nodeY);
			} else {
				space.moveTo((Marker)agent, centerX + nodeX, centerY + nodeY);
			}
			
		}	
		
	}

}
