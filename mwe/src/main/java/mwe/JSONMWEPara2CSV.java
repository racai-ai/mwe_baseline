package mwe;

import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Converts MWE CUPT to CSV file, suitable for converting PARSEME data to CSV.
 */
public class JSONMWEPara2CSV {
	
	public static void help() {
		System.out.println("JSONMWEPara2CSV MWEJSON CSV");
		System.exit(-1);
	}
	
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
    	
    	if(args.length!=2)help();
    	
    	String json=args[0];
    	String csv=args[1];
    	
    	System.out.println(String.format("JSON=%s\nCSV=%s\n",
    			json,csv
    	));
    	    	
    	System.out.println("Reading MWE JSON file and computing stats");
    	JSONMWEParaStatsExtractor statsExtractor=new JSONMWEParaStatsExtractor(json);
    	TMXStats stats=statsExtractor.parseFile();
    	System.out.println(String.format("Number of TU: %d\nNumber of TUV: %d\nNumber of TUV: %d",
    			stats.numTU, stats.numTUV, stats.numTUVsrc
    	));
    	
    	TMXBuffer tmxBufferRead=new TMXBuffer();
    	tmxBufferRead.numTUVSrc=stats.numTUVsrc;
    	
    	ArrayList<Thread> threads=new ArrayList<>(100);
    	Thread t=new JSONMWEParaProcessor_id_rawtext(json, tmxBufferRead);
    	t.start();
    	threads.add(t);

    	t=new ProcessMonitor(tmxBufferRead);
    	t.start();
    	threads.add(t);

    	t=new CSVWriter(csv, tmxBufferRead);
    	t.start();
    	threads.add(t);
    	
    	for(Thread th:threads) {
    		th.join();
    	}
    	
        System.out.println(String.format("Finished\nSource sentences: %d\n",tmxBufferRead.getInsertedPairs()));
    }
}
