/**
 * 
 */
package lpbcast;

import java.util.ArrayList;
import java.util.Hashtable;

import lpbcast.utils.ProcessMetrics;

/**
 * @author Marco Zanetti
 *
 */
public class SuperAgent 
{
	private static final String PREFIX = "proc";
	private static int millisToSleep = 18000;				//3 minutes
	private static int counter = 0;
	
	private int totalProcesses;
	private int processesToCrash;
	ProcessMetrics procMetric;
	
	private Hashtable<String, Process> processesMap;

	public SuperAgent() {}
	
	public SuperAgent(ArrayList<Process> processes,
			ProcessMetrics procMetric,
			int totalProcesses)
	{
		this.processesMap = new Hashtable<String, Process>();
		this.procMetric = procMetric;
		
		for (Process process : processes) 
		{
			String id = generateId();
			process.setId(id);
			process.setSuperAgent(this);
			
			processesMap.put(id, process);
		}
		
		for (Process process : processes)
		{
			// get the list of random processes and sent it to every process
			ArrayList<Process> randomProcs = Util.getRandomValueSubset(procMetric.getViewSize(), processesMap);
			process.addView(randomProcs);
		}
		
		this.totalProcesses = totalProcesses;
		this.processesToCrash = (int)Math.random()*(this.totalProcesses/2);
	}
	
	/**
	 * 
	 * @param id the id of the process
	 * @return the relative process 
	 */
	public Process getProcessById(String id)
	{
		return processesMap.get(id);
	}
	
	private String generateId()
	{
		String id = PREFIX + String.valueOf(counter);
		counter++;
		return id;
	}
	
	public void ExecuteChaosThread()
	{
		 Runnable r = new Runnable() 
		 {
			
			@Override
			public void run() 
			{
				try 
				{
					ArrayList<String> procIds = Util.getRandomKeySubset(processesToCrash, processesMap);
					
					for(String id : procIds)
					{
						Process proc = processesMap.get(id);
						if(proc.isCrashed())
							proc.setCrashed(false);
						else
							proc.setCrashed(true);
					}
				
					Thread.sleep(millisToSleep);
				} 
				catch (InterruptedException e) 
				{
					// TODO Auto-generated catch block
					System.out.println("\n\nExecuteChaosThread() error\n\n");
					e.printStackTrace();
				}
			}
		};
		
		new Thread(r).start();
	}
	
}
