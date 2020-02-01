package chord.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;


public class Util 
{
	/**
	 * @param list the list to purge
	 * @param size the max size of that list
	 * 
	 * This function aims to purge a map by removing random elements
	 */
	public static <V> void purgeRandomElements(Map<BigInteger, V> map, int size)
	{
		if(map.size() > size)
		{
			// calculate the number of elements to delete
			short diff = (short) ((short)map.size() - size);
			
			ArrayList<BigInteger> keysToRemove = getRandomKeySubset(diff, map);
			
			for(BigInteger key : keysToRemove) 
				map.remove(key);
		}
	}
	
	/**
	 * 
	 * @param subs the map of 'subs'
	 * @param frequencies how many time a notification of a subscription has been received
	 * @param size the maximum size of the map
	 * 
	 * This function aims to purge a map of subs by removing more frequent elements
	 */
	public static void purgeOldSubs(Hashtable<String, Process> subs, Hashtable<String, Integer> frequencies, short size)
	{
		if(frequencies.size() > size)
		{
			// calculate the number of elements to delete
			short diff = (short) ((short)frequencies.size() - size);
			int totalFrequency = 0;
			
			Enumeration<Integer> freq = frequencies.elements();
			
			// calculate the average frequency
			while(freq.hasMoreElements())
				totalFrequency += freq.nextElement();
			int avgFrequency = (int)totalFrequency/subs.size();
			
			while(diff > 0) 
			{
				// remove the random process
				String randProcessId = selectProcess(frequencies, avgFrequency);
				subs.remove(randProcessId);
				frequencies.remove(randProcessId);
				
				diff--;
			}
		}
	}
	
	/**
	 * 
	 * @param frequencies the map of subs and relative frequencies
	 * @param avg the average that the random frequency has to exceed
	 * @return the random process id
	 */
	private static String selectProcess(Hashtable<String, Integer> frequencies, int avg)
	{
		boolean found = false;
		String randProcessId = null;
		
		while(!found)
		{
			randProcessId = (String) Util.getRandomElement(frequencies.keySet());
			if(frequencies.get(randProcessId) > avg) found = true;
			else
			{
				Integer tmpInt = Integer.valueOf(frequencies.get(randProcessId).intValue() + 1);
				frequencies.put(randProcessId, tmpInt);
			}
		}
		
		return randProcessId;	
	}
	

	/**
	 * Returns a random element from a collection
	 * @return 
	 */
	public static <T> T getRandomElement(Collection<T> collection)
	{
		/*return (T) collection.stream()
		            .skip((int) (collection.size() * Math.random()))
		            .findFirst();*/
		T element = null;
		int num = (int) (Math.random() * collection.size());
	    for(T t: collection) if (--num < 0) element = t;
	    
	    return element;
	}
	
	/**
	 * @param numElements number of random elements to retrieve
	 * @param dataMap the original dataset
	 * 
	 * Returns a random subset of keys of the given dataset
	 * 
	 */
	public static <V> ArrayList<BigInteger> getRandomKeySubset(int numElements, Map<BigInteger,V> dataMap)
	{
		
		Random rand = new Random();
		ArrayList<BigInteger> mapKeys = new ArrayList<BigInteger>();
		mapKeys.addAll(dataMap.keySet());
		ArrayList<BigInteger> randomSubset = new ArrayList<BigInteger>();
 
	    for (int i = 0; i < numElements; i++) {
	        int randomIndex = rand.nextInt(mapKeys.size());
	        randomSubset.add(mapKeys.get(randomIndex));
	        mapKeys.remove(randomIndex);
	    }
		
	    return randomSubset;
	}
	
	/**
	 * @param numElements number of random elements to retrieve
	 * @param dataMap the original dataset
	 * 
	 * Returns a random subset of keys of the given dataset
	 * 
	 */
	public static <V> ArrayList<V> getRandomValueSubset(int numElements, Map<String,V> dataMap)
	{
		Random rand = new Random();
		ArrayList<V> mapKeys = new ArrayList<V>();
		mapKeys.addAll(dataMap.values());
		ArrayList<V> randomSubset = new ArrayList<V>();
 
	    for (int i = 0; i < numElements; i++) {
	        int randomIndex = rand.nextInt(mapKeys.size());
	        randomSubset.add(mapKeys.get(randomIndex));
	        mapKeys.remove(randomIndex);
	    }
		
	    return randomSubset;
	}
	
}
