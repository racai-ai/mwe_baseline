package mwe;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import com.opencsv.CSVReader;

import java.io.InputStreamReader;

public class CSVStatsExtractor extends Thread {

	private String csv;
	private CSVReader in;
	
	
	public CSVStatsExtractor(String csv) throws FileNotFoundException {
		this.csv=csv;
		this.in=new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(this.csv), Charset.forName("UTF-8"))));
	}
	
	public TMXStats parseFile() throws IOException{
		String[] nextRecord; 
		  
		TMXStats stats=new TMXStats();
		
        // we are going to read data line by line 
        while ((nextRecord = in.readNext()) != null) { 
        	if(nextRecord.length<2 || nextRecord[0].isEmpty())continue;
        	
        	stats.numPairs++;
        	stats.numTU++;
        	stats.numTUV+=2;
        	stats.numTUVdst++;
        	stats.numTUVsrc++;
        }
	    
		in.close();
	    
	    return stats;
	}	
		
}

