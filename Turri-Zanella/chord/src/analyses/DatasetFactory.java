package analyses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class DatasetFactory {
	
	/**
	 * The PrintWriter instance where the dataset's entries are appended
	 */
	private PrintWriter writer;
	/**
	 * The size of a table's row
	 */
	private int entrySize;
	
	/**
	 * Instantiate a new datasetFactory creating a csv
	 * 
	 * @param fileName the name of the file
	 * @param headerNames the headers of the csv file
	 */
	public DatasetFactory(String fileName, String...headerNames) {
		// Get the size of a row of the table
		this.entrySize = headerNames.length;
		// Create the header string
		String header = "";
		for(int i = 0; i < entrySize - 1; i++) {
			header = header + "\"" + headerNames[i] + "\",";
		}
		header = header + "\"" + headerNames[entrySize - 1] + "\"\n";
		// Create the dataset file
		try {
			writer = new PrintWriter(new File(fileName));		
			writer.write(header);
			writer.flush();
		} catch	(Exception e) {
			System.out.println("ERROR: unable to create the file");
		}
	}
	
	/**
	 * Appends a row of attributes to the file
	 * 
	 * @param attributes the attributes forming a row 
	 */
	public void addEntry(String... attributes) {
		// Check if the number of attributes passed corresponds to the number of specified headers
		if(attributes.length != entrySize) {
			throw new IllegalArgumentException("The number of attributes does not match the header size");
		} else {
			// add a table row to the file
			String entry = "";
			for(int i = 0; i < entrySize - 1; i++) {
				entry = entry + attributes[i] + ",";
			}
			entry = entry + attributes[entrySize - 1] + "\n";
			writer.write(entry);
			writer.flush();
		}
	}
	
}
