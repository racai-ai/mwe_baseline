package mwe;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map.Entry;

import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;


public class TMXWriter extends Thread {

	private String tmx;
	private String src_lang;
	private String dst_lang;
	private ConditionsList clist;
	private XMLWriter out;
	private TMXBuffer tmxBuffer;
	
	
	public TMXWriter(String tmx, String src, String dst, ConditionsList clist, TMXBuffer tmxBuffer) throws NullPointerException, IOException {
		this.tmx=tmx;
		this.src_lang=src;
		this.dst_lang=dst;
		this.clist=clist;
		this.tmxBuffer=tmxBuffer;
		
		this.out=new XMLWriterImpl(
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.tmx),Charset.forName("UTF-8"))),
				true
		);
		out.setIndentChars("    ");
		
		out.openElement("tmx",true);
		out.attribute("version", "1.4");
		out.openElement("header",true);
		out.closeElement();
		out.openElement("body",true);
	}
	
	@Override
	public void run() {
		while(true) {
			TMXPair p=tmxBuffer.get();
			if(p==null && tmxBuffer.getDone())break;
			
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
					tmxBuffer.incProcessed();
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
		}
		
		try {
			out.closeElement(); // body
			out.closeElement(); // tmx
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("TMXWriter: end thread");
	}
	
	
	public void insertPair(TMXPair p) throws IOException, InterruptedException {
		
		/*for(Entry<String,String> ent:p.props.entrySet()) {
			System.out.println(ent.getKey()+" => "+ent.getValue());
		}
		System.exit(-1);*/
		
		@SuppressWarnings("unchecked")
		HashMap<String,String> cprops=(HashMap<String,String>)p.props.clone();
		cprops.put("SRCTEXT", p.src);

		//if(!clist.isValid(cprops))return ;
		
		boolean tuWasWritten=false;
		
		//synchronized(lock) {
			
			int ins=0;
			for(String dst:p.dst) {
				cprops.put("DSTTEXT", dst);
				if(clist!=null && !clist.isValid(cprops))continue ;
				
				if(!tuWasWritten) {
					tuWasWritten=true;
					
					out.openElement("tu",true);
					
					for(Entry<String,String> ent:p.props.entrySet()) {
						out.openElement("prop",false);
						out.attribute("type", ent.getKey());
						out.writeText(ent.getValue());
						out.closeElement();
					}
					
					out.openElement("tuv",true);
					out.attribute("xml:lang", src_lang);
					out.openElement("seg",false);
					out.writeText(p.src);
					out.closeElement();
					out.closeElement();
				}
				
				
				out.openElement("tuv",true);
				out.attribute("xml:lang", dst_lang);
				out.openElement("seg",false);
				out.writeText(dst);
				out.closeElement();
				out.closeElement();
				ins++;
			}
			tmxBuffer.addInsertionData(0, ins);
			if(tuWasWritten) {out.closeElement();out.flush();}
		//}
	}
	
}
