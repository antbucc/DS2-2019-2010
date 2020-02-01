/**
 * 
 */
package chord;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;

import chord.utils.HashHandler;
import chord.utils.Util;

/**
 * @author Marco Zanetti
 *
 */
public class SuperAgent 
{
	private static int chaosThreadMillisToSleep = 60000;				//1 minute
	private static int keyInsertMillisToSleep = 100;				//0.1 seconds
	private static int counter = 0;
	
	private int m;
	private int modulo;
	private int totalProcesses;
	private int processesToCrash;
	private int maxKeys;
	private short maxKeyPathLength;
	private int joinedNodes;
	
	private boolean chordRingCreated = false;
	private Hashtable<BigInteger, Node> nodesMap;
	private Hashtable<BigInteger, String> createdKeys;
	
	int lowerLimit = 48;				// numeral '0'
    int upperLimit = 122; 				// letter 'z'
    int targetStringLength = 25;
	
	Object testKey;
	Node testNode;

	public SuperAgent() {}
	
	public SuperAgent(ArrayList<Node> nodes,
			int m, int maxKeys)
	{
		this.nodesMap = new Hashtable<BigInteger, Node>();
		this.createdKeys = new Hashtable<BigInteger, String>();
		this.totalProcesses = nodes.size();
		this.modulo = (int) Math.pow(2, m);
		this.maxKeys = maxKeys;
		this.maxKeyPathLength = 0;
		
		for (Node node : nodes) 
		{
			String ip;
			BigInteger id;
			do
			{
				ip = generateRandomIP();
				id = HashHandler.hashIDModM(ip, modulo);
			}while(nodesMap.get(id) != null);
			
			node.setIp(ip);
			node.setId(id);
			node.setSuperAgent(this);
			
			nodesMap.put(id, node);
		}
		
		testKey = nodesMap.keySet().toArray()[0];
		testNode = nodesMap.get(testKey);
		
//		for (Node node : nodes)
//		{
//			// get the list of random processes and sent it to every process
//			ArrayList<Process> randomProcs = Util.getRandomValueSubset(procMetric.getViewSize(), nodesMap);
//			node.addView(randomProcs);
//		}

		this.m = m;
		this.processesToCrash = (int)(Math.random()*((double)nodes.size()/4));
		this.joinedNodes = 0;
		System.out.println("\nRicevuta lista di " + nodesMap.size() + " nodi - m: " + m);
		//this.processesToCrash = 5;
//		System.out.println("totalProcesses: " + totalProcesses);
//		System.out.println("processesToCrash: " + this.processesToCrash);
	}
	
	/**
	 * 
	 * @param id the id of the process
	 * @return the relative process 
	 */
	public Node getProcessById(String id)
	{
		return nodesMap.get(id);
	}
	
	private String generateId()
	{
		String id = "";
		return id;
	}
	
	private String generateRandomIP() 
	{
		Random r = new Random();
		return r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
	}
	
	public void ExecuteChaosThread()
	{
		 Runnable r = new Runnable() 
		 {
			
			@Override
			public void run() 
			{
				while(true) 
				{
					try 
					{					
						ArrayList<BigInteger> procIds = Util.getRandomKeySubset(processesToCrash, nodesMap);
//						System.out.println("\nprocIds list has " + procIds.size() + " entries");
						
						for(BigInteger id : procIds)
						{
							
							Node node = nodesMap.get(id);
							if(node.isCrashed())
							{
								node.setCrashed(false);
							}
							else
							{
								node.setCrashed(true);
							}
						}		
						
//						int i=1;
//						System.out.println("\n");
//						for(Node n : testNode.getFingerTable())
//						{
//							if(n!= null)
//							{
//								System.out.println(i + ". node hash: " + n.getId().toString(16));
//								i++;
//							}else {
//								System.out.println("\n node is null");
//							}
//						}
						
//						System.out.println("\ntestNode hash: " + testNode.getId().toString(16));
//						System.out.println("testNode fingerTable size -> " + testNode.getFingerTable().length + "\n");
//						
								 
						Thread.sleep(chaosThreadMillisToSleep);
//						System.out.println("\nChaosThread resuming from switch");
					} 
					catch (InterruptedException e) 
					{
						// TODO Auto-generated catch block
//						System.out.println("\n\nExecuteChaosThread() error\n\n");
						e.printStackTrace();
					}
				}
				
			}
		};
		
		//new Thread(r).start();
	}
	
