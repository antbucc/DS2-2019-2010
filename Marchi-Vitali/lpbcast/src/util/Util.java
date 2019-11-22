package util;

import java.util.Random;
import java.util.Set;

public final class Util {
	
	// Hidden constructor
	private Util() {}
	
	public static <T> T extractRandom(Set<T> set) {
		if (set.size() == 0) {
			System.out.println("empty set");
			return null;
		}
		int target = new Random().nextInt(set.size());
		int i = 0;
		for (T e : set) {
			if (i == target) return e;
			++i;
		}
		return null;  // Empty set
	}

}
