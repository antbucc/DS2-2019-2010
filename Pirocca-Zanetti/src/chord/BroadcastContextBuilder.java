/**
 * 
 */

package chord;

import java.util.ArrayList;

import chord.utils.NetworkConfiguration;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

/**
 * @author Marco Zanetti
 *
 */
public class BroadcastContextBuilder implements ContextBuilder<Object> 
{

	@Override
	public Context build(Context<Object> context) {
		
		context.setId("chord");
		
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(
				"infection network", context, true);
		netBuilder.buildNetwork();
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 50,
				50);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(),
						new SimpleGridAdder<Object>(), true, 50, 50));
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		int mParam = (int) params.getValue("m");
		int rParam = (int) params.getValue("r");
		int periodParam = (int) params.getValue("period");
		int maxKeys = (int) params.getValue("max_keys");
		
		NetworkConfiguration networkConf = new NetworkConfiguration((short)mParam, (short)periodParam);
		
		int nodesCount = (Integer) params.getValue("node_number");
		ArrayList<Node> nodesList = new ArrayList<Node>();
		
		for (int i = 0; i < nodesCount; i++) 
		{
			Node node = new Node(networkConf);
			nodesList.add(node);
			context.add(node);
		}
		
		SuperAgent superAgent = new SuperAgent(nodesList, mParam, maxKeys);
		context.add(superAgent);
		
		//nodesList.get(1).createChordRing();
		superAgent.ExecuteChaosThread();
		superAgent.ExecuteKeyIntroductionThread();
		superAgent.ExecuteGettingKeyThread();
		
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(20);
		}
		
		return context;
	}

}
