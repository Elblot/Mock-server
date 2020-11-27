package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.Map;
import java.util.Set;

/**
 * Represent a request 
 * @author Elliott Blot
 *
 */
public class RequestT extends Transition {

	private String Verb;
	private String Uri;
	private HashSet<ResponseT> resp;

	/**
	 * Parse the label of the request 
	 * @param src, the state source
	 * @param trans, the label of the transition
	 * @param dst, the state target
	 */
	@SuppressWarnings("unchecked")
	public RequestT(State src, String trans, State dst) {
		resp = new HashSet<ResponseT>();
		name = trans;
		Verb = trans.substring(trans.indexOf("Verb=")+5);
		if (Verb.contains(separator)) {
			Verb = Verb.substring(0,Verb.indexOf(separator));
		}
		else {
			Verb = Verb.substring(0,Verb.indexOf(")"));
		}
		//Build Uri
		Uri = trans.substring(trans.indexOf("Uri=")+4);
		if (Uri.contains(separator)) {
			Uri = Uri.substring(0,Uri.indexOf(separator));
		}
		else {
			Uri = Uri.substring(0,Uri.indexOf(")"));
		}
		//Build the sender
		from = trans.substring(trans.indexOf("Host=")+5);
		if (from.contains(separator)) {
			from = from.substring(0,from.indexOf(separator));
		}
		else {
			from = from.substring(0,from.indexOf(")"));
		}
		//Build the receiver 
		to = trans.substring(trans.indexOf("Dest=")+5);
		if (to.contains(separator)) {
			to = to.substring(0,to.indexOf(separator));
		}
		else {
			to = to.substring(0,to.indexOf(")"));
		}
		//Build the weight
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
		//Build the repetition
		if (trans.contains("repetition=")) {
			String w = trans.substring(trans.indexOf("repetition=")+11);
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
		//Build parameters used by functions replacing **values** in outputs
		if (trans.contains("start=") && trans.contains("fun=")) {
			String st = trans.substring(trans.indexOf("start=")+6);
			if (st.contains(separator)) {
				st = st.substring(0,st.indexOf(separator));
			}
			else {
				st = st.substring(0,st.indexOf(")"));
			}
			String[] sts = st.split(",");
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
			String st = trans.substring(trans.indexOf("step=")+5);
			if (st.contains(separator)) {
				st = st.substring(0,st.indexOf(separator));
			}
			else {
				st = st.substring(0,st.indexOf(")"));
			}
			String[] sts = st.split(",");
			step = new ArrayList<Double>();
			for (String s: sts) {
				step.add(Double.parseDouble(s));
			}
		}		
		//Build the body
		if (trans.contains("body=")) {
			String bod = trans.substring(trans.indexOf("body=")+5);
			if (bod.contains(separator)) {
				body = bod.substring(0,bod.indexOf(separator));
			}
			else {
				body = bod.substring(0,bod.indexOf(")"));
			}
		}
		//Build the regex used for abstraction of **values** in input
		if (trans.contains("regex=")) {
			String reg = trans.substring(trans.indexOf("regex=")+6);
			if (reg.contains(separator)) {
				reg = reg.substring(0,reg.indexOf(separator));
			}
			else {
				reg = reg.substring(0,reg.indexOf(")"));
			}
			regex = new ArrayList<String>();
			for (String r: reg.split(",")) {
				if (r.contains("\\")){
					regex.add(r.replaceAll("\\\\", "\\\\\\\\"));
				}
				else {
					regex.add(r);
				}
			}
		}
		//Build the delay
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
					!param.contains("fun=") && !param.contains("regex=") && !param.contains("step=")) {
				headers.put(param.substring(0, param.indexOf("=")), param.substring(param.indexOf("=") + 1));
			}
		}

