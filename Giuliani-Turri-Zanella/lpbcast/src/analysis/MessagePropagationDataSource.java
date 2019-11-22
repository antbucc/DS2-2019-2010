package analysis;

import java.util.Iterator;

public class MessagePropagationDataSource implements repast.simphony.data2.AggregateDataSource{

	@Override
	public String getId() {
		return "message_propagation";
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
			return c.getTickMessagePropagationData();
		}
		
		return "Error in getting MessagePropagationData";
	}

	@Override
	public void reset() {
		// do nothing
	}

}
