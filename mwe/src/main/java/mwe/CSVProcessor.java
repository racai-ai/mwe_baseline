package mwe;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;

import com.opencsv.CSVReader;

import java.io.InputStreamReader;

public class CSVProcessor extends Thread {

	private String csv;
	private TMXBuffer tmxBuffer;
	private CSVReader in;
	
	
	public CSVProcessor(String csv,TMXBuffer tmxBuffer) throws FileNotFoundException {
		this.csv=csv;
		this.tmxBuffer=tmxBuffer;
		this.in=new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(this.csv), Charset.forName("UTF-8"))));
	}
	
	@Override
	public void run() {
		try {
			String[] nextRecord; 
			  
	        // we are going to read data line by line 
	        while ((nextRecord = in.readNext()) != null) { 
	        	if(nextRecord.length<2 || nextRecord[0].isEmpty())continue;
	        	
	        	tmxBuffer.add(new TMXPair(nextRecord[0], Collections.singletonList(nextRecord[1]), new HashMap<String,String>(1)));
	        }
	    } catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		tmxBuffer.setDone();
		
		System.out.println("CSVProcessor: end thread");
	}
	
}

