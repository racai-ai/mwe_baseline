package mwe;
import ro.racai.base.Sentence;
import ro.racai.conllup.CONLLUPReader;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class CONLLUPProcessor extends Thread {

	private String conllup;
	private TMXBuffer tmxBuffer;
	
	public CONLLUPProcessor(String conllup, TMXBuffer tmxBuffer) {
		this.conllup=conllup;
		this.tmxBuffer=tmxBuffer;
	}
	
	@Override
	public void run() {
		try {
			
			CONLLUPReader in=new CONLLUPReader(Paths.get(this.conllup));
			Sentence sent;
			while((sent=in.readSentence())!=null) {
				HashMap<String,String> meta=sent.getMetadata();
				if(meta.containsKey("text")) {
					String text=meta.get("text");
					tmxBuffer.add(new TMXPair(text, new ArrayList<String>(1),new HashMap<String,String>(1)));
				}
			}
			in.close();
			tmxBuffer.setDone();
		}catch(Exception ex) {
			ex.printStackTrace();
			tmxBuffer.setDone();
		}
		System.out.println("CONLLUPProcessor: end thread");
	}
	
}
