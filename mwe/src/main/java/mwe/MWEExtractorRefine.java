package mwe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;


public class MWEExtractorRefine extends Thread {

	private TMXBuffer in, out;
	private OllamaAPI ollamaAPI;
	private String ollamaTranslationModel;
	private float temperature;
	private String template_user; 
	private String template_system; 
	private String template_refine; 
		
	public MWEExtractorRefine(TMXBuffer in, TMXBuffer out,  
			OllamaAPI ollamaAPI, String ollamaTranslationModel, float temperature,
			String templateSystem, String templateUser, String templateRefine
	) throws NullPointerException, IOException {
		this.in=in;
		this.out=out;
		this.ollamaAPI=ollamaAPI;
		this.ollamaTranslationModel=ollamaTranslationModel;
		this.temperature=temperature;
		this.template_system=templateSystem;
		this.template_user=templateUser;
		this.template_refine=templateRefine;
	}
	
	@Override
	public void run() {
		while(true) {
			TMXPair p=in.get();
			if(p==null && in.getDone())break;
			
			if(p==null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}else {
				try {
					insertPair(p);
				} catch (IOException | InterruptedException | OllamaBaseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
		}
		
		out.setDone();
		
	}
		
	public void insertPair(TMXPair p) throws IOException, InterruptedException, OllamaBaseException {
		List<OllamaChatMessage> messages=new ArrayList<>(100);

		HashMap<String, Boolean> mwes=MWEScoring.getMWE(p.dst.get(0));
		if(mwes.size()==0) {
			@SuppressWarnings("unchecked")
			TMXPair p1=new TMXPair(p.src,Collections.singletonList("NONE"),(HashMap<String,String>)p.props.clone());
			out.add(p1);			
			return ;
		}
		

		ArrayList<String> mwesNew=new ArrayList<>(mwes.size());
		
		for(String mwe:mwes.keySet()) {
			OllamaChatMessage m=new OllamaChatMessage();
			m.setRole(OllamaChatMessageRole.SYSTEM);
			m.setContent(this.template_system);
			messages.add(m);
			
			m=new OllamaChatMessage();
	        m.setRole(OllamaChatMessageRole.USER);
			m.setContent(String.format(this.template_user,p.src));
			messages.add(m);
			
			m=new OllamaChatMessage();
	        m.setRole(OllamaChatMessageRole.ASSISTANT);
			m.setContent(p.dst.get(0));
			messages.add(m);
	
			m=new OllamaChatMessage();
	        m.setRole(OllamaChatMessageRole.USER);
			m.setContent(String.format(this.template_refine,p.src,mwe));
			messages.add(m);

			Options ollamaOptions=new OptionsBuilder().setTemperature(temperature).build();
			
			OllamaChatRequest req=OllamaChatRequestBuilder.getInstance(ollamaTranslationModel).withOptions(ollamaOptions).withMessages(messages).build();
			OllamaChatResult response=ollamaAPI.chat(req);
			String data=response.getChatHistory().get(response.getChatHistory().size()-1).getContent();
			
			if(data.contains("YES"))mwesNew.add(mwe);
			
		}
		String data="NONE";
		if(mwesNew.size()>0)data=String.join("|", mwesNew);
		
		@SuppressWarnings("unchecked")
		TMXPair p1=new TMXPair(p.src,Collections.singletonList(data),(HashMap<String,String>)p.props.clone());
		out.add(p1);
		
	}
	
}
