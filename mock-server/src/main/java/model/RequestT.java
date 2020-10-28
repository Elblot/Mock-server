package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RequestT extends Transition {

	private String Verb;
	private String Uri;
	private ResponseT resp;

	public RequestT(State src, String trans, State dst) {
		name = trans;
		Verb = trans.substring(trans.indexOf("Verb=")+5);
		if (Verb.contains(separator)) {
			Verb = Verb.substring(0,Verb.indexOf(separator));
		}
		else {
			Verb = Verb.substring(0,Verb.indexOf(")"));
		}
		Uri = trans.substring(trans.indexOf("Uri=")+4);
		if (Uri.contains(separator)) {
			Uri = Uri.substring(0,Uri.indexOf(separator));
		}
		else {
			Uri = Uri.substring(0,Uri.indexOf(")"));
		}
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
		if (trans.contains("start=") && trans.contains("fun=")) {
			String st = trans.substring(trans.indexOf("start=")+6);
			st = st.substring(0,st.indexOf(separator));
			start = Double.parseDouble(st);
			fun = trans.substring(trans.indexOf("fun=")+4);
			if (fun.contains(separator)) {
				fun = fun.substring(0,fun.indexOf(separator));
			}
			else {
				fun = fun.substring(0,fun.indexOf(")"));
			}
		}
		if (trans.contains("body=")) {
			String bod = trans.substring(trans.indexOf("body=")+5);
			if (bod.contains(separator)) {
				body = bod.substring(0,bod.indexOf(separator));
			}
			else {
				body = bod.substring(0,bod.indexOf(")"));
			}
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
		headers = new HashMap<String,String>();
		String[] e = trans.substring(trans.indexOf("("), trans.length() - 1).split(separator);
		for (String param: e) {
			if (!param.contains("Verb=") && !param.contains("Host=") && !param.contains("Dest=") &&
					!param.contains("delay=") && !param.contains("repetition=") && !param.contains("weight=") && 
					!param.contains("Uri=") && !param.contains("body=") && !param.contains("start=") && 
					!param.contains("law=") && !param.contains("regex=")) {
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
		if (Uri.contains("**values**")) {
			String Uri2 = Uri.replaceAll("\\*\\*values\\*\\*", Double.toString(start));
			ApplyLaw();
			return Uri2;
		}
		else {
			return Uri;
		}
	}

	public void setUri(String path) {
		Uri = path;
	}


	public String getPath() {
		String res = "http://";
		res = res + to + ":8080";
		res = res + getUri();
		return res;
	}

	/**
	 * @return the shorten Uri (without the parameters).
	 */
	public String getPathReq() {
		if (Uri.contains("?")) {
			return Uri.substring(0,Uri.indexOf("?"));
		}
		return Uri;
	}

	/**
	 * @return the waited response associated.
	 */
	public ResponseT getResponse() {
		return resp;
	}

	public void setResponse(ResponseT r) {
		resp = r;
	}

	/**
	 * @param dot the lts
	 * @return the set of state that have the request as future.
	 */
	public Set<State> getSources(LTS dot) {
		Set<State> res = new HashSet<State>();
		for (Transition t : dot.getTransitions()) {
			if (this.samePath(t)) {
				res.add(t.getSource());
			}
		}
		return res;
	}

	/**
	 * Check if the Transition t and this have the same Uri.
	 */
	private boolean samePath(Transition t) {
		return (t instanceof RequestT && getUri().equals(((RequestT) t).getUri()));
	}


}
