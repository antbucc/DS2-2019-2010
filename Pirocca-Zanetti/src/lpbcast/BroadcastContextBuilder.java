/**
 * 
 */
package lpbcast;

import java.util.ArrayList;


import lpbcast.utils.ProcessMetrics;
import lpbcast.utils.RoundConfiguration;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
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
		
		context.setId("lpbroadcast");
		
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
		
		int intervalParam = (int) params.getValue("interval");
		int rParam = (int) params.getValue("r");
		int rrParam = (int) params.getValue("rr");
		int kParam = (int) params.getValue("k");
		int mrbrsParam = (int) params.getValue("mrbrs");
		
		RoundConfiguration roundConf = new RoundConfiguration(intervalParam, (short)kParam, (short)rParam, (short)rrParam, (short)mrbrsParam);
		
		int eventsSizeParam = (int) params.getValue("events_size");
		int eventIdsSizeParam = (int) params.getValue("eventIds_size");
		int unSubsSizeParam = (int) params.getValue("unSubs_size");
		int subsSizeParam = (int) params.getValue("subs_size");
		int viewSizeParam = (int) params.getValue("view_size");
		int fanoutParam = (int) params.getValue("fanout");
		int eventPerRoundParam = (int) params.getValue("event_per_round");
		
		ProcessMetrics procConf = new ProcessMetrics((short)eventsSizeParam, 
				eventIdsSizeParam, 
				(short)unSubsSizeParam, 
				(short)subsSizeParam, 
				(short)viewSizeParam, 
				(short)fanoutParam,
				(short)eventPerRoundParam);
		
		int agentsCount = (Integer) params.getValue("agents_count");	
		ArrayList<Process> procList = new ArrayList<Process>();
		
		for (int i = 0; i < agentsCount; i++) 
		{
			Process proc = new Process(roundConf, procConf);
			procList.add(proc);
			context.add(proc);
		}
		
		SuperAgent superAgent = new SuperAgent(procList, procConf, agentsCount);
		context.add(superAgent);
		superAgent.ExecuteChaosThread();
		
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(20);
		}
		return context;
	}

}
