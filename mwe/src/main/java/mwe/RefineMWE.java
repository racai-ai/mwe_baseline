package mwe;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.xml.sax.SAXException;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;

/**
 * Hello world!
 */
public class RefineMWE {
	
	public static void help() {
		System.out.println("RefineMWE CSV_IN CSV_OUT NUM_THREADS OLLAMA_URI MODEL TEMPERATURE TEMPLATE_SYSTEM TEMPLATE_USER TEMPLATE_REFINE");
		System.out.println("\nSplit multiple OLLAMA URIs with ';'");
		System.out.println("Templates are files containing the template text to be sent to OLLAMA. Will be passed to String.format.");
		System.out.println("TEMPLATE_USER should contain %s for replacement with the sentence.");
		System.out.println("TEMPLATE_REFINE should contain 2x %s for replacement with the sentence and the MWE.");
		System.out.println("TEMPLATE_SYSTEM and TEMPLATE_USER should be the same used for SimpleMWE.");
		System.out.println("CSV_IN should be the output from SimpleMWE.");
		System.exit(-1);
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
    	
    	if(args.length!=9)help();
    	
    	String csv_in=args[0];
    	String csv_out=args[1];
    	int numThreads=Integer.parseInt(args[2]);
    	String ollamaURI=args[3];
    	String []ollamaHost=ollamaURI.split("[;]");
    	//String ollamaEmbeddingsModel="all-minilm";
    	String ollamaTranslationModel= args[4]; //"llama3.3";
    	float temp=Float.parseFloat(args[5]);
    	String templateSystemFile= args[6];
    	String templateUserFile= args[7];
    	String templateRefineFile= args[8];
    	
    	String templateSystem=Files.readString(Path.of(templateSystemFile), Charset.forName("UTF-8"));
    	String templateUser=Files.readString(Path.of(templateUserFile), Charset.forName("UTF-8"));
    	String templateRefine=Files.readString(Path.of(templateRefineFile), Charset.forName("UTF-8"));
    	
    	if(numThreads<1) {
    		System.out.println("ERROR: numThreads less than 1");
    		System.exit(-1);
    	}
    	
    	System.out.println(String.format("CSV_IN=%s\nCSV_OUT=%s\nOLLAMA_HOST=%s\nOLLAMA_Model=%s\nTemperature=%f\n",
    			csv_in, csv_out,
    			ollamaURI,ollamaTranslationModel,
    			temp
    	));
    	
    	System.out.println("Connecting to OLLAMA and pulling embeddings and translation models");
    	OllamaAPI []ollamaAPI=new OllamaAPI[ollamaHost.length];
    	for(int i=0;i<ollamaHost.length;i++) {
    		ollamaAPI[i] = new OllamaAPI(ollamaHost[i]);
    		ollamaAPI[i].setRequestTimeoutSeconds(15*60);
    		//ollamaAPI[i].pullModel(ollamaEmbeddingsModel);
    		ollamaAPI[i].pullModel(ollamaTranslationModel);
    	}
    	System.out.println("DONE");
    	
    	/*OllamaEmbedResponseModel embeddings = ollamaAPI[0].embed(ollamaEmbeddingsModel, Arrays.asList("Why is the sky blue?"));
    	int embeddingsDimension=embeddings.getEmbeddings().get(0).size();
    	System.out.println("Embeddings dimension="+embeddingsDimension);*/
    	
    	/*System.out.println("Connecting to MILVUS");
    	ConnectConfig config = ConnectConfig.builder().uri(db_uri).build();    	
    	MilvusClientV2 client = new MilvusClientV2(config);
    	if(client.hasCollection(HasCollectionReq.builder().collectionName(db_collection).build())) {
    		System.out.println("Collection exists");
    	}else {
			System.out.println("Collection does not exist");
			System.exit(-1);
    	}
    	
    	client.loadCollection(LoadCollectionReq.builder().collectionName(db_collection).build());*/
    	
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
        	t=new MWEExtractorRefine(
        			tmxBufferRead, tmxBufferWrite,  
        			ollamaAPI[i%ollamaAPI.length], ollamaTranslationModel, temp,
        			templateSystem, templateUser, templateRefine
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
