package chord.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashHandler 
{
	/*
	 * Hash a string using SHA-1 algorithm
	 * Returns a BigInteger representing the hashed value 
	 */
	public static BigInteger hashID(String id)
	{
		try
		{
			// getInstance() method is called with algorithm SHA-1 
            MessageDigest md = MessageDigest.getInstance("SHA-1"); 
            
            // digest() method is called 
            // to calculate message digest of the input string 
            // returned as array of byte 
            byte[] messageDigest = md.digest(id.getBytes()); 
  
            // Convert byte array into signum representation 
            BigInteger idNum = new BigInteger(1, messageDigest); 
            
            return idNum;
  
		}catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        } 
	}
	
	/*
	 * Hash a string using SHA-1 algorithm
	 * Returns a BigInteger representing the hashed value, reduced to m bit
	 */
	public static BigInteger hashIDModM(String id, int modulo)
	{
		try
		{
			// getInstance() method is called with algorithm SHA-1 
            MessageDigest md = MessageDigest.getInstance("SHA-1"); 
            
            // digest() method is called 
            // to calculate message digest of the input string 
            // returned as array of byte 
            byte[] messageDigest = md.digest(id.getBytes()); 
  
            // Convert byte array into signum representation 
            BigInteger idNum = new BigInteger(1, messageDigest); 
            
            // Reduce the bigInt to m bit number
            BigInteger hash = idNum.mod(BigInteger.valueOf(Integer.valueOf(modulo)));
            
            return hash;
  
		}catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        } 
	}
}
