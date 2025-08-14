package mwe;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import com.github.pvf2005.cliparameters.CommandLineParameters;
import com.github.pvf2005.cliparameters.Parameter;
import com.github.pvf2005.genericaimodelclient.GenericAIModelClient;

/**
 * Hello world!
 */
public class SimpleMWE {
	
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
    	ConsoleAppender app=new ConsoleAppender();
    	app.setTarget(ConsoleAppender.SYSTEM_ERR);
    	app.addFilter(new Filter() {

			@Override
			public int decide(LoggingEvent event) {
				return event.getLevel()!=Level.DEBUG?Filter.ACCEPT:Filter.DENY;
			}
    		
    	});
    	BasicConfigurator.configure(app);

    	CommandLineParameters params=new CommandLineParameters();
    	params.addParameter(new Parameter("model","Model","string",true,"llama3.3"));
    	params.addParameter(new Parameter("key","API key (X.AI, ChatGPT, etc., optional)","string",true,null));
    	params.addParameter(new Parameter("csv_in","CSV","string",false,null));
    	params.addParameter(new Parameter("csv_out","CSV","string",false,null));
    	params.addParameter(new Parameter("num_threads","Number of threads","int",true,"10"));
    	params.addParameter(new Parameter("template","Template to use for user prompts","string",false,null));
    	params.addParameter(new Parameter("template_sys","System template to use","string",false,null));
    	params.addParameter(new Parameter("temperature","Temperature","string",true,null));
    	params.addParameter(new Parameter("reasoning_effort","Reasoning Effort","string",true,null));
    	params.addParameter(new Parameter("type","Model type","string",true,"ollama"));
    	params.addParameter(new Parameter("endpoint","Model endpoint(s). Separate multiple endpoints by ;","string",true,"http://127.0.0.1:11434/api/chat"));
    	params.parseArguments(args);
    	
    	int numThreads=params.getInt("num_threads");
    	String []endpoints=params.getString("endpoint").split("[;]");
    	String templateSystemFile=params.getString("template_sys");
    	String templateUserFile=params.getString("template");
    	String csv_in=params.getString("csv_in");
    	String csv_out=params.getString("csv_out");
    	String model=params.getString("model");
    	
    	String templateSystem=Files.readString(Path.of(templateSystemFile), Charset.forName("UTF-8"));
    	String templateUser=Files.readString(Path.of(templateUserFile), Charset.forName("UTF-8"));
    	
    	if(numThreads<1) {
    		System.out.println("ERROR: numThreads less than 1");
    		System.exit(-1);
    	}
    	
    	System.out.println(String.format("CSV_IN=%s\nCSV_OUT=%s\nEndpoint(s)=%s\nModel=%s\n",
    			csv_in, csv_out,
    			params.getString("endpoint"),
    			model
    	));
    	
    	
        GenericAIModelClient client[]=new GenericAIModelClient[endpoints.length];
        for(int i=0;i<endpoints.length;i++) {
        	client[i]=new GenericAIModelClient();
        	client[i].setType(params.getString("type"));
        	client[i].setModel(params.getString("model"));
        	client[i].setEndpoint(endpoints[i]);
        	client[i].setKey(params.getString("key"));
        
        	client[i].setSaveRequestsFolder("./raw_requests");
        	client[i].setSaveResponsesFolder("./raw_responses");
        }
    	
    	System.out.println("Reading CSV file and computing stats");
    	CSVStatsExtractor statsExtractor=new CSVStatsExtractor(csv_in);
    	TMXStats stats=statsExtractor.parseFile();
    	System.out.println(String.format("Number of lines: %d",
    			stats.numTU
    	));
    	
    	TMXBuffer tmxBufferRead=new TMXBuffer();
    	tmxBufferRead.numTUVSrc=stats.numTUVsrc;
    	
    	TMXBuffer tmxBufferWrite=new TMXBuffer();
    	tmxBufferWrite.numTUVSrc=stats.numTUVsrc;

    	ArrayList<Thread> threads=new ArrayList<>(100);
    	Thread t=new CSVProcessor(csv_in,tmxBufferRead);
    	t.start();
    	threads.add(t);

    	for(int i=0;i<numThreads;i++) {
        	t=new MWEExtractorSimple(
        			tmxBufferRead, tmxBufferWrite,  
        			client[i%client.length], 
        			params.getString("temperature"),
        			templateSystem, templateUser,
        			params.getString("reasoning_effort")
        	);
        	t.start();
        	threads.add(t);
    	}
    	
    	
    	t=new ProcessMonitor(tmxBufferWrite);
    	t.start();
    	threads.add(t);

    	t=new CSVWriter(csv_out,tmxBufferWrite);
    	t.start();
    	threads.add(t);
    	
    	
    	
    	for(Thread th:threads) {
    		th.join();
    	}
    	
        System.out.println(String.format("Finished\nTranslations: %d\n",tmxBufferWrite.getInsertedPairs()));
    }
}
