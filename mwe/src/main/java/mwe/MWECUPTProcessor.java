package mwe;
import ro.racai.base.Sentence;
import ro.racai.base.Token;
import ro.racai.conllup.CONLLUPReader;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class MWECUPTProcessor extends Thread {

	private String conllup;
	private TMXBuffer tmxBuffer;
	
	public MWECUPTProcessor(String conllup, TMXBuffer tmxBuffer) {
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
					HashMap<String,String> mwes=new HashMap<>(10);
					for(Token t: sent.getTokens()) {
						String mwe=t.getByKey("PARSEME:MWE");
						if(mwe.contentEquals("*"))continue;
						String mweid=mwe.replaceAll("(\\d+).+", "$1");
						if(mwes.containsKey(mweid)) {
							mwes.put(mweid,mwes.get(mweid)+" "+t.getByKey("FORM"));
						}else {
							mwes.put(mweid,t.getByKey("FORM"));
						}
					}
					ArrayList<String> mweList=new ArrayList<>(1);
					if(mwes.size()>0) {
						mweList.add(String.join("|", mwes.values()));
					}else mweList.add("");
					tmxBuffer.add(new TMXPair(text, mweList, new HashMap<String,String>(1)));
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
