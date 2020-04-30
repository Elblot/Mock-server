package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ResponseT extends Transition {

	
	private int status;
	private String content;
	
	public ResponseT(State src, String trans, State dst) {
		name = trans;
		body = trans.substring(trans.indexOf("body=")+5);
		body = body.substring(0,body.indexOf(separator));
		String st = trans.substring(trans.indexOf("status=")+7);
		st = st.substring(0,st.indexOf(separator));
		status = Integer.parseInt(st);
		from = trans.substring(trans.indexOf("Host=")+5);
		from = from.substring(0,from.indexOf(separator));
		to = trans.substring(trans.indexOf("Dest=")+5);
		to = to.substring(0,to.indexOf(separator));
		if (trans.contains("weight=")) {
			String w = trans.substring(trans.indexOf("weight=")+7);
			w = w.substring(0,w.indexOf(separator));
			weight = Integer.parseInt(w);
		}
		else {
			weight = 0;			
		}
		if (trans.contains("repetition=")) {
			String w = trans.substring(trans.indexOf("reptition=")+10);
			w = w.substring(0,w.indexOf(separator));
			repetition = Integer.parseInt(w);
		}
		else {
			repetition = 1;			
		}
		if (trans.contains("Content-Type=")) {
			String w = trans.substring(trans.indexOf("Content-Type=")+13);
			w = w.substring(0,w.indexOf(separator));
			content = w;
		}
		else {
			content = null;			
		}
		if (trans.contains("delay=")) {
			String w = trans.substring(trans.indexOf("delay=")+6);
			w = w.substring(0,w.indexOf(separator));
			delay = Long.parseLong(w);
		}
		else {
			delay = -1;
		}	
		headers = new HashMap<String,String>();
		String[] e = trans.substring(trans.indexOf("("), trans.length() - 1).split(separator);
		for (String param: e) {
			if (!param.contains("Host=") && !param.contains("Dest=") &&
				!param.contains("delay=") && !param.contains("repetition=") && !param.contains("weight=") && 
				!param.contains("status=") && !param.contains("body=") ) {
				headers.put(param.substring(0, param.indexOf("=")), param.substring(param.indexOf("=") + 1));
			}
		}
		source = src;
		target = dst;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String v) {
		body = v;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int st) {
		status = st;
	}

	public String getContent() {
		return content;
	}
	
	public Set<RequestT> getLastRequests(){
		return getLastRequests(this);		
	}
	
}
