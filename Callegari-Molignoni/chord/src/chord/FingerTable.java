package chord;

import java.util.Arrays;
import java.util.Iterator;

/**
 * The finger table used by nodes in a Chord ring. It maintains an array of
 * fingers.
 */
public class FingerTable implements Iterable<Integer> {
	/**
	 * Size of the finger table.
	 */
	private int size;

	/**
	 * The index of the next finger to update.
	 */
	private int next;

	/**
	 * Array that stores the fingers.
	 */
	private Integer array[];

	/**
	 * Constructor of the finger table.
	 */
	public FingerTable(int size) {
		this.size = size;
		
		next = 0;
		array = new Integer[size];

		// Initialize every finger to a default value.
		for (int i = 0; i < size; i++) {
			array[i] = -1;
		}
	}

	/**
	 * Returns an iterator for the fingers.
	 * 
	 * @return the iterator.
	 */
	@Override
	public Iterator<Integer> iterator() {
		return Arrays.asList(array).iterator();
	}

	/**
	 * Returns a string representation of the object.
	 * 
	 * @return the string representation.
	 */
	@Override
	public String toString() {
		String res = "";

		// Create the string.
		res += "[";
		for (int item : this) {
			res += "\t" + item;
		}
		res += "\t]";

		return res;
	}

	/**
	 * Gets the size of the finger table.
	 * 
	 * @return the size.
	 */
	public int size() {
		return size;
	}

	/**
	 * Gets a finger.
	 * 
	 * @param index the index of the finger to set.
	 * @return the value of the finger or -1 for invalid indices.
	 */
	public int get(int index) {
		// Check whether the index is valid.
		if (0 <= index && index < size) {
			// Return the value.
			return array[index];
		} else {
			return -1;
		}
	}

	/**
	 * Sets a finger.
	 * 
	 * @param index the index of the finger to set.
	 * @param value the value of the finger.
	 */
	public void set(int index, int value) {
		// Check whether the index is valid.
		if (0 <= index && index < size) {
			// Set the value.
			array[index] = value;
		}
	}

	/**
	 * Gets the index of the next finger.
	 * 
	 * @return the index.
	 */
	public int getNext() {
		return next;
	}

	/**
	 * Increments the index of the next finger.
	 */
	public void setNext() {
		next += 1;

		// Check whether the index has overflowed.
		if (next >= size) {
			next = 0;
		}
	}
}
