package mwe;

import java.util.HashMap;
import java.util.List;

public class TMXPair {

	public String src;
	public List<String> dst;
	public HashMap<String,String> props;
	
	public TMXPair(String src, List<String> dst, HashMap<String,String> props) {
		this.src=src;
		this.dst=dst;
		this.props=props;
	}
	
}
