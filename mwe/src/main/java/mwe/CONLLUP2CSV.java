package mwe;

import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Hello world!
 */
public class CONLLUP2CSV {
	
	public static void help() {
		System.out.println("CONLLUP2CSV CONLLUP CSV");
		System.exit(-1);
	}
	
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
    	
    	if(args.length!=2)help();
    	
    	String conllup=args[0];
    	String csv=args[1];
    	
    	System.out.println(String.format("CONLLUP=%s\nCSV=%s\n",
    			conllup,csv
    	));
    	    	
    	System.out.println("Reading CONLLUP file and computing stats");
    	CONLLUPStatsExtractor statsExtractor=new CONLLUPStatsExtractor(conllup);
    	TMXStats stats=statsExtractor.parseFile();
    	System.out.println(String.format("Number of TU: %d\nNumber of TUV: %d\nNumber of TUV: %d",
    			stats.numTU, stats.numTUV, stats.numTUVsrc
    	));
    	
    	TMXBuffer tmxBufferRead=new TMXBuffer();
    	tmxBufferRead.numTUVSrc=stats.numTUVsrc;
    	
    	ArrayList<Thread> threads=new ArrayList<>(100);
    	Thread t=new CONLLUPProcessor(conllup, tmxBufferRead);
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
