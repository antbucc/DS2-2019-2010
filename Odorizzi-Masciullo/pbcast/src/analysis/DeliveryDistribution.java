package analysis;

import java.util.ArrayList;
import java.util.HashMap;

public class DeliveryDistribution {
	public static HashMap<String, Integer> performance;
	public static ArrayList<HashMap<String, Integer>> received;
	public static ArrayList<HashMap<String, Integer>> fanout;
	
	public static double getReceivedAvarage() {
		int n = performance.size();
		if (n == 0)
			return 0;
		int s = 0;
		for (String key : performance.keySet()) 
			s += performance.get(key);
		return s / n;
	}

}

