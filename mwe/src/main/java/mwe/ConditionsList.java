package mwe;

import java.util.ArrayList;
import java.util.HashMap;

public class ConditionsList {

	private ArrayList<Condition> conditions;
	
	public ConditionsList(String cString) {
		conditions=new ArrayList<>(100);
		
		String[] data=cString.split("[|]");
		for(String s:data) {
			String[] cdata=s.split("[:]");
			conditions.add(new Condition(cdata[0],cdata[1],cdata[2],cdata[3]));
		}
	}
	
	public boolean isValid(HashMap<String,String> props) {
		for(Condition c:conditions) {
			if(!c.isValid(props))return false;
		}
		return true;
	}
	
}
