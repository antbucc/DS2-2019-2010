package chord;

import java.math.BigInteger;

/**
 * 
 * @author Simone Pirocca
 *
 */
public class QueryResponse 
{
	private BigInteger key;
	private String value;
	private int pathLength;
	private Node sender;
	private Node liable;
	
	public QueryResponse() 
	{}

	
	/**
	 * @param key
	 * @param value
	 * @param pathLength
	 * @param sender
	 * @param liable
	 */
	public QueryResponse(BigInteger key, String value, int pathLength, Node sender, Node liable) 
	{
		this.key = key;
		this.value = value;
		this.pathLength = pathLength;
		this.sender = sender;
		this.liable = liable;
	}

	public void setKey(BigInteger key) {
		this.key = key;
	}


	public void setValue(String value) {
		this.value = value;
	}


	public void setPathLength(int pathLength) {
		this.pathLength = pathLength;
	}


	public void setSender(Node sender) {
		this.sender = sender;
	}


	public void setLiable(Node liable) {
		this.liable = liable;
	}


	/**
	 * @return the key
	 */
	public BigInteger getKey() 
	{
		return key;
	}

	/**
	 * @return the value
	 */
	public String getValue() 
	{
		return value;
	}

	/**
	 * @return the pathLength
	 */
	public int getPathLength() 
	{
		return pathLength;
	}

	/**
	 * @return the sender
	 */
	public Node getSender() 
	{
		return sender;
	}

	/**
	 * @return the liable
	 */
	public Node getLiable() 
	{
		return liable;
	}
}

