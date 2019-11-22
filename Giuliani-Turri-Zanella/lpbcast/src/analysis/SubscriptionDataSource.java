package analysis;
import java.util.ArrayList;
import java.util.Iterator;
import lpbcast.Process;
import lpbcast.Configuration;
import repast.simphony.data2.AggregateDataSource;


public class SubscriptionDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "subscriber_aware_processes";
	}

	@Override
	public Class<?> getDataType() {
		return Integer.class;
	}

	@Override
	public Class<?> getSourceType() {
		return Collector.class;
	}

	@Override
	public Object get(Iterable<?> objs, int size) {
		int subscriberAwareProcesses = 0;
		
		Iterator iterator = objs.iterator();
		if(iterator.hasNext()) {
			Collector collector = (Collector) iterator.next();
			double currentTick = collector.getCurrentTick();
	
			for(Double tick : collector.getSubscriptionData().values()) {
				if(tick <= currentTick) {
					subscriberAwareProcesses++;
				}
			}
		}
		
		return subscriberAwareProcesses;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
	
}
