package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;


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
	
	public Set<RequestT> getInputRequests(){
		Set<RequestT> reqs = new HashSet<RequestT>();
		for (Transition t: transitions.values()) {
			if (t.isInput() && t instanceof RequestT){
				reqs.add((RequestT) t);
			}
		}
		return reqs;
	}
	
	/* associate all the response with their associated requests */
	public void buildResp() {
		for (ResponseT resp : getResponses()) {
			for (RequestT req : resp.getLastRequests()) {
				req.setResponse(resp);
			}
		}
	}
	
}
