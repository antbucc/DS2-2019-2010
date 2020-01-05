package util;

public class KeyRange {
	private Key begin;
	private Key end;
	
	public KeyRange(Key begin, Key end) {
		if (begin.equals(end)) {
			throw new RuntimeException("Empty range");
		}
		this.begin = begin;
		this.end = end;
	}
	
	// Check if `key` is contained in range (begin, end]
	public boolean containsOpenClosed(Key key) {
		if (end.compareTo(begin) > 0) {
			// Check if `begin` < `key` <= `end`
			if (key.compareTo(begin) > 0 && key.compareTo(end) <= 0) {
				return true;
			} else {
				return false;
			}
		} else { // The range is wrapped around 0
			// Check if `end` < `begin` < `key` or `key` <= `end` < `begin`
			if ((key.compareTo(begin) > 0 && key.compareTo(end) > 0)
					|| (key.compareTo(begin) < 0 && key.compareTo(end) <= 0)) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	// Check if `key` is contained in range (begin, end)
	public boolean containsOpen(Key key) {
		if (end.compareTo(begin) > 0) {
			// Check if `begin` < `key` < `end`
			if (key.compareTo(begin) > 0 && key.compareTo(end) < 0) {
				return true;
			} else {
				return false;
			}
		} else { // The range is wrapped around 0
			// Check if `end` < `begin` < `key` or `key` < `end` < `begin`
			if ((key.compareTo(begin) > 0 && key.compareTo(end) > 0)
					|| (key.compareTo(begin) < 0 && key.compareTo(end) < 0)) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	// Check if `key` is contained in range [begin, end)
	public boolean containsClosedOpen(Key key) {
		if (end.compareTo(begin) > 0) {
			// Check if `begin` <= `key` < `end`
			if (key.compareTo(begin) >= 0 && key.compareTo(end) < 0) {
				return true;
			} else {
				return false;
			}
		} else { // The range is wrapped around 0
			// Check if `end` < `begin` <= `key` or `key` < `end` < `begin`
			if ((key.compareTo(begin) >= 0 && key.compareTo(end) > 0)
					|| (key.compareTo(begin) < 0 && key.compareTo(end) < 0)) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	public String toString() {
		return begin.toString() + ", " + end.toString();
	}
	
}
