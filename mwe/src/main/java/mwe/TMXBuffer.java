package mwe;

import java.util.ArrayList;

public class TMXBuffer {

	public static final int MAX_SIZE=10000;
	
	private final Object lock=new Object();
	
	private ArrayList<TMXPair> list=new ArrayList<>(MAX_SIZE+1);
	
	private int numProcessed=0;
	
	private boolean done=false;
	
	public int numTUVSrc=0;
	
	private int duplicates;
	private int insertedPairs;
	
	public void add(TMXPair pair) throws InterruptedException {
		while(true) {
			synchronized(lock) {
				if(list.size()<MAX_SIZE) {
					list.add(pair);
					break;
				}
			}
			Thread.sleep(100);
		}
	}
	
	public TMXPair get() {
		synchronized(lock) {
			if(list.size()==0)return null;
			TMXPair p=list.get(0);
			list.remove(0);
			return p;
		}
	}
	
	public void incProcessed() {
		synchronized(lock) {numProcessed++;}
	}
	
	public int getProcessed() {
		synchronized(lock) {return numProcessed;}
	}
	
	public boolean isListEmpty() {
		synchronized(lock) {return list.size()==0;}
	}
	
	public void setDone() {
		synchronized(lock) {done=true;}
	}
	
	public boolean getDone() {
		synchronized(lock) {return done;}
	}
	
	public void addInsertionData(int dup, int ins) {
		synchronized(lock) {
			duplicates+=dup;
			insertedPairs+=ins;
		}
	}
	
	public int getDuplicates() {
		synchronized(lock) {return duplicates;}
	}

	public int getInsertedPairs() {
		synchronized(lock) {return insertedPairs;}
	}

}
