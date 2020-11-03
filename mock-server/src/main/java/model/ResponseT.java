package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class ResponseT extends Transition {

	
	private int status;
	private String content;
	private boolean proc;
	
	@SuppressWarnings("unchecked")
	public ResponseT(State src, String trans, State dst) {
		proc = false;
		name = trans;
		body = trans.substring(trans.indexOf("body=")+5);
		if (body.contains(separator)) {
			body = body.substring(0,body.indexOf(separator));
		}
		else {
			body = body.substring(0,body.indexOf(")"));
		}
		String st = trans.substring(trans.indexOf("status=")+7);
		if (st.contains(separator)) {
			st = st.substring(0,st.indexOf(separator));
		}
		else {
			st = st.substring(0,st.indexOf(")"));
		}
		status = Integer.parseInt(st);
		from = trans.substring(trans.indexOf("Host=")+5);
		if (from.contains(separator)) {
			from = from.substring(0,from.indexOf(separator));
		}
		else {
			from = from.substring(0,from.indexOf(")"));
		}
		to = trans.substring(trans.indexOf("Dest=")+5);
		if (to.contains(separator)) {
			to = to.substring(0,to.indexOf(separator));
		}
		else {
			to = to.substring(0,to.indexOf(")"));
		}
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
		if (trans.contains("regex=")) {
			String reg = trans.substring(trans.indexOf("regex=")+6);
			if (reg.contains(separator)) {
				regex = reg.substring(0,reg.indexOf(separator));
			}
			else {
				regex = reg.substring(0,reg.indexOf(")"));
			}
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
		if (trans.contains("start=") && trans.contains("fun=")) {
			String sta = trans.substring(trans.indexOf("start=")+6);
			if (sta.contains(separator)) {
				sta = sta.substring(0,sta.indexOf(separator));
			}
			else {
				sta = sta.substring(0,sta.indexOf(")"));
			}
			String[] sts = sta.split(",");
			start = new ArrayList<Double>();
			for (String s: sts) {
				start.add(Double.parseDouble(s));
			}
			values = (ArrayList<Double>) start.clone();
			String f = trans.substring(trans.indexOf("fun=")+4);
			if (f.contains(separator)) {
				f = f.substring(0,f.indexOf(separator));
			}
			else {
				f = f.substring(0,f.indexOf(")"));
			}
			fun = new ArrayList<String>();
			for (String s : f.split(",")) {
				fun.add(s);
			}
		}
		if (trans.contains("step=")){
			String ste = trans.substring(trans.indexOf("step=")+5);
			if (ste.contains(separator)) {
				ste = ste.substring(0,ste.indexOf(separator));
			}
			else {
				ste = ste.substring(0,ste.indexOf(")"));
			}
			String[] sts = ste.split(",");
			step = new ArrayList<Double>();
			for (String s: sts) {
				step.add(Double.parseDouble(s));
			}
		}		
		headers = new HashMap<String,String>();
		String[] e = trans.substring(trans.indexOf("("), trans.length() - 1).split(separator);
		for (String param: e) {
			if (!param.contains("Host=") && !param.contains("Dest=") &&
				!param.contains("delay=") && !param.contains("repetition=") && !param.contains("weight=") && 
				!param.contains("status=") && !param.contains("body=") && !param.contains("start=") &&
				!param.contains("law=") && !param.contains("regex=") && !param.contains("step=")) {
				headers.put(param.substring(0, param.indexOf("=")), param.substring(param.indexOf("=") + 1));
			}
		}
		source = src;
		target = dst;
	}
	
	public String getBody() {
		if (isOutput() && body.contains("**values**")) {
			ApplyLaw();
			String body2 = body;
			for (int i =0; i < values.size(); ++i) {
				body2 = body2.replaceFirst("\\*\\*values\\*\\*", Double.toString(values.get(i)));
			}
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
	
	public boolean isProc() {
		return proc;
	}
	
	public void setProc(boolean b) {
		proc = b;
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
