package mwe;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;

import ro.racai.base.Sentence;
import ro.racai.base.Token;
import ro.racai.conllup.CONLLUPReader;
import ro.racai.conllup.CONLLUPWriter;

/**
 * Converts MWE CUPT to CSV file, suitable for converting PARSEME data to CSV.
 */
public class CSV2MWECUPT {
	
	public static void help() {
		System.out.println("CSV2MWECUPT MWE_CUPT_IN CSV MWE_CUPT_OUT");
		System.out.println("   MWE_CUPT_IN is CUPT without MWEs");
		System.out.println("   CSV contains detected MWEs for each sentence");
		System.out.println("   MWE_CUPT_OUT will be written based on the previous two files");
		System.exit(-1);
	}
	
	static class TokPos {
		String tok;
		int pos;
		
		public TokPos(String t, int p) {this.tok=t; this.pos=p;}
	}
	
	public static ArrayList<int[]> matchMWEtoTokens(String mwe, ArrayList<TokPos> forms, ArrayList<TokPos> lemmas) {
		String[] testTok = mwe.split("[ ]");
		ArrayList<int[]> ret = new ArrayList<>(5);
		int[] ids = null;
		for (int i = 0; i < forms.size(); i++) {
			if (testTok[0].equalsIgnoreCase(forms.get(i).tok) || testTok[0].equalsIgnoreCase(lemmas.get(i).tok)) {
				ids = new int[testTok.length];
				int idPos = 0;
				ids[idPos] = forms.get(i).pos;
				idPos++;
				for (; idPos < testTok.length; idPos++) {
					int found = -1;
					for (int j = ids[idPos - 1] + 1; j < forms.size(); j++) {
						if (forms.get(j).tok.equalsIgnoreCase(testTok[idPos]) || lemmas.get(j).tok.equalsIgnoreCase(testTok[idPos])) {
							found = forms.get(j).pos;
							break;
						}
					}
					if (found == -1 || found-ids[idPos-1]>3)
						break;
					ids[idPos] = found;
				}
				if (idPos == testTok.length) {
					ret.add(ids);
					ids = null;
				}
			}
		}
		return ret;
	}
		
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
    	
    	if(args.length!=3)help();
    	
    	String conllupInStr=args[0];
    	String csv=args[1];
    	String conllupOutStr=args[2];
    	
    	
    	System.out.println(String.format("CUPT_IN=%s\nCSV=%s\nCUPT_OUT=%s\n",
    			conllupInStr,csv,conllupOutStr
    	));

		//CSVReader csvIn=new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(csv), Charset.forName("UTF-8"))));
		CSVReader csvIn=new CSVReaderBuilder(new BufferedReader(new InputStreamReader(new FileInputStream(csv), Charset.forName("UTF-8"))))
         .withCSVParser(new RFC4180ParserBuilder().build())
         .build();
		
		CONLLUPReader in=new CONLLUPReader(Paths.get(conllupInStr));
   		CONLLUPWriter out=new CONLLUPWriter(Paths.get(conllupOutStr), List.of("global.columns"), List.of("source_sent_id","text"), List.of("ID","FORM","LEMMA","UPOS","XPOS","FEATS","HEAD","DEPREL","DEPS","MISC","PARSEME:MWE"));
		Sentence sentIn;
		int lnum=0;
		while((sentIn=in.readSentence())!=null) {
			lnum++;
			System.out.println("CSV LINE: "+lnum);
			String []data=csvIn.readNext();
			if(data==null || data.length!=2) {
				System.out.println(String.format("ERROR: Invalid entry in CSV (line: %d)",lnum));
				System.exit(-1);
			}
			String []mwes=data[1].split("[|]");
			Sentence sentOut=new Sentence();
			sentOut.setMetadata(sentIn.getMetadata());

			ArrayList<TokPos> forms=new ArrayList<>();
			ArrayList<TokPos> lemmas=new ArrayList<>();
			String []formsString=new String[sentIn.getNumTokens()];
			
			int i=0;
			for(Token tok:sentIn.getTokens()) {
				String form=tok.getByKey("FORM").toLowerCase();
				formsString[i]=form;
				forms.add(new TokPos(form,i));
				if(form.contains("-") || form.contains(" ")) {
					for(String s:form.split("[- ]")) {
						forms.add(new TokPos(s,i));
					}
				}

				form=tok.getByKey("LEMMA").toLowerCase();
				lemmas.add(new TokPos(form,i));
				if(form.contains("-") || form.contains(" ")) {
					for(String s:form.split("[- ]")) {
						lemmas.add(new TokPos(s,i));
					}
				}
				
				for(int k=forms.size();k<lemmas.size();k++)forms.add(new TokPos("",i));
				for(int k=lemmas.size();k<forms.size();k++)lemmas.add(new TokPos("",i));
				
				tok.setByKey("PARSEME:MWE", "");
				i++;
			}
			
			int currentMWENum=0;
			for(String mwe:mwes) {
				mwe=mwe
						.strip()
						.replace("'", " '")
						.replace("```", " ")
						.replace("-", " ")
						.replace("...", " ")
						.replaceAll(" +", " ")
						.strip()
					;
				
				if(mwe.isEmpty() || mwe.equalsIgnoreCase("NONE") || mwe.split("[ ]").length==1)continue;
				
				ArrayList<int[]>idsList=matchMWEtoTokens(mwe.toLowerCase(),forms,lemmas);
				if(idsList!=null && idsList.size()>0) {
					for(int[] ids:idsList) {
						currentMWENum++;
						boolean first=true;
						int previd=-1;
						for(int id:ids) {
							if(id==previd)continue;
							previd=id;
							if(sentIn.getToken(id).getByKey("PARSEME:MWE").isEmpty()) {
								sentIn.getToken(id).setByKey("PARSEME:MWE",""+currentMWENum+(first?":MWE":""));
							}else {
								/*sentIn.getToken(id).setByKey("PARSEME:MWE",
										sentIn.getToken(id).getByKey("PARSEME:MWE")+","+currentMWENum+(first?":MWE":""));*/
								break;
							}
							first=false;
						}
					}
				}else {
					System.out.println("Error matching: ["+mwe+"] in sentence ["+String.join(" ", formsString)+"]");
				}
			}
			
			for(Token tok:sentIn.getTokens()) {
				if(tok.getByKey("PARSEME:MWE").isEmpty())
					tok.setByKey("PARSEME:MWE", "*");
				sentOut.addToken(tok);
			}
			out.writeSentence(sentOut);
		}
    	in.close();
    	csvIn.close();
    	out.close();
    	
    	    	
        //System.out.println(String.format("Finished\nSource sentences: %d\n",tmxBufferRead.getInsertedPairs()));
    }
}
