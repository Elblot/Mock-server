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
		//TODO abstraction
		for (RequestT t : pos.getFutureInReq()) {
			if (t.getPath().contains("**values**")) {
				boolean match = true;
				int prefix = t.getPath().indexOf("**values**");
				int suffix = t.getPath().length() - 1 - t.getPath().indexOf("**values**") - 10;
				if (!t.getRegex().equals("")) {
					if (url.length() > prefix + suffix) {
						String value = url.substring(prefix, url.length() - suffix - 1);
						System.out.println("regex: " + t.getRegex() + "\n"
								+ "value: " + value);		
						match = Pattern.matches(t.getRegex(), value);
					}
				}
				System.out.println("url: " + url + "\n"
						+ "path: " + t.getPath() + "\n"
						+ "prefix:" + prefix + "\n"
						+ "suffix:" + suffix);
				String path1 = t.getPath().replace("\\*\\*values\\*\\*", "");
				if (url.length() > prefix + suffix) {
					String urlprefix = url.substring(0, prefix);
					String urlsuffix = url.substring(url.length() - suffix - 1);
					String path2 = urlprefix + urlsuffix;
					System.out.println("url: " + url + "\n"
							+ "path: " + t.getPath() + "\n"
							+ "prefix:" + urlprefix + "\n"
							+ "suffix:" + urlsuffix +"\n"
							+ "path1: " + t.getPath() + "\n"
							+ "path2: " + t.getPath());
					if (match && path1.equals(path2) && (t.getWeight() < w | w == -1)) {
						res = t;
						w = t.getWeight();
					}
				}				
			}
			else {
				System.out.println("no values found");
				if (t.getPath().equals(url) && (t.getWeight() < w | w == -1)) {
					res = t;
					w = t.getWeight();
				}
			}
		}
		return res;
	}

	/* associate all the response with their associated requests */
	public void buildResp() {
		for (ResponseT resp : getResponses()) {
			//System.out.println(resp + "is a response");
			for (RequestT req : resp.getLastRequests()) {
				req.setResponse(resp);
			}
		}
	}

	public void printReq() {
		for (Transition t : getTransitions()) {
			if (t instanceof RequestT) {
				System.out.println(t + "\n"
						+ "from:" + t.getFrom() + ", to:" + t.getTo()
						+ " resp : " + ((RequestT) t).getResponse());
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
