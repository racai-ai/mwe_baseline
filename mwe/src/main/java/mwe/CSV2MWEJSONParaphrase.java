package mwe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.opencsv.CSVReader;

import ro.racai.base.Sentence;
import ro.racai.base.Token;
import ro.racai.conllup.CONLLUPReader;
import ro.racai.conllup.CONLLUPWriter;

/**
 * Converts MWE CUPT to CSV file, suitable for converting PARSEME data to CSV.
 */
public class CSV2MWEJSONParaphrase {
	
	public static void help() {
		System.out.println("CSV2MWEJSONParaphrase CSV_IN JSON_OUT");
		System.out.println("   CSV_IN contains paraphrases in the format ID,SENTENCE");
		System.out.println("   JSON_OUT will be the output");
		System.exit(-1);
	}
	
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
    	
    	if(args.length!=2)help();
    	
    	String csv=args[0];
    	String jsonOut=args[1];
    	
    	
    	System.out.println(String.format("CSV_IN=%s\nJSON_OUT=%s\n",
    			csv,jsonOut
    	));

		CSVReader csvIn=new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(csv), Charset.forName("UTF-8"))));
		JSONArray json=new JSONArray();
		
		int lnum=0;
		for(String[] data=csvIn.readNext();data!=null;data=csvIn.readNext()) {
			lnum++;
			if(data.length!=2) {
				System.out.println(String.format("ERROR: Invalid entry in CSV (line: %d)",lnum));
				System.exit(-1);
			}
			
			JSONObject ob=new JSONObject();
			ob.put("source_sent_id", data[0]);
			ob.put("prediction", data[1]);
			json.put(ob);
		}
		
    	csvIn.close();
    	
    	BufferedWriter outWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonOut),Charset.forName("UTF-8")));
    	json.write(outWriter);
    	outWriter.close();
    	
    }
}
