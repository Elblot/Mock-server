package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import model.Transition;


/**
 * Represent a state of the LTS
 * @author Elliott Blot
 *
 */
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
	
	/**
	 * Return the label of the state
	 * @return
	 */
	public String getLabel() {
		return label;
	}

	
	/**
	 * Change the label of the state
	 * @param label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Add the transition t in the list of successors
	 * @param t
	 */
	public void addSuccesseur(Transition t){
		if(t.getSource()!=this){
			throw new RuntimeException("transition " + t + " is not a successor of " + this);
		}
		successeurs.add(t);
	}
	
	/**
	 * Add the transition t in the list of predecessors
	 * @param t
	 */
	public void addPredecesseur(Transition t){
		if(t.getTarget()!=this){
			throw new RuntimeException("transition " + t + " is not a predecessor of " + this);
		}
		predecesseurs.add(t);
	}
	
	/**
	 * Remove the transition t to the list of successors
	 * @param t
	 */
	public void removeSuccesseur(Transition t){
		successeurs.remove(t);
	}
	
	/**
	 * Remove the transition t to the list of predecessors
	 * @param t
	 */
	public void removePredecesseur(Transition t){
		predecesseurs.remove(t);
	}
	
	/**
	 * Empty the list of successors
	 */
	public void clearSuccesseurs(){
		successeurs=new LinkedHashSet<Transition>();
	}
	
	/**
	 * Empty the list of predecessors
	 */
	public void clearPredecesseurs(){
		predecesseurs=new LinkedHashSet<Transition>();
	}
	
	/**
	 * Return the list of successors
	 * @return
	 */
	public ArrayList<Transition> getSuccesseurs(){
		return(new ArrayList<Transition>(successeurs));
	}
	
	/**
	 * Return the target states of a transition labelled label, from this state 
	 */
	public ArrayList<State> getSuccesseur(String label) {
		ArrayList<State> res = new ArrayList<State>();
		for (Transition succ : getSuccesseurs()) {
			if (succ.equals(label)) {
				res.add(succ.getTarget());
			}
		}
		return res;
	}
	
	/**
	 * Return the list of predecessors
	 * @return
	 */
	public ArrayList<Transition> getPredecesseurs(){
		return(new ArrayList<Transition>(predecesseurs));
	}
	
	/**
	 * Set the state as initial state
	 */
	public void setInit() {
		init = true;
	}
	
	/**
	 * Return true if the state is the initial state
	 * @return
	 */
	public boolean isInit() {
		return init;
	}
	
	/**
	 * Set the state as final state
	 */
	public void setFinal() {
		finalState = true;
	}
	
	/**
	 * Return true if the state is final
	 * @return
	 */
	public boolean isFinal() {
		return finalState;
	}
	
	/**
	 * Return a printable version of the state
	 */
	public String toString(){
		return label;
	}

	/**
	 * Return the set of requests that can be received from this state.
	 * @return 
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
	 * Return the set of responses that can be sent from this state.
	 * @return 
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
	 * Return the set of requests that can be sent form this state.
	 * @return
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
	 * Return the next request to send (the one with the minimal weight) 
	 *  @return 
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
	 * Return the next received response from this state with the minimal weight. 
	 * @return 
	 */
	public ResponseT getInRespProc() {
		ResponseT res = null;
		int weight = -1;
		for (Transition t: getSuccesseurs()) {
			if (t instanceof ResponseT && t.isInput() && ((ResponseT) t).isProc() && (weight == -1 || weight > t.getWeight())) {
				res = (ResponseT) t;
			}
		}
		return res;
	}
	
	public boolean equals(State s) {
		return label.equals(s.getLabel()); //each state have a different label
	}

	/**
	 * Return the maximal time to wait for a request from this state.
	 * @return 
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
	 * Return the maximal time to wait for the response to be sent.
	 * @return 
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
