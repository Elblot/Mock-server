package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import model.State;
import model.Transition;

public class LTS {

	/* initial state */
	private State initialState;

	/* Transitions list */
	private HashMap<String, Transition> transitions;

	/* Set of states */
	private HashMap<String, State> states;


	public LTS() {
		transitions = new HashMap<String, Transition>();
		states = new HashMap<String, State>(); 
	}

	public void addTransitions(ArrayList<Transition> tr){
		for(Transition t:tr){
			addTransition(t);
		}
	}

	public void addTransition(Transition transition) {
		String key = transition.getSource().toString() + transition.getName() + transition.getTarget().toString();
		transitions.put(key, transition);
	}

	public void addStates(ArrayList<State> st){
		for(State s:st){
			addState(s);
		}
	}

	public void addState(State state) {
		states.put(state.getLabel(), state);
	}

	public void setStates(ArrayList<State> new_states) {
		states = new HashMap<String, State>();
		for(State s:new_states){
			addState(s);
		}
	}

	public State getState(String st) {
		return states.get(st);
	}

	public State getInitialState() {
		return this.initialState;
	}

	public Set<Transition> getTransitions() {
		Set<Transition> list = new HashSet<Transition>();
		list.addAll(transitions.values());
		return(list);
	}	

	public Set<ResponseT> getResponses(){
		Set<ResponseT> list = new HashSet<ResponseT>();
		for (Transition t : transitions.values()) {
			if (t instanceof ResponseT){
				list.add((ResponseT) t);
			}
		}
		return list;
	}

	public Set<State> getStates() {
		Set<State> list = new HashSet<State>();
		list.addAll(states.values());
		return(list);		
	}

	public void setInitialState(State new_initial_state) {
		new_initial_state.setInit();
		this.initialState = new_initial_state;
	}

	public String toString() {
		String lts = this.getStates().size() + " states: ";
		lts = lts + this.getStates().toString();
		lts = lts + "\n" + transitions.size() + " transitions: ";
		lts = lts + this.getTransitions().toString();
		return lts;		
	}

	public Set<String> getInputRequests(){
		Set<String> reqs = new HashSet<String>();
		for (Transition t: transitions.values()) {
			if (t.isInput() && t instanceof RequestT){
				reqs.add(((RequestT) t).getPathReq());
			}
		}
		return reqs;
	}


	public RequestT getReq(String url, State pos) {
		RequestT res = null;
		int w = -1;
		//abstraction
		for (RequestT t : pos.getFutureInReq()) {
			if (t.getPath().contains("**values**")){ 	
				String r = "^(" + cleanReg(t.getPath());
				System.out.println(r);
				System.out.println(url);
				for (int i = 0; i < t.getRegex().size(); ++i) {
					r = r.replaceFirst("\\*\\*values\\*\\*", ")" + t.getRegex().get(i) + "(");
				}
				r = r + ")$";
				if (Pattern.matches(r, url) && (t.getWeight() < w | w == -1)) {
					res = t;
					w = t.getWeight();
				}
			}
			else {
				if (t.getPath().equals(url) && (t.getWeight() < w | w == -1)){
					res = t;
					w = t.getWeight();
				}
			}


			/*while (path1.contains("**values**")) { //todo uilt a regex ^(prefix reg1 next reg2 suffix)$
				if (i >= t.getRegex().size()) {
					System.err.println("Some regex are missing, we found more varaible **values** than regex.");
				}
				int prefix = path1.indexOf("**values**");
				int suffix = path1.length() - 1 - path1.indexOf("**values**") - 10;
				if (path2.length() > prefix + suffix) {
					String value = path2.substring(prefix, path2.length() - suffix - 1);	
					match = Pattern.matches(t.getRegex().get(i), value);
				}
				path1 = path1.replaceFirst("\\*\\*values\\*\\*", "");
				if (path2.length() > prefix + suffix) {
					String urlprefix = path2.substring(0, prefix);
					String urlsuffix = path2.substring(path2.length() - suffix - 1);
					path2 = urlprefix + urlsuffix;
					if (!match){ // && path1.equals(path2) && (t.getWeight() < w | w == -1))) {
						break;
						//res = t;
						//w = t.getWeight();
					}
				}	
				++i;
			}

			//			if (!t.getPath().contains("**values**")){
			if (match && path1.equals(path2) && (t.getWeight() < w | w == -1)) {
				res = t;
				w = t.getWeight();
			}
			//			}*/
		}
		return res;
	}

	private static String cleanReg(String reg) {
		String res = reg;
		res = res.replaceAll("\\&", "\\\\&");
		res = res.replaceAll("\\(", "\\\\(");
		res = res.replaceAll("\\)", "\\\\)");
		res = res.replaceAll("\\[", "\\\\[");
		res = res.replaceAll("\\]", "\\\\]");
		res = res.replaceAll("\\.", "\\\\.");
		res = res.replaceAll("\\?", "\\\\?");		
		res = res.replaceAll("\\{", "\\\\{");
		res = res.replaceAll("\\}", "\\\\}");
		//res = res.replaceAll("\\*", "\\\\*"); block the detection of **values**
		res = res.replaceAll("\\|", "\\\\|");
		res = res.replaceAll("\\+", "\\\\+");
		res = res.replaceAll("\\:", "\\:");
		res = res.replaceAll("\\$", "\\\\$");
		res = res.replaceAll("\\^", "\\\\^");
		return res;
	}
	
	/* associate all the response with their associated requests */
	public void buildResp() {
		for (ResponseT resp : getResponses()) {
			//System.out.println(resp + "is a response");
			for (RequestT req : resp.getLastRequests()) {
				req.addResponse(resp);
			}
		}
	}

	public void printReq() {
		for (Transition t : getTransitions()) {
			if (t instanceof RequestT) {
				System.out.println(t + "\n"
						+ "from:" + t.getFrom() + ", to:" + t.getTo()
						+ " resp : " + ((RequestT) t).getResponses());
			}
		}
	}

	// for debug purpose, to remove
	public ResponseT getRandResp() {
		for (ResponseT t : getResponses()) {
			return t;
		}
		return null;

	}

	public State getRandSt() {
		for (State s : getStates()) {
			return s;
		}
		return null;
	}

}
