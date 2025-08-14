package mwe;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;


public class CSVWriter extends Thread {

	private String csv;
	private com.opencsv.CSVWriter out;
	private TMXBuffer tmxBuffer;
	
	
	public CSVWriter(String csv, TMXBuffer tmxBuffer) throws NullPointerException, IOException {
		this.csv=csv;
		this.tmxBuffer=tmxBuffer;
		
		this.out=new com.opencsv.CSVWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.csv),Charset.forName("UTF-8"))));
		
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
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("CSVWriter: end thread");
	}
	
	
	public void insertPair(TMXPair p) throws IOException, InterruptedException {
		
		if(p.dst==null || p.dst.isEmpty()) {
			out.writeNext(new String[] {p.src});
		}else {
			for(String dst:p.dst) {
				out.writeNext(new String[] {p.src,dst});
			}
		}
		out.flush();
		tmxBuffer.addInsertionData(0, 1);
	}
	
}