		source = src;
		target = dst;
	}

	public RequestT(RequestT t, String newuri) {
		source = t.getSource();
		target = t.getTarget();
		Verb = t.getVerb();
		resp = new HashSet<ResponseT>();
		for (ResponseT r: t.getResponses()) {
			addResponse(r);
			r.addRequest(this);
		}	
		//resp = t.getResponses();
		if (t.isInput()) {
			name = "?" + newuri;
		}
		else if (t.isOutput()) {
			name = "!" + newuri;
		}
		else {
			name = newuri;    //t.getName();
		}
		from = t.getFrom();
		to = t.getTo();
		weight = t.getWeight();
		repetition = t.getRepetition();
		delay = t.getDelay();
		headers = t.getHeaders();
		Uri = newuri;
		body = t.getBody();
		fun = t.getFun();
		start = t.getStart();
		step = t.getStep();
		values = t.getStart();
		regex = t.getRegex();
	}
	
	/**
	 * Return the Verb
	 */
	public String getVerb() {
		return Verb;
	}

	/**
	 * Change the Verb
	 * @param v
	 */
	public void setVerb(String v) {
		Verb = v;
	}

	/**
	 * Build the Uri, and replace the **values** if present
	 * @return
	 */
	public String getUri() {
		if (isOutput() && Uri.contains("**values**")) {
			ApplyLaw();
			String Uri2 = Uri;
			for (int i = 0; i < values.size(); ++i) {
				Uri2 = Uri2.replaceFirst("\\*\\*values\\*\\*", Double.toString(values.get(i)));
			}
			return Uri2;
		}
		else {
			return Uri;
		}
	}

	/**
	 * Change the Uri
	 * @param path
	 */
	public void setUri(String path) {
		Uri = path;
	}

	/**
	 * Return the parameters in the uri.
	 */
	public Set<String> getParam(){
		Set<String> res = new HashSet<String>();
		String st;
		if (!Uri.contains("?")) {
			return res;
		}
		st = Uri.substring(Uri.indexOf("?") + 1);
		while (st.contains("=")) {
			String p = st.substring(st.indexOf("=")+1);
			if (p.contains("&")) {
				st = st.substring(st.indexOf("&")+1);
				p = p.substring(0, p.indexOf("&"));
				res.add(p);
			}
			else {
				res.add(p);
				return res;
			}			
		}		
		return res;
	}
	
	/**
	 * Get the complete path of the request
	 * @return
	 */
	public String getPath() {
		String res = "http://";
		res = res + to; // + ":8080";
		res = res + getUri();
		return res;
	}

	/**
	 * Return the shorten Uri (without the parameters).
	 * @return 
	 */
	public String getPathReq() {
		if (Uri.contains("?")) {
			return Uri.substring(0,Uri.indexOf("?"));
		}
		return Uri;
	}

	/**
	 * Return the waited response associated.
	 * @return 
	 */
	public HashSet<ResponseT> getResponses() {
		return resp;
	}

	/**
	 * Return the waited response that can be received in the delay d
	 */
	public HashSet<ResponseT> getResponsesDelay(long d){
		HashSet<ResponseT> res = new HashSet<ResponseT>();
		for (ResponseT resp: getResponses()) {
			if ((resp.getDelay() == 0) || resp.getDelay() >= d) {
				res.add(resp);
			}
		}
		
		return res;
	}
	
	/**
	 * Add a response to the request
	 * @param r
	 */
	public void addResponse(ResponseT r) {
		resp.add(r);
		//r.addRequest(this);
	}
	
	/**
	 * remove a response to resp
	 */
	public void removeResponse(ResponseT r) {
		if (resp.contains(r)) {
			resp.remove(r);
		}
	}

	/**
	 * Return the response with the minimal weight
	 * @return
	 */
	public ResponseT getMinResponse() {
		int w = -1;
		ResponseT res = null;
		for (ResponseT r: resp) {
			if (w == -1 || w > r.getWeight()) {
				res = r;
				w = r.getWeight();
			}
		}
		res.incWeight();
		return res;
	}

	/**
	 * Return the set of state that have the request as future.
	 * @param dot the lts
	 * @return 
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
