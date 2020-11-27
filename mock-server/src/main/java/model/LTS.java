package model;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

//import model.State;
//import model.Transition;


/**
 * @author Elliott Blot
 *
 *represent the model representing the behaviour the mock has to follow.
 */
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

	/**
	 * Add the list of transitions tr to the the LTS
	 * @param tr
	 */
	public void addTransitions(ArrayList<Transition> tr){
		for(Transition t:tr){
			addTransition(t);
		}
	}

	/**
	 * Add a transition to the LTS
	 * @param transition
	 */
	public void addTransition(Transition transition) {
		String key = transition.getSource().toString() + transition.getName() + transition.getTarget().toString();
		transitions.put(key, transition);
		transition.getSource().addSuccesseur(transition);
		transition.getTarget().addPredecesseur(transition);		
	}

	/**
	 * Remove a transition of the LTS
	 */
	public void removeTransition(Transition t) {
		if (transitions.containsValue(t)) {
			transitions.remove(t.getSource().toString() + t.getName() + t.getTarget().toString());
			t.getSource().removeSuccesseur(t);
			t.getTarget().removePredecesseur(t);	
			if (t instanceof RequestT) {
				for (ResponseT r: ((RequestT) t).getResponses()) {
					r.removeRequest((RequestT) t);
				}
			}
			else if (t instanceof ResponseT) {
				for (RequestT r: ((ResponseT) t).getRequests()) {
					r.removeResponse((ResponseT) t);
				}
			}
		}
	}

	/**
	 * Add the list of states st to the LTS
	 * @param st
	 */
	public void addStates(ArrayList<State> st){
		for(State s:st){
			addState(s);
		}
	}


	/**
	 * Modify the transition of the model to produce a dos to the targets
	 */
	public void makeDos() {
		for (RequestT t: getOutputRequests()) {
			String generatedString = rdmWord(1000);
			String newuri = t.getPathReq() + "?" + generatedString;
			t.setUri(newuri);
			t.setRepetition(10000);
			t.setDelay(0);
		}
	}


	/**
	 * Modify the transitions of the LTS to assess the robustness of other components
	 */
	public void makeRobustness() {
		// TODO Auto-generated method stub
		for (RequestT t: getOutputRequests()) {
			//System.out.println("t:" + t);
			for (String p: t.getParam()) {
				for (String rob: getRobustnessString(p)) {
					String newuri = t.getUri().replace(p, rob);
					RequestT x = new RequestT(t, newuri);
					//System.out.println(getTransitions());
					addTransition(x);
					//System.out.println(getTransitions());
				}
			}
			removeTransition(t);
		}
		for (ResponseT t: getOutputResponses()) {
			for (String p: t.getParam()) {
				for (String rob: getRobustnessString(p)) {
					String newbody = t.getBody().replace(p, rob);
					ResponseT x = new ResponseT(t, newbody);
					addTransition(x);
					System.out.println("tr: " + x);
				}
			}
			removeTransition(t);
		}
	}
	
	/**
	 * Return list of strings that can be include in parameters 
	 */
	public Set<String> getRobustnessString(String p){
		Set<String> res = new HashSet<String>();
		// put replacement here
		res.add("xoxo");
		res.add(rdmString(8));
		res.add(rdmNumber(8));
		res.add(rdmWord(8));
		res.add(rdmWordDec(8));
		res.add(rdmWordP(8));
		//injection of char
		int index = p.length() / 2;
		System.out.println("p: " + p);
		System.out.println("index: " + index);
		res.add(p.substring(index) + rdmNumber(1) + p.substring(index + 1, p.length() - 1));
		res.add(p.substring(index) + rdmWord(1) + p.substring(index + 1, p.length() - 1));
		res.add(p.substring(index) + rdmString(1) + p.substring(index + 1, p.length() - 1));
		
		return res;
	}
	
	/**
	 * Modify the transition of the model to produce XSS attacks
	 */
	public void makeXSS() {
		//System.out.println("modification du lts");
		for (RequestT t: getOutputRequests()) {
			//System.out.println("t:" + t);
			for (String p: t.getParam()) {
				for (String XSS: getXSSString()) {
					String newuri = t.getUri().replace(p, XSS);
					RequestT x = new RequestT(t, newuri);
					//System.out.println(getTransitions());
					addTransition(x);
					//System.out.println(getTransitions());
				}
			}
			removeTransition(t);
		}
	}

	/**
	 * Return list of strings that can be include in parameters 
	 */
	public Set<String> getXSSString(){
		Set<String> res = new HashSet<String>();
		// put XSS script here
		res.add("<script>alert(1);</script>");
		res.add("%3Cscript%3Ealert(1);%3C/script%3E");
		res.add("%3Cscript%3Ealert(1)%3B%3C%2Fscript%3E");
		res.add("%3Cscript%3Ealert%281%29%3B%3C%2Fscript%3E");	
		return res;
	}
	
	/*
	 * Return a random string composed of any ascii character
	 */
	private static String rdmString(int size) {
		byte[] array = new byte[size]; // length is bounded by 7
	    new Random().nextBytes(array);
	    String generatedString = new String(array, Charset.forName("UTF-8"));
	    return generatedString;
	}
	
	/*
	 * Return a random stirng composed of numbers
	 */
	private static String rdmNumber(int size) {
		String chars = "0123456789";	
		Random random = new Random();	 
		String res = "";
		for (int i = 0; i < size; ++i) {
			res = res + chars.charAt(random.nextInt(chars.length()));
		}
		System.out.println(res);
		return res;
	}
	
	/*
	 * Return a random string composed of letters and numbers characters
	 */
	private static String rdmWordDec(int size) {
		String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";	
		Random random = new Random();	 
		String res = "";
		for (int i = 0; i < size; ++i) {
			res = res + chars.charAt(random.nextInt(chars.length()));
		}
		System.out.println(res);
		return res;
	}

	/*
	 * Return a random string composed of letters
	 */
	private static String rdmWord(int size) {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";	
		Random random = new Random();	 
		String res = "";
		for (int i = 0; i < size; ++i) {
			res = res + chars.charAt(random.nextInt(chars.length()));
		}
		System.out.println(res);
		return res;
	}
	
	/*
	 * Return a random string composed of letters, numbers and ponctuations
	 */
	private static String rdmWordP(int size) {
		String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,;:!=)_-('\"&?./\\{[}]";	
		Random random = new Random();	 
		String res = "";
		for (int i = 0; i < size; ++i) {
			res = res + chars.charAt(random.nextInt(chars.length()));
		}
		System.out.println(res);
		return res;
	}
	

	/**
	 * Add a state to the LTS 
	 * @param state
	 */
	public void addState(State state) {
		states.put(state.getLabel(), state);
	}

	/**
	 * Initialize the states to new_states 
	 * @param new_states
	 */
	public void setStates(ArrayList<State> new_states) {
		states = new HashMap<String, State>();
		for(State s:new_states){
			addState(s);
		}
	}

	/**
	 * Return the set of states
	 * @param st
	 * @return
	 */
	public State getState(String st) {
		return states.get(st);
	}

	/**
	 * Return the initial state
	 * @return
	 */
	public State getInitialState() {
		return this.initialState;
	}

	/**
	 * Return the list of transitions
	 * @return
	 */
	public Set<Transition> getTransitions() {
		Set<Transition> list = new HashSet<Transition>();
		list.addAll(transitions.values());
		return(list);
	}	

	/**
	 * Return the list of response in the LTS
	 * @return
	 */
	public Set<ResponseT> getResponses(){
		Set<ResponseT> list = new HashSet<ResponseT>();
		for (Transition t : transitions.values()) {
			if (t instanceof ResponseT){
				list.add((ResponseT) t);
			}
		}
		return list;
	}

	/**
	 * Return teh set of state of the LTS 
	 * @return
	 */
	public Set<State> getStates() {
		Set<State> list = new HashSet<State>();
		list.addAll(states.values());
		return(list);		
	}

	/**
	 * Set the initial state to new_initial_state
	 * @param new_initial_state
	 */
	public void setInitialState(State new_initial_state) {
		new_initial_state.setInit();
		this.initialState = new_initial_state;
	}

	/**
	 * Return a printable version of the LTS
	 */
	public String toString() {
		String lts = this.getStates().size() + " states: ";
		lts = lts + this.getStates().toString();
		lts = lts + "\n" + transitions.size() + " transitions: ";
		lts = lts + this.getTransitions().toString();
		return lts;		
	}

	/**
	 * Return the response that can be sent by the mock
	 */
	public Set<ResponseT> getOutputResponses(){
		Set<ResponseT> res = new HashSet<ResponseT>();
		for (Transition t: transitions.values()) {
			if (t.isOutput() && t instanceof ResponseT){
				res.add(((ResponseT) t));
			}
		}
		return res;
	}
	
	/**
	 * Return the requests that can be sent by the mock
	 */
	public Set<RequestT> getOutputRequests(){
		Set<RequestT> res = new HashSet<RequestT>();
		for (Transition t: transitions.values()) {
			if (t.isOutput() && t instanceof RequestT){
				res.add(((RequestT) t));
			}
		}
		return res;
	}

	/**
	 * Return the requests that can be received by the mock, in strings
	 * @return
	 */
	public Set<String> getInputRequests(){
		Set<String> reqs = new HashSet<String>();
		for (Transition t: transitions.values()) {
			if (t.isInput() && t instanceof RequestT){
				reqs.add(((RequestT) t).getPathReq());
			}
		}
		return reqs;
	}


	/**
	 * Get the next request that should be received by the mock.
	 * @param url
	 * @param pos
	 * @return
	 */
	public RequestT getReq(String url, State pos) {
		RequestT res = null;
		int w = -1;
		//abstraction
		for (RequestT t : pos.getFutureInReq()) {
			if (t.getPath().contains("**values**")){ 	
				String r = "^(" + cleanReg(t.getPath());
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
		}
		return res;
	}

	/**
	 * Clean the string to keep special characters in th regex.
	 * @param reg
	 * @return
	 */
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

	/**
	 *  Associate all the response with their associated requests and vice versa
	 **/
	public void buildResp() {
		for (ResponseT resp : getResponses()) {
			for (RequestT req : resp.getLastRequests()) {
				req.addResponse(resp);
				resp.addRequest(req);
			}
		}
	}

	/**
	 * Print the request of the LTS, and their associated responses
	 */
	public void printReq() {
		for (Transition t : getTransitions()) {
			if (t instanceof RequestT) {
				System.out.println(t + "\n"
						+ "from:" + t.getFrom() + ", to:" + t.getTo()
						+ " resp : " + ((RequestT) t).getResponses());
			}
		}
	}



}
