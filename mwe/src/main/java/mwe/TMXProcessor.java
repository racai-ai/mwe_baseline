package mwe;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.HashMap;

public class TMXProcessor extends Thread {

	private String tmx;
	private String src;
	private String dst;
	private TMXBuffer tmxBuffer;
	
	public TMXProcessor(String tmx, String src, String dst,TMXBuffer tmxBuffer) {
		this.tmx=tmx;
		this.src=src;
		this.dst=dst;
		this.tmxBuffer=tmxBuffer;
	}
	
	@Override
	public void run() {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
	
	        // XXE attack, see https://rules.sonarsource.com/java/RSPEC-2755
	        SAXParser saxParser = factory.newSAXParser();
	
	        TMXProcessorHandlerSax handler = new TMXProcessorHandlerSax(src,dst,tmxBuffer);
	
		    saxParser.parse(tmx, handler);
		}catch(Exception ex) {
			ex.printStackTrace();
			tmxBuffer.setDone();
		}
		System.out.println("TMXProcessor: end thread");
	}
	
}

class TMXProcessorHandlerSax extends DefaultHandler {

  private StringBuilder currentValue = new StringBuilder();
  private String currentProp;
  
  public String src=null, dst=null;

  private boolean isTUVsrc, isTUVdst, isProp;
  
  private ArrayList<String> srcList;
  private ArrayList<String> dstList;
  private HashMap<String,String> props;
  private TMXBuffer tmxBuffer;
  
  public TMXProcessorHandlerSax(String src, String dst, TMXBuffer tmxBuffer) {
	  this.src=src;
	  this.dst=dst;
	  srcList=new ArrayList<>(100);
	  dstList=new ArrayList<>(100);
	  props=new HashMap<>(100);
	  this.tmxBuffer=tmxBuffer;
  }

  @Override
  public void startDocument() {;}

  @Override
  public void endDocument() {
	  tmxBuffer.setDone();
	  System.out.println("TMXProcessor: end document");
  }

  @Override
  public void startElement(
          String uri,
          String localName,
          String qName,
          Attributes attributes) {

      // reset the tag value
      currentValue.setLength(0);
      if (qName.equalsIgnoreCase("tu")) {
    	  props.clear();
      }else if (qName.equalsIgnoreCase("tuv")) {
          isTUVsrc=false;
          isTUVdst=false;
    	  String lang=attributes.getValue("xml:lang");
    	  if(lang==null)lang=attributes.getValue("lang");
    	  if(lang==null)lang="";
    	  if(lang.contentEquals(src)) {isTUVsrc=true;}
    	  else if(lang.contentEquals(dst)) {isTUVdst=true;}
      }else if(qName.equalsIgnoreCase("prop")) {
    	  currentProp=attributes.getValue("type");
    	  isProp=true;
      }

  }
  
  private String getCurrentValue() {
	  String str=Jsoup.parse(currentValue.toString()).text();
	  if(str.contains("þ"))return null; // used instead of ț
	  return str;
  }
  
  private String getSegValue(String s) {
	  if(s==null)return null;
	  
	  s=StringUtils.stripStart(s,"- .•");
	  return s;
  }

  @SuppressWarnings("unchecked")
@Override
  public void endElement(String uri,
                         String localName,
                         String qName) {

      if (qName.equalsIgnoreCase("seg")) {
    	  if(isTUVsrc) {
    		  String str=getSegValue(getCurrentValue());
    		  if(str!=null && !str.isEmpty())
    			  srcList.add(str);
    	  }
    	  else if(isTUVdst) {
    		  String str=getSegValue(getCurrentValue());
    		  if(str!=null && !str.isEmpty())    		  
    			  dstList.add(str);
    	  }
          isTUVsrc=false;
          isTUVdst=false;

      }else if(qName.equalsIgnoreCase("tuv")) {
          isTUVsrc=false;
          isTUVdst=false;

      }else if (qName.equalsIgnoreCase("tu")) {
    	  if(srcList.size()>0 && dstList.size()>0) {
    		  // generate pairs
    		  for(String s:srcList) {
				  try {
						tmxBuffer.add(new TMXPair(s,(ArrayList<String>)dstList.clone(), (HashMap<String,String>)props.clone()));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    		  }
    	  }
    	  srcList.clear();
    	  dstList.clear();
    	  props.clear();
      }else if(qName.equalsIgnoreCase("prop")) {
    	  isProp=false;
    	  if(currentProp!=null && !currentProp.isEmpty()) {
    		  if(props.containsKey(currentProp)) {
    			  props.put(currentProp, props.get(currentProp)+" "+currentValue.toString());
    		  }else
    			  props.put(currentProp, currentValue.toString());
    	  }
      }
      
  }

  // http://www.saxproject.org/apidoc/org/xml/sax/ContentHandler.html#characters%28char%5B%5D,%20int,%20int%29
  // SAX parsers may return all contiguous character data in a single chunk,
  // or they may split it into several chunks
  @Override
  public void characters(char ch[], int start, int length) {

      // The characters() method can be called multiple times for a single text node.
      // Some values may missing if assign to a new string

      // avoid doing this
      // value = new String(ch, start, length);

      // better append it, works for single or multiple calls
      if(isTUVsrc || isTUVdst || isProp)currentValue.append(ch, start, length);

  }

}