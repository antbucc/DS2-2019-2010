package chord.utils;

/**
 * @author Simone Pirocca
 *
 */
public class NetworkConfiguration 
{
	private short m;
	private short r;
	private short nodeNumber;
	private short period;
	
	/**
	 * @param m
	 * @param period
	 */
	public NetworkConfiguration(short m, short period) 
	{
		this.m = m;
		//this.r = r;
		this.r = m; // suggested
		this.nodeNumber = (short) Math.pow(2, m);
		this.period = period;
	}

	/**
	 * @return the m
	 */
	public short getM() 
	{
		return m;
	}

	/**
	 * @return the r
	 */
	public short getR() 
	{
		return r;
	}

	/**
	 * @return the nodeNumber
	 */
	public short getNodeNumber() 
	{
		return nodeNumber;
	}

	/**
	 * @return the period
	 */
	public short getPeriod() 
	{
		return period;
	}
}
