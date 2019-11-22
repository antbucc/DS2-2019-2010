package analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import repast.simphony.data2.AggregateDataSource;
import lpbcast.Process;
import lpbcast.Configuration;

public class ViewDistributionDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "view_distribution";
	}

	@Override
	public Class<?> getDataType() {
		// TODO Auto-generated method stub
		return ArrayList.class;
	}

	@Override
	public Class<?> getSourceType() {
		// TODO Auto-generated method stub
		return Process.class;
	}

	@Override
	public Object get(Iterable<?> objs, int size) {
		
		int numOfProcesses = Configuration.INITIAL_NODES_NUMBER;
		Iterator iterator = objs.iterator();
		ArrayList<Integer> viewDistribution = new ArrayList<Integer>(Collections.nCopies(numOfProcesses, 0));
		
		while(iterator.hasNext()) {
			Process process = (Process) iterator.next();
			for(Integer processId : process.view.keySet()) {
				int occurrencies = viewDistribution.get(processId);
				viewDistribution.add(processId, occurrencies+1);
			}
		}
		
		return viewDistribution;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

}
