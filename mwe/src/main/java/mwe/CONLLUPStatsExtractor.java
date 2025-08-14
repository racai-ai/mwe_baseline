package mwe;
import java.io.IOException;
import java.nio.file.Paths;

import ro.racai.conllup.CONLLUPReader;

public class CONLLUPStatsExtractor extends Thread {

	private String csv;
	private CONLLUPReader in;
	
	
	public CONLLUPStatsExtractor(String csv) throws IOException {
		this.csv=csv;
		this.in=new CONLLUPReader(Paths.get(this.csv));
	}
	
	public TMXStats parseFile() throws IOException{
		TMXStats stats=new TMXStats();
		
        // we are going to read data line by line
		//Sentence sent;
        while (( in.readSentence()) != null) { 
        	stats.numPairs++;
        	stats.numTU++;
        	stats.numTUV+=1;
        	stats.numTUVsrc++;
        }
	    
		in.close();
	    
	    return stats;
	}	
		
}

