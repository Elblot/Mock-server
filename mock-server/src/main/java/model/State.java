package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import model.Transition;

public class State {

	private Set<Transition> successeurs;
	private Set<Transition> predecesseurs;
	private String label;
	
	private boolean init;
	private boolean finalState;
	
	public State() {
		successeurs = new LinkedHashSet<Transition>();
		predecesseurs = new LinkedHashSet<Transition>();
		init = false;
	}
	
	public State(State name){
		label = name.getLabel();
		successeurs = new HashSet<Transition>(name.getSuccesseurs());
		predecesseurs = new HashSet<Transition>(name.getPredecesseurs());
		init = name.isInit();
	}
	
	public State(String name){
		label = name;
		successeurs = new LinkedHashSet<Transition>();
		predecesseurs = new LinkedHashSet<Transition>();
		init = false;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void addSuccesseur(Transition t){
		if(t.getSource()!=this){
			throw new RuntimeException("transition " + t + " is not a successor of " + this);
		}
		successeurs.add(t);
	}
	
	public void addPredecesseur(Transition t){
		if(t.getTarget()!=this){
			throw new RuntimeException("transition " + t + " is not a predecessor of " + this);
		}
		predecesseurs.add(t);
	}
	
	public void removeSuccesseur(Transition t){
		successeurs.remove(t);
	}
	
	public void removePredecesseur(Transition t){
		predecesseurs.remove(t);
	}
	
	public void clearSuccesseurs(){
		successeurs=new LinkedHashSet<Transition>();
	}
	
	public void clearPredecesseurs(){
		predecesseurs=new LinkedHashSet<Transition>();
	}
	
	public ArrayList<Transition> getSuccesseurs(){
		return(new ArrayList<Transition>(successeurs));
	}
	
	/* return the target state of a transition labelled label form this state */
	public ArrayList<State> getSuccesseur(String label) {
		ArrayList<State> res = new ArrayList<State>();
		for (Transition succ : getSuccesseurs()) {
			if (succ.equals(label)) {
				res.add(succ.getTarget());
			}
		}
		return res;
	}
	
	public ArrayList<Transition> getPredecesseurs(){
		return(new ArrayList<Transition>(predecesseurs));
	}
	
	public void setInit() {
		init = true;
	}
	
	public boolean isInit() {
		return init;
	}
	
	public void setFinal() {
		finalState = true;
	}
	
	public boolean isFinal() {
		return finalState;
	}
	
	public String toString(){
		return label;
	}

	/**
	 * @return the set of requests that can be received from this state.
	 */
	public Set<RequestT> getFutureInReq() {
		Set<RequestT> res = new HashSet<RequestT>();
		for (Transition succ : getSuccesseurs()) {
			if (succ.isInput() && succ instanceof RequestT) {
				res.add((RequestT) succ);
			}
		}
		return res;
	}
	
	/**
	 * @return the set of responses that can be sent from this state.
	 */
	public Set<ResponseT> getFutureOutResp() {
		Set<ResponseT> res = new HashSet<ResponseT>();
		for (Transition succ : getSuccesseurs()) {
			if (succ.isOutput() && succ instanceof ResponseT) {
				res.add((ResponseT) succ);
			}
		}
		return res;
	}
	
	/**
	 * @return the set of requests that can be sent form this state.
	 */
	public Set<RequestT> getFutureOutReq() {
		Set<RequestT> res = new HashSet<RequestT>();
		for (Transition succ : getSuccesseurs()) {
			if (succ.isOutput() && succ instanceof RequestT) {
				res.add((RequestT) succ);
			}
		}
		return res;
	}
	
	/**
	 *  @return the next request to send (the one with the minimal weight) 
	 */
	public RequestT getOutReq(long lastAction) {
		//LoggerFactory.getLogger("MOCK").info(String.format(this.toString()));
		RequestT res = null;
		int weight = -1;
		for (Transition succ : getSuccesseurs()) {
			if (succ.isOutput() && succ instanceof RequestT && (weight < 0 | succ.getWeight() < weight) && ((System.currentTimeMillis() - lastAction) >= succ.getDelay())) {
				res = (RequestT) succ;
				weight = res.getWeight();
			}
		}
		return res;
	}

	/**
	 * @return the next received response from this state with the minimal weight. 
	 */
	public ResponseT getInResp() {
		ResponseT res = null;
		int weight = -1;
		for (Transition t: getSuccesseurs()) {
			if (t instanceof ResponseT && t.isInput() && (weight == -1 || weight > t.getWeight())) {
				res = (ResponseT) t;
			}
		}
		return res;
	}
	
	public boolean equals(State s) {
		return label.equals(s.getLabel()); //each state have a different label
	}

	/**
	 * @return the maximal time to wait for a request from this state.
	 */
	public long getMaxDelay() {
		long d = -1;
		for (RequestT t : getFutureInReq()) {
			if (t.getDelay() > d) {
				d = t.getDelay();
			}
		}
		return d;
	}

	/**
	 * @return the maximal time to wait for the response to be sent.
	 */
	public long plannedResponse() {
		long d = -1;
		for (ResponseT t : getFutureOutResp()) {
			if (t.getDelay() > d) {
				d = t.getDelay();
			}
		}
		return d;
	}
}
