package mwe;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import io.github.ollama4j.exceptions.OllamaBaseException;

/**
 * Hello world!
 */
public class MWEScoring {
	
	public static void help() {
		System.out.println("Scoring CSV_GOLD CSV_OUT");
		System.out.println("\nScoring for 2 MWE CSV");
		System.exit(-1);
	}
	
	public static HashMap<String,Boolean> getMWE(String s){
		HashMap<String,Boolean> ret=new HashMap<>(100);
		
		for(String mwe:s.split("[|]")) {
			if(mwe.contains("NONE"))break;
			mwe=mwe.toLowerCase().strip();
			if(mwe.length()>0)ret.put(mwe, Boolean.TRUE);
		}
		
		return ret;
	}
	
	public static void main(String[] args) throws OllamaBaseException, IOException, URISyntaxException, InterruptedException, ParserConfigurationException, SAXException {
    	ConsoleAppender app=new ConsoleAppender();
    	app.setTarget(ConsoleAppender.SYSTEM_ERR);
    	app.addFilter(new Filter() {

			@Override
			public int decide(LoggingEvent event) {
				return event.getLevel()!=Level.DEBUG?Filter.ACCEPT:Filter.DENY;
			}
    		
    	});
    	BasicConfigurator.configure(app);
    	
    	
    	if(args.length!=2)help();
    	
    	String csv_gold=args[0];
    	String csv_out=args[1];
    	
    	System.out.println(String.format("CSV GOLD=%s\nCSV OUT=%s",
    			csv_gold, csv_out
    	));
    	
    	TMXBuffer tmxBufferGold=new TMXBuffer();
    	TMXBuffer tmxBufferOut=new TMXBuffer();

    	System.out.println("Reading files");
    	Thread t1=new CSVProcessor(csv_gold, tmxBufferGold);
    	t1.start();
    	Thread t2=new CSVProcessor(csv_out,tmxBufferOut);
    	t2.start();
    	
    	
    	t1.join();
    	t2.join();

    	System.out.println("Computing metrics");
    	
    	int mweCandidates=0;
    	int correct=0;
    	int mweAll=0;
    	int totalSentences=0;
    	int totalCorrectNo=0;
    	int totalCorrectYes=0;
    	int totalYES=0;
    	int totalNO=0;
    	
    	while(true) {
    		TMXPair p1=tmxBufferGold.get();
    		TMXPair p2=tmxBufferOut.get();
    		if(p1==null || p2==null) break;
    		
    		totalSentences++;
    		
    		HashMap<String,Boolean> mweGold=getMWE(p1.dst.get(0));
    		HashMap<String,Boolean> mweOut=getMWE(p2.dst.get(0));
    		
    		if(mweGold.size()==0 && mweOut.size()==0)totalCorrectNo++;
    		if(mweGold.size()!=0 && mweOut.size()!=0)totalCorrectYes++;
    		if(mweGold.size()==0)totalNO++;else totalYES++;
    		
    		mweAll+=mweGold.size();
    		mweCandidates+=mweOut.size();
    		for(String mwe:mweOut.keySet()) {
    			//System.out.println(mwe);
    			if(mweGold.containsKey(mwe))correct++;
    		}
    	}
    	
    	double P=(double)correct/(double)mweCandidates;
    	double R=(double)correct/(double)mweAll;
    	double F1=2.0*P*R/(P+R);
    	
    	double accYESNO=(double)(totalCorrectYes+totalCorrectNo)/(double)totalSentences;
    	double correctYesP=(double)totalCorrectYes*100.0/(double)totalYES;
    	double correctNoP=(double)totalCorrectNo*100.0/(double)totalNO;
    	
    	System.out.println(String.format("LLM MWE Candidates=%d\nLLM Correct=%d\nGold MWE=%d\nP=%f\nR=%f\nF1=%f\n%s\nSentences=%d\nTotal YES=%d\nTotal NO=%d\nCorrect YES=%d (%f %%)\nCorrect NO=%d (%f %%)\nACC YES/NO=%f\n%s", 
    			mweCandidates,correct,mweAll,P,R,F1,String.format("%.4f & %.4f & %.4f",P,R,F1),
    			totalSentences,totalYES,totalNO,totalCorrectYes,correctYesP,   totalCorrectNo,correctNoP, accYESNO,
    			String.format("%.2f & %.2f & %.4f", correctYesP, correctNoP, accYESNO)
    	));
    	
    }
}
