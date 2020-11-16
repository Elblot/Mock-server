package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import model.State;
import model.Transition;


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
	 *  Associate all the response with their associated requests 
	 **/
	public void buildResp() {
		for (ResponseT resp : getResponses()) {
			for (RequestT req : resp.getLastRequests()) {
				req.addResponse(resp);
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
