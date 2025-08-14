package mwe;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;


public class SourceLSTWriter extends Thread {

	private String lst;
	private PrintWriter out;
	private TMXBuffer tmxBuffer;
	
	
	public SourceLSTWriter(String lst, TMXBuffer tmxBuffer) throws NullPointerException, IOException {
		this.lst=lst;
		this.tmxBuffer=tmxBuffer;
		
		this.out=new PrintWriter(
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.lst),Charset.forName("UTF-8")))
		);
	}
	
	@Override
	public void run() {
		while(true) {
			TMXPair p=tmxBuffer.get();
			if(p==null && tmxBuffer.getDone())break;
			
			if(p==null) {
				try {
					Thread.sleep(100);
					System.out.println("SourceLSTWriter: waiting for data");
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
		
		out.close();
		System.out.println("SourceLSTWriter: end thread");
	}
	
	
	public void insertPair(TMXPair p) throws IOException, InterruptedException {
		String s=p.src.replace('\n', ' ').replace('\r', ' ');
		out.println(s);
		out.flush();
		tmxBuffer.addInsertionData(0, 1);
	}
	
}
