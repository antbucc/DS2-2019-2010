package lpb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Utils {
	public static class Event {
		static private int globalId = 0;
		private String msg;
		private EventId eventIdRef;
		
		public Event(String msg, int originId) {
			eventIdRef = new EventId(++globalId, originId, GossipManager.getRound());
			this.msg = new String(msg);
		}
		
		public EventId getEventIdRef() {
			return eventIdRef;
		}
		
		public String getMsg() {
			return msg;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Event) {
				return ((Event)obj).eventIdRef.eventId == this.eventIdRef.eventId;
			} else if(obj instanceof EventId) {
				return ((EventId)obj).eventId == this.eventIdRef.eventId;
			} else {
				return super.equals(obj);	
			}
		}
		
		
		
	}
	
	public static class EventId {
		private int eventId;
		private int originId;
		private int timestamp;
		
		public EventId(int eventId, int originId, int timestamp) {
			this.eventId = eventId;
			this.originId = originId;
			this.timestamp = timestamp;
		}
		
		public int getOriginId() {
			return originId;
		}
		
		public int getEventId() {
			return eventId;
		}
		
		public int getTimestamp() {
			return timestamp;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Event) {
				return ((Event)obj).eventIdRef.eventId == this.eventId;
			} else if(obj instanceof EventId) {
				return ((EventId)obj).eventId == this.eventId;
			} else {
				return super.equals(obj);	
			}
		}
	}
	
	public static class EventToFetch {
		private EventId eventIdRef;
		private int rounds;
		
		public EventToFetch(EventId toFetch, int rounds) {
			this.eventIdRef = toFetch;
			this.rounds = rounds;
		}
		
		public EventId getEventIdRef() {
			return eventIdRef;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Event) {
				return ((Event)obj).eventIdRef.eventId == this.eventIdRef.eventId;
			} else if(obj instanceof EventId) {
				return ((EventId)obj).eventId == this.eventIdRef.eventId;
			} else {
				return super.equals(obj);	
			}
		}

		public int getRounds() {
			// TODO Auto-generated method stub
			return rounds;
		}
	}

	public static class HashArrayList<E> extends ArrayList<E> {

    private HashMap<E, Integer> map = new HashMap<>();
    public HashArrayList() {
    }

    public HashArrayList(Collection<? extends E> c) {
        for (E e : c) {
            add(e);
        }
    }
    
    public HashArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void ensureCapacity(int minCapacity) {
        super.ensureCapacity(minCapacity);
        HashMap<E, Integer> oldMap = map;
        map = new HashMap<>(minCapacity * 10 / 7);
        map.putAll(oldMap);
    }

    @Override
    public boolean add(E e) {
        if(!map.containsKey(e)) {
        	super.add(e);
        	map.put(e, size());
        	return true;
        }    
        return false;
    }

    @Override
    public E set(int index, E element) {
        map.remove(get(index));
        map.put(element, index);
        return super.set(index, element);
    }

    @Override
    public int indexOf(Object o) {
        int index = super.indexOf(o);
        return index;
    }
    
    @Override
    public E remove(int index) {
    	map.remove(this.get(index));
    	return super.remove(index);
    }
    @Override
    public boolean remove(Object o) {
    	map.remove(o);
    	return super.remove(o);
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
    	// TODO Auto-generated method stub
    	ArrayList<E> array = new ArrayList<E>();
    	array.addAll(c);
    	c.forEach(e -> {
    		if(map.containsKey(e))	
    			array.remove(e);
    		});
    	array.forEach(e -> {add(e);});
    	return true;
    }
    
}
}
