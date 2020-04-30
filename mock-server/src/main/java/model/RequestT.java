package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestT extends Transition {

	private String Verb;
	private String Uri;
	private ResponseT resp;
	
	public RequestT(State src, String trans, State dst) {
		name = trans;
		Verb = trans.substring(trans.indexOf("Verb=")+5);
		Verb = Verb.substring(0,Verb.indexOf(separator));
		Uri = trans.substring(trans.indexOf("Uri=")+4);
		Uri = Uri.substring(0,Uri.indexOf(separator));
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
		if (trans.contains("delay=")) {
			String body = trans.substring(trans.indexOf("delay=")+6);
			body = body.substring(0,body.indexOf(separator));
		}
		if (trans.contains("body=")) {
			String w = trans.substring(trans.indexOf("body=")+5);
			w = w.substring(0,w.indexOf(separator));
			delay = Long.parseLong(w);
		}
		else {
			delay = -1;
		}
		headers = new HashMap<String,String>();
		String[] e = trans.substring(trans.indexOf("("), trans.length() - 1).split(separator);
		for (String param: e) {
			if (!param.contains("Verb=") && !param.contains("Host=") && !param.contains("Dest=") &&
				!param.contains("delay=") && !param.contains("repetition=") && !param.contains("weight=") && 
				!param.contains("Uri=") && !param.contains("body=") ) {
				headers.put(param.substring(0, param.indexOf("=")), param.substring(param.indexOf("=") + 1));
			}
		}
		
		source = src;
		target = dst;
	}
	
	public String getVerb() {
		return Verb;
	}
	
	public void setVerb(String v) {
		Verb = v;
	}
	
	public String getUri() {
		return Uri;
	}
	
	public void setUri(String path) {
		Uri = path;
	}

	public String getPath() {
		String res = "http://";
		res = res + to + ":8080";
		res = res + Uri;
		return res;
	}
	
	public ResponseT getResponse() {
		return resp;
	}
	
	public void setResponse(ResponseT r) {
		resp = r;
	}

	public Set<State> getSources(LTS dot) {
		Set<State> res = new HashSet<State>();
		for (Transition t : dot.getTransitions()) {
			if (this.samePath(t)) {
				res.add(t.getSource());
			}
		}
		return res;
	}

	private boolean samePath(Transition t) {
		return (t instanceof RequestT && getUri().equals(((RequestT) t).getUri()));
	}

}
