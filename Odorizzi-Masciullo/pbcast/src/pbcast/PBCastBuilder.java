package pbcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import analysis.DeliveryDistribution;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.util.collections.IndexedIterable;
import tree.Node;
import tree.Tree;

public class PBCastBuilder implements ContextBuilder<Object> {

	@Override
	public Context build(Context<Object> context) {
		context.setId("pbcast");
		
		// create a direct link network to represent broadcasting
		NetworkBuilder<Object> netBuilder1 = new NetworkBuilder<Object>(
				"send network", context, true);
		netBuilder1.buildNetwork();
		
		// create a direct link network to represent a specific spanning tree
		NetworkBuilder<Object> netBuilder2 = new NetworkBuilder<Object>(
				"spanning tree", context, true);
		netBuilder2.buildNetwork();
		
		// create a direct link network to represent packet exchange on a specific spanning tree
		NetworkBuilder<Object> netBuilder3 = new NetworkBuilder<Object>(
				"exchange", context, true);
		netBuilder3.buildNetwork();
		NetworkBuilder<Object> netBuilder4 = new NetworkBuilder<Object>(
				"loss network", context, true);
		netBuilder4.buildNetwork();
		NetworkBuilder<Object> netBuilder5 = new NetworkBuilder<Object>(
				"retrieved network", context, true);
		netBuilder5.buildNetwork();
		
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
		int processesCount = (Integer) params.getValue("processes_count");
		for (int i = 0; i < processesCount; i++) {
			context.add(new Process(space, grid, i));
		}
		
		// Set a start location for each process
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			Object other;
			// Processes have to be located in different places!
			do {
				double x = RandomHelper.nextDoubleFromTo(0, grid.getDimensions().getWidth());
				double y = RandomHelper.nextDoubleFromTo(0, grid.getDimensions().getHeight());
				pt = new NdPoint(x, y);
				other = grid.getObjectAt((int) pt.getX(), (int) pt.getY());
			} while (other != null);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		// Generate and share spanning trees
		ArrayList<Tree> spanning_trees = generate_spanning_trees(context);
		share_virtual_spanning_trees(spanning_trees, context);
		
		// Track the used spanning tree
		int process_id = (int) params.getValue("originator_view");
		Tree spanning_tree_view = spanning_trees.get(process_id);
		Network<Object> net = (Network<Object>) context.getProjection("spanning tree");
		this.draw_tree(spanning_tree_view, net);
		
		// statistic initializations
		DeliveryDistribution.performance = new HashMap<String, Integer>();
		DeliveryDistribution.received = new ArrayList<HashMap<String, Integer>>();
		for (int i = 0; i < processesCount; i++)
			DeliveryDistribution.received.add(new HashMap<String, Integer>());
		
		DeliveryDistribution.fanout = new ArrayList<HashMap<String, Integer>>();
		for (int i = 0; i < processesCount; i++)
			DeliveryDistribution.fanout.add(new HashMap<String, Integer>());
		
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(20);
		}
		
		return context;
	}

	private void share_virtual_spanning_trees(ArrayList<Tree> spanning_trees, Context<Object> context) {
		IndexedIterable collection = context.getObjects(Process.class);
		for(Object obj : collection)
			if (obj instanceof Process) {
				Process p = (Process) obj;
				p.setSpanningTree(spanning_trees);
			}
	}

	private ArrayList<Tree> generate_spanning_trees(Context context) {
		ArrayList<Tree> trees = new ArrayList<Tree>();
		IndexedIterable collection = context.getObjects(Process.class);
		int processesCount = collection.size();
		Grid grid = (Grid) context.getProjection("grid");
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		int max_child = (Integer) params.getValue("max_children");
		int min_child = (Integer) params.getValue("min_children");
		
		for(Object obj : collection) {
			if (obj instanceof Process) {
				Process p = (Process) obj;
				
				// Create a new spanning tree
				Tree t = new Tree(p.getId(), p);
				
				// Add n_child for each border process already in the tree, until spanning tree is complete
				while(t.getTreeSize() < processesCount) {					
					while(t.getTreeSize() < processesCount) {
						int leafs = t.getBorder().size();
						Process border = t.getBorder().get(RandomHelper.nextIntFromTo(0, leafs - 1));
						int restant_nodes = processesCount - t.getTreeSize();
						int n_child = RandomHelper.nextIntFromTo(min_child, ((restant_nodes < max_child) ? restant_nodes : max_child));
						// set randomly N neighbors as children
						ArrayList<Process> neighbors = new ArrayList<Process>();
						int distance = 1;
						while(neighbors.size() < n_child && t.getTreeSize() + neighbors.size() < processesCount) {
							
							// search busy cells near the selected border process
							GridPoint pt = grid.getLocation(border);
							GridCellNgh<Process> nghCreator = new GridCellNgh<Process>(grid, pt,
									Process.class, distance, distance);
							List<GridCell<Process>> gridCells = nghCreator.getNeighborhood(true);
							//SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
							
							// try to find a near process
							boolean found = false;
							for (GridCell<Process> cell : gridCells)
								if (cell.size() == 1 && !cell.getPoint().equals(pt)) {
									Object object_in_cell = grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY()).iterator().next();
									if (object_in_cell instanceof Process) {
										Process process_in_cell = (Process) object_in_cell;
										if (!neighbors.contains(process_in_cell) && !t.hasProcess(process_in_cell)) {
											neighbors.add(process_in_cell);
											found = true;
											break;
										}
									}
								}
							
							// if no process has been found then retry with a greater distance
							if(!found)
								distance++;
						}
						t.add_children(border, neighbors);
					}
				}
				trees.add(t);
			}
		}
		return trees;
	}
	
	private void draw_tree(Tree my_spanning_tree, Network<Object> net) {
		for(Node n : my_spanning_tree.getNodes())
			for(Node child : n.getChildren())
				net.addEdge(n.getP(), child.getP());
	}
	
}