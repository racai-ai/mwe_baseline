package mwe;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import com.github.pvf2005.genericaimodelclient.GenericAIModelClient;
import com.github.pvf2005.genericaimodelclient.impl.Request;
import com.github.pvf2005.genericaimodelclient.impl.Response;


public class MWEExtractorSimple extends Thread {

	private TMXBuffer in, out;
	private GenericAIModelClient client;
	private String temperature;
	private String reasoning_effort;
	private String template_user; //="Extract a list of multiword expressions from the following sentence. If there is no multiword expression, write NONE. Do not include named entities. Do not include explanations. Separate the list of identified multiword expressions with comma.\n```%s```";
	private String template_system; //="You are a linguist that identifies multiword expressions.";
		
	public MWEExtractorSimple(TMXBuffer in, TMXBuffer out,  
			GenericAIModelClient client, String temperature,
			String templateSystem, String templateUser,
			String reasoning_effort
	) throws NullPointerException, IOException {
		this.in=in;
		this.out=out;
		this.client=client;
		this.temperature=temperature;
		this.template_user=templateUser;
		this.template_system=templateSystem;
		this.reasoning_effort=reasoning_effort;
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
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
		}
		
		out.setDone();
		
	}
		
	public void insertPair(TMXPair p) throws IOException, InterruptedException {
        Request req=new Request();
        req.addMessage("system", this.template_system);
        req.addMessage(String.format(this.template_user,p.src));
        if(this.temperature!=null)req.setTemperature(this.temperature);
        if(this.reasoning_effort!=null)req.setReasoningEffort(this.reasoning_effort);
		
        Response r=client.chat(req);
        String data="";
        if(r.isValid()) {
        	data=r.getMessage().getContent();        
		
			String check="the final answer is:";
			int pos=data.toLowerCase().indexOf(check);
			if(pos>=0)data=data.substring(pos+check.length()).strip();
	
			check="corrected response:";
			pos=data.toLowerCase().indexOf(check);
			if(pos>=0)data=data.substring(pos+check.length()).strip();
			
			check="(i replaced this with)";
			pos=data.toLowerCase().indexOf(check);
			if(pos>=0)data=data.substring(pos+check.length()).strip();
	
			check="the answer should be:";
			pos=data.toLowerCase().indexOf(check);
			if(pos>=0)data=data.substring(pos+check.length()).strip();
			
			check="corrected response is:";
			pos=data.toLowerCase().indexOf(check);
			if(pos>=0)data=data.substring(pos+check.length()).strip();
	
			check="revised response:";
			pos=data.toLowerCase().indexOf(check);
			if(pos>=0)data=data.substring(pos+check.length()).strip();
        }
        
		@SuppressWarnings("unchecked")
		TMXPair p1=new TMXPair(p.src,Collections.singletonList(data),(HashMap<String,String>)p.props.clone());
		out.add(p1);
		
	}
	
}
