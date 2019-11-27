package analysis;

import java.util.Iterator;

import repast.simphony.data2.AggregateDataSource;

public class RedundancyDataSource implements AggregateDataSource{

	@Override
	public String getId() {
		return "redundancies";
	}

	@Override
	public Class<?> getDataType() {
		return String.class;
	}

	@Override
	public Class<?> getSourceType() {
		return Collector.class;
	}

	@Override
	public Object get(Iterable<?> objs, int size) {
		assert size <= 1;
		Iterator<?> it = objs.iterator();
		if(it.hasNext()) {
			Collector c = (Collector) it.next();
			String row = c.getRedundancies();
			c.resetRedundancies();
			return row;
		}
		
		return "Error in getting MessagePropagationData";
	}

	@Override
	public void reset() {
		// nothing to do
		
	}

}
