package chord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Utils {

	/*
	 * Contains auxiliary functions 
	 */
	
	
	public static int computeDistance(Node n1, Node n2, int maxHashCount) {
		int distance = 0;
		int half = (n1.getHashId() + maxHashCount/2) % maxHashCount;
		if(half <= maxHashCount/2) {
			distance = (n2.getHashId() <= half) ? (maxHashCount - n1.getHashId() + n2.getHashId()) : (n1.getHashId() - n2.getHashId());
		} else {
			distance = (n2.getHashId() <= half) ? (n2.getHashId() - n1.getHashId()) : (maxHashCount - n2.getHashId() + n1.getHashId()) ;
		}
		return distance;
	}
	
	public static int computeCDistance(int key1, int key2, int maxHashCount) { // Clockwise
		int distance = 0;
		if(key1 <= key2) {
			distance = key2 - key1;
		} else {
			distance = maxHashCount - key1 + key2;
		}
		return distance;
	}
	
	public static int computeCCDistance(Node n1, Node n2, int maxHashCount) { // Counter clockwise
		int distance = 0;
		if(n1.getHashId() <= n2.getHashId()) {
			distance = maxHashCount - n2.getHashId() + n1.getHashId();
		} else {
			distance = n1.getHashId() - n2.getHashId();
		}
		return distance;
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
