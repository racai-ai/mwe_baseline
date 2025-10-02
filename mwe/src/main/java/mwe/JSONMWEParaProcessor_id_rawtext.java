package mwe;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONMWEParaProcessor_id_rawtext extends Thread {

	@SuppressWarnings("unused")
	private String jsonFile;
	private TMXBuffer tmxBuffer;
	private JSONArray json;
	
	
	public JSONMWEParaProcessor_id_rawtext(String jsonFile,TMXBuffer tmxBuffer) throws JSONException, IOException {
		this.jsonFile=jsonFile;
		this.tmxBuffer=tmxBuffer;
		this.json=new JSONArray(Files.readString(Path.of(jsonFile), Charset.forName("UTF-8")));
	}
	
	@Override
	public void run() {
		try {
			for(int i=0;i<this.json.length();i++) {
				JSONObject ob=this.json.getJSONObject(i);
				String text=ob.getString("raw_text");
				String id=ob.getString("source_sent_id");
	        	
	        	tmxBuffer.add(new TMXPair(""+id, Collections.singletonList(text), new HashMap<String,String>(){{
	        		put("id",""+id);
	        	}}));
	        }
	    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		tmxBuffer.setDone();
		
		System.out.println("JSONMWEProcessor: end thread");
	}
	
}

