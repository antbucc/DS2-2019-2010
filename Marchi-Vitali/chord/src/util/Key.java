package util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Key implements Comparable<Key> {
	
	private static MessageDigest sha1Hasher = null;
	
	private BigInteger repr;
	
	public Key(String stringKey) {
		repr = new BigInteger(1, computeHash(stringKey));
	}
	
	public Key(int intKey) {
		this(Integer.toString(intKey));
	}
	
	public Key(Object genericKey) {
		this(genericKey.toString());
	}
		
	public Key (BigInteger bigInt) { 
		repr = bigInt;
	}
	
	@Override
	public int compareTo(Key other) {
		return repr.compareTo(other.repr);
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) {
	        return true;
	    }
	    if (other instanceof Key) {
	        Key otherKey = (Key) other;
	        return this.repr.equals(otherKey.repr);
	    }
	    return false;
	}
	
	public BigInteger toBigInteger() {
		return repr;
	}
	
	private static byte[] computeHash(String input) {
		// Initialize the static hasher if necessary
		if (sha1Hasher == null) {
			try {
				sha1Hasher = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		return sha1Hasher.digest(input.getBytes());
	}
	
	public String toString() {
		String hexString = repr.toString(16);
		String leadingZeros = new String(new char[40-hexString.length()]).replace("\0", "0");
		return leadingZeros + hexString;
	}
	
	public int getTenMostSignifDigits() {
		return repr.shiftRight(150).intValueExact();
	}
	
	
	public static void main(String[] args) {
		// Unit testing
		Key key1 = new Key("key1");
		Key key2 = new Key("key2");
		Key key3 = new Key(3);
		
		System.out.println("key1 " + key1);
		System.out.println("key2 " + key2);
		System.out.println("key3 " + key3);
		
		System.out.println(key1.compareTo(key2));
		System.out.println(key1.compareTo(key3));
		System.out.println(key2.compareTo(key3));
		
		KeyRange range1 = new KeyRange(key1, key2);
		System.out.println("range1 (key1, key2) " + range1);
		System.out.println("range1 contains (open-closed) key3 " + range1.containsOpenClosed(key3));
		System.out.println("range1 contains (open) key(3) " + range1.containsOpen(new Key(3)));
		System.out.println("range1 contains (open) key2 " + range1.containsOpen(key2));
		System.out.println("range1 contains (open-closed) key2 " + range1.containsOpenClosed(key2));
		System.out.println("range1 contains (open-closed) key1 " + range1.containsOpenClosed(key1));
		System.out.println("range1 contains key1 (open) " + range1.containsOpen(key1));

		Key key4 = new Key("abcd");
		Key key5 = new Key("123");
		Key key6 = new Key(12345);
		
		KeyRange range2 = new KeyRange(key4, key5);
		System.out.println(key4);
		System.out.println(key5);
		System.out.println(key6);
		System.out.println(range2);
		
		System.out.println(range2.containsOpen(key6));
		System.out.println(range2.containsOpenClosed(key1));
	}

}
