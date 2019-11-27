package analysis;

import java.util.Iterator;

import repast.simphony.data2.AggregateDataSource;

public class DeliveriesPerTickDataSource implements AggregateDataSource{

	@Override
	public String getId() {
		return "deliveries";
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
			String row =  c.getDeliveredEvents();
			c.resetDeliveredEvents();
			return row;
		}
		
		return "Error in getting MessagePropagationData";
	}

	@Override
	public void reset() {
		
		
	}

}
