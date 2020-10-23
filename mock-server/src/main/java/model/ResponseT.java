package model;

import java.util.HashMap;
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
			if (w.contains(separator)) {
				w = w.substring(0,w.indexOf(separator));
			}
			else {
				w = w.substring(0,w.indexOf(")"));
			}
			weight = Integer.parseInt(w);
		}
		else {
			weight = 0;			
		}
		if (trans.contains("repetition=")) {
			String w = trans.substring(trans.indexOf("reptition=")+10);
			if (w.contains(separator)) {
				w = w.substring(0,w.indexOf(separator));
			}
			else {
				w = w.substring(0,w.indexOf(")"));
			}
			repetition = Integer.parseInt(w);
		}
		else {
			repetition = 1;			
		}
		if (trans.contains("Content-Type=")) {
			String w = trans.substring(trans.indexOf("Content-Type=")+13);
			if (w.contains(separator)) {
				w = w.substring(0,w.indexOf(separator));
			}
			else {
				w = w.substring(0,w.indexOf(")"));
			}
			content = w;
		}
		else {
			content = null;			
		}
		if (trans.contains("delay=")) {
			String w = trans.substring(trans.indexOf("delay=")+6);
			if (w.contains(separator)) {
				w = w.substring(0,w.indexOf(separator));
			}
			else {
				w = w.substring(0,w.indexOf(")"));
			}
			delay = Long.parseLong(w);
		}
		else {
			delay = 0;
		}	
		if (trans.contains("start=") && trans.contains("law=")) {
			String sta = trans.substring(trans.indexOf("start=")+6);
			sta = sta.substring(0,sta.indexOf(separator));
			start = Double.parseDouble(sta);
			law = trans.substring(trans.indexOf("law=")+4);
			if (law.contains(separator)) {
				law = law.substring(0,law.indexOf(separator));
			}
			else {
				law = law.substring(0,law.indexOf(")"));
			}
		}
		headers = new HashMap<String,String>();
		String[] e = trans.substring(trans.indexOf("("), trans.length() - 1).split(separator);
		for (String param: e) {
			if (!param.contains("Host=") && !param.contains("Dest=") &&
				!param.contains("delay=") && !param.contains("repetition=") && !param.contains("weight=") && 
				!param.contains("status=") && !param.contains("body=") && !param.contains("start=") &&
				!param.contains("law=")) {
				headers.put(param.substring(0, param.indexOf("=")), param.substring(param.indexOf("=") + 1));
			}
		}
		source = src;
		target = dst;
	}
	
	public String getBody() {
		if (body.contains("**values**")) {
			String body2 = body.replaceAll("\\*\\*values\\*\\*", Double.toString(start));
			ApplyLaw();
			return body2;
		}
		else {
			return body;
		}
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
