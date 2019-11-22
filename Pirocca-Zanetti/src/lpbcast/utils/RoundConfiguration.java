/**
 * 
 */
package lpbcast.utils;

/**
 * @author Marco Zanetti
 *
 */
public class RoundConfiguration
{
	
	private short k, r, rr, maxRoundsBeforeReSubscribe;
	private int interval;
	
	/**
	 * @param interval the duration of each round
	 * @param k the max number of rounds before asking event to gossip sender
	 * @param r the max number of rounds before asking event to random process
	 * @param rr the max number of rounds before asking event source process
	 * @param maxRoundsBeforeReSubscribe the max round before resend the substribe notification
	 */
	public RoundConfiguration(int interval,
			short k, 
			short r, 
			short rr,
			short maxRoundsBeforeReSubscribe) 
	{
		this.k = k;
		this.r = r;
		this.rr = rr;
		this.interval = interval;
		this.maxRoundsBeforeReSubscribe = maxRoundsBeforeReSubscribe;
	}

	public short getK() 
	{
		return k;
	}

	public void setK(short k) 
	{
		this.k = k;
	}

	public short getR() 
	{
		return r;
	}

	public void setR(short r) 
	{
		this.r = r;
	}

	public short getRr() 
	{
		return rr;
	}

	public void setRr(short rr) 
	{
		this.rr = rr;
	}

	public int getInterval() 
	{
		return interval;
	}

	public void setInterval(int interval) 
	{
		this.interval = interval;
	}

	/**
	 * @return the maxRoundsBeforeReSubscribe
	 */
	public short getMaxRoundsBeforeReSubscribe() 
	{
		return maxRoundsBeforeReSubscribe;
	}

	/**
	 * @param maxRoundsBeforeReSubscribe the maxRoundsBeforeReSubscribe to set
	 */
	public void setMaxRoundsBeforeReSubscribe(short maxRoundsBeforeReSubscribe) 
	{
		this.maxRoundsBeforeReSubscribe = maxRoundsBeforeReSubscribe;
	}
}