	public void ExecuteKeyIntroductionThread()
	{
		
		Runnable r = new Runnable() 
		{

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				Random random = new Random();
				
				while(true)
				{
					if(createdKeys.size() > maxKeys) break;
					// TODO da togliere dopo le analisi
					if(joinedNodes >= totalProcesses/(double)10)
					{
						try
						{
							String generatedString = random.ints(lowerLimit, upperLimit + 1)
								      .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
								      .limit(targetStringLength)
								      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
								      .toString();
							
							Node node = getRandomNode();
							if(node != null && node.getPredecessor() != null)
							{
								//System.out.println("\nexecuteKeyIntroductionThread()");
								
								BigInteger keyId = HashHandler.hashIDModM(generatedString, modulo);

								if(!createdKeys.containsKey(keyId))
								{
									if(node.setValueOfKey(keyId, generatedString));
									{
										createdKeys.put(keyId, generatedString);
										System.out.println(" *** GENERATED NEW KEY: (" + keyId + "," + generatedString + ") ***");
									}
									//System.out.println("key inserted at node: " + node.getId().toString(16) +"\n\t ID= " + keyId.toString(16) + " and value: " + generatedString);
								}
							}
							
							Thread.sleep(keyInsertMillisToSleep);
							
						}catch (InterruptedException e) {
							// TODO: handle exception
							
							e.printStackTrace();
						}
					}
				}
			}
			
		};
		 
		 new Thread(r).start();
	}
	
	public void ExecuteGettingKeyThread()
	{
		
		Runnable r = new Runnable() 
		{

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				Random random = new Random();
				
				while(true)
				{
					try
					{
						if(createdKeys.size() > 0)
						{
							// choose random key among already created keys
							Set<BigInteger> keys = createdKeys.keySet();
							BigInteger key = null;
							int num = (int) (Math.random() * keys.size());
						    for(BigInteger k: keys) if (--num < 0) key = k;
						    
							Node node = getRandomNode();
							if(node != null && node.getPredecessor() != null)
							{
								//System.out.println("\nexecuteGettingKeyThread()");
								QueryResponse response = node.getValueOfKey(key, 0, node);
								if((response.getValue() != null) && (response != null))
								{
									// check whether the new path length is highier of the maximum value 
									if(response.getPathLength() > maxKeyPathLength) maxKeyPathLength = (short) response.getPathLength();
									System.out.println(" *** ASKED KEY '" + key + "' to node '" + node.getId() + "' --> [value=" + response.getValue() + 
																														" - path length:" + response.getPathLength() + 
																														" - liable:" + response.getLiable().getId() + "] ***");
								}
							}
							
							Thread.sleep(keyInsertMillisToSleep*10);
						}
					}catch (InterruptedException e) {
						// TODO: handle exception
						
						e.printStackTrace();
					}
				}
			}
			
		};
		 
		 new Thread(r).start();
	}
	
	/**
	 * return a random Node which has joined the network
	 * @return 
	 */
	public synchronized Node getRandomNode() 
	{
		List<Object> keyList = Arrays.asList(nodesMap.keySet().toArray()); 
		Collections.shuffle(keyList);
		
		for(Object key : keyList)
		{
			Node node = nodesMap.get(key);
			if(node.isJoined())
				return node;
		}
		
		return null;
		
//		Object key = nodesKeys[new Random().nextInt(nodesKeys.length)];
//		
//		randomNode = nodesMap.get(key);
//		
//		if(randomNode.isJoined())
//		{
//			return randomNode;
//		}else
//		{
//			return null;
//		}
	}

	public Hashtable<BigInteger, Node> getNodesMap() 
	{
		return nodesMap;
	}

	public boolean isChordRingCreated() 
	{
		return chordRingCreated;
	}

	public void setChordRingCreated(boolean chordRingCreated) 
	{
		this.chordRingCreated = chordRingCreated;
	}

	/**
	 * @return the maxKeyPathLength
	 */
	public short getMaxKeyPathLength() 
	{
		return maxKeyPathLength;
	}

	/**
	 * @return the maxKeys
	 */
	public int getKeysNumber() 
	{
		return createdKeys.size();
	}

	/**
	 * @param joinedNodes the joinedNodes to set
	 */
	public synchronized void incJoinedNodes() 
	{
		this.joinedNodes++;
	}
}
