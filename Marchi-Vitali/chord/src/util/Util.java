package util;

import java.util.Collection;
import java.util.NoSuchElementException;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;

public final class Util {
	
	// Hidden constructor
	private Util() {}
	
	public static <T> T extractRandom(Collection<T> collection) {
		if (collection.size() == 0) {
			System.out.println("empty set");
			return null;
		}
		int target = RandomHelper.nextIntFromTo(0, collection.size()-1);
		int i = 0;
		for (T e : collection) {
			if (i == target) return e;
			++i;
		}
		throw new NoSuchElementException();
	}
	
	// Global tick of the simulation
	public static double getTick() {
		return RunEnvironment.getInstance()
							 .getCurrentSchedule()
							 .getTickCount();
	}

}
