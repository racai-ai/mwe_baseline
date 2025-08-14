package mwe;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class ProcessMonitor extends Thread {

	private TMXBuffer tmxBuffer;
	
	public ProcessMonitor(TMXBuffer tmxBuffer) {
		this.tmxBuffer=tmxBuffer;
	}
	
	@Override
	public void run() {
		ProgressBar pb = new ProgressBar("Insertion Progress", 100, ProgressBarStyle.ASCII);
		pb.start();
		while(true) {
			if(tmxBuffer.getDone() && tmxBuffer.isListEmpty() || tmxBuffer.numTUVSrc<=0)break;
			
			int n=tmxBuffer.getProcessed();
			int prog=n*100/tmxBuffer.numTUVSrc;
			pb.stepTo(prog);
			pb.setExtraMessage(String.format("%d / %d (INS:%d DUP:%d)", n, tmxBuffer.numTUVSrc,tmxBuffer.getInsertedPairs(),tmxBuffer.getDuplicates()));
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		
		System.out.println("processMonitor: end thread");
		
		pb.stepTo(100);
		pb.stop();
	}
	
}
