package mwe;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONException;

public class JSONMWEParaStatsExtractor extends Thread {

	@SuppressWarnings("unused")
	private String jsonFile;
	private JSONArray json;
	
	
	public JSONMWEParaStatsExtractor(String jsonFile) throws JSONException, IOException {
		this.jsonFile=jsonFile;
		
		this.json=new JSONArray(Files.readString(Path.of(jsonFile), Charset.forName("UTF-8")));
	}
	
	public TMXStats parseFile() throws IOException{		  
		TMXStats stats=new TMXStats();
		stats.numPairs=this.json.length();
		stats.numTU=stats.numPairs;
		stats.numTUV=stats.numTU;
		stats.numTUVsrc=stats.numTU;
		stats.numTUVdst=stats.numTU;
	    
	    return stats;
	}	
		
}

