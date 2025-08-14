package mwe;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Condition {
	String prop;
	String cond;
	String type;
	String vString;
	float vFloat;
	Pattern vPattern;
	
	public Condition(String prop, String cond, String type, String value) {
		this.prop=prop;
		this.cond=cond;
		this.type=type;
		this.vString=value;
		this.vFloat=0.0f; try {vFloat=Float.parseFloat(value);}catch(Exception e) {;}
		this.vPattern=null;
		try{ this.vPattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE); }catch(Exception e) {;}		
		
		System.out.println(String.format("COND: [%s] [%s] [%s] [%s]",prop,cond,type,value));
	}
	
	public boolean isValid(HashMap<String,String> props) {
		if(props==null || !props.containsKey(this.prop)) return false;
		
		String vs=props.get(this.prop);
		float vf=0.0f;
		try{ vf=Float.parseFloat(vs);}catch(Exception ex) {;}
		
		if(type.equalsIgnoreCase("STRING")) {
			if(this.cond.equalsIgnoreCase("eq")) {
				if(vs.contentEquals(vString))return true;
			}else if(this.cond.equalsIgnoreCase("neq")) {
				if(!vs.contentEquals(vString))return true;
			}else if(this.cond.equalsIgnoreCase("like")) {
				if(vPattern==null)return false;
				 Matcher matcher = vPattern.matcher(vs);
				 if(matcher.find())return true;
			}else if(this.cond.equalsIgnoreCase("nlike")) {
				if(vPattern==null)return false;
				 Matcher matcher = vPattern.matcher(vs);
				 if(!matcher.find())return true;
			}else if(this.cond.equalsIgnoreCase("samedigit")) {
				if(!props.containsKey("SRCTEXT"))return true;
				if(!props.containsKey("DSTTEXT"))return true;
				String srctext=props.get("SRCTEXT");
				String dsttext=props.get("DSTTEXT");
				if(dsttext.length()>0 && Character.isDigit(dsttext.charAt(0)) || srctext.length()>0 && Character.isDigit(srctext.charAt(0))) {
					if(srctext.length()>0 && dsttext.length()>0 && srctext.charAt(0)==dsttext.charAt(0))return true;
				}else return true;
			}
		}else if(type.equalsIgnoreCase("FLOAT")) {
			if(this.cond.equalsIgnoreCase("eq")) {
				if(vf==vFloat)return true;
			}else if(this.cond.equalsIgnoreCase("neq")) {
				if(vf!=vFloat)return true;
			}else if(this.cond.equalsIgnoreCase("gt")) {
				if(vf>vFloat)return true;
			}else if(this.cond.equalsIgnoreCase("gte")) {
				if(vf>=vFloat)return true;
			}else if(this.cond.equalsIgnoreCase("lt")) {
				if(vf<vFloat)return true;
			}else if(this.cond.equalsIgnoreCase("lte")) {
				if(vf<=vFloat)return true;
			}
		}
		
		return false;
		
	}
}
