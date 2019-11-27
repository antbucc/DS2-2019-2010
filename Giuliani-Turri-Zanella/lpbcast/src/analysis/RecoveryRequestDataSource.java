package analysis;

import java.util.Iterator;

import lpbcast.Configuration;

public class RecoveryRequestDataSource implements repast.simphony.data2.AggregateDataSource{

	@Override
	public String getId() {
		return "avg_recovery_requests";
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
			return "avg_recovery=" + c.getRecoveryData() + ", fanout=" + Configuration.F + ", view=" + Configuration.VIEW_MAX_SIZE;
		}
		
		return "Error in getting MessageRecoveryData";
	}

	@Override
	public void reset() {}

}
