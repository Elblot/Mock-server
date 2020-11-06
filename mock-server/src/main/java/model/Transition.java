package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
//import java.lang.Math;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public abstract class Transition {

	protected String name;

	protected String from;
	protected String to;

	protected int weight;
	protected int repetition;
	protected long delay; // in milliseconds

	protected Map<String, String> headers; 
	protected String body;

	protected State source;
	protected State target;

	protected String separator = "@";

	protected int index;
	protected ArrayList<String> fun;
	protected ArrayList<Double> start; 
	protected ArrayList<Double> step;
	protected ArrayList<Double> values;

	protected ArrayList<String> regex;

	/**
	 * Return the label of the transition
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Change the label of the transition
	 * @param newname
	 */
	public void setName(String newname) {
		name = newname;
	}

	/**
	 * Return the sender of the message
	 * @return
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Change the sender of the message
	 * @param newFrom
	 */
	public void setFrom(String newFrom) {
		from = newFrom;
	}

	/**
	 * Return the receiver of the message
	 * @return
	 */
	public String getTo() {
		//System.out.println(to);
		return to;
	}

	/**
	 * Change the receiver of the message
	 * @param newTo
	 */
	public void setTo(String newTo) {
		to = newTo;
	}

	/**
	 * Return the source state of the transition
	 * @return
	 */
	public State getSource() {
		return source;
	}

	/**
	 * Change the source of the transition
	 * @param newsource
	 */
	public void setSource(State newsource) {
		source = newsource;
	}

	/**
	 * Return the target of the transition
	 * @return
	 */
	public State getTarget() {
		return target;
	}
	
	/**
	 * Change the target of the transition
	 * @param newtarget
	 */
	public void setTarget(State newtarget) {
		target = newtarget;
	}

	/**
	 * Return the weight of the transition
	 * @return
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * Change the weight of the transition
	 * @param i
	 */
	public void setWeight(int i) {
		weight = i;
	}

	/**
	 * Increment the weight of the transition
	 */
	public void incWeight() {
		weight++;
	}

	/**
	 * return the number of repetition of the transition
	 * @return
	 */
	public int getRepetition() {
		return repetition;
	}

	/**
	 * Change the number of repetition of the transition
	 * @param r
	 */
	public void setRepetition(int r) {
		repetition = r;
	}

	/**
	 * Return the delay before firing the transition
	 * @return
	 */
	public long getDelay() {
		return delay;
	}

	/** 
	 * Change the delay before firing the transition
	 * @param d
	 */
	public void setDelay(long d) {
		delay = d;
	}

	/**
	 * Return the headers of the messages
	 * @return
	 */
	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}
	
	/**
	 * Return the body of the message
	 * @return
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Return true if the message is an input of the mock
	 * @return
	 */
	public boolean isInput() {
		if (name.startsWith("?")){
			return true;
		}
		return false;
	}

	/**
	 * Return true if the message is an output of the mock
	 * @return
	 */
	public boolean isOutput() {
		if (name.startsWith("!")){
			return true;
		}
		return false;
	}

	/**
	 * Return true if the nameof transition is equal to t
	 * @param t
	 * @return
	 */
	public boolean equals(String t) {
		return name.equals(t);
	}
	
	/**
	 * Return a printable version of the transition
	 */
	public String toString() {
		return name;				
	}

	/**
	 * Return the list of regex used for the abstraction of **values** 
	 * in the input messages.
	 * @return
	 */
	public ArrayList<String> getRegex() {
		return regex;
	}

	/**
	 * Return  the set of request that have resp for response in the model.
	 * @param resp
	 * @return
	 */
	public Set<RequestT> getLastRequests(ResponseT resp){
		Set<RequestT> set = new HashSet<RequestT>();
		for (Transition t : source.getPredecesseurs()) {
			if (t instanceof RequestT && t.getFrom().equals(resp.getTo()) && t.getTo().equals(resp.getFrom())){
				set.add((RequestT) t); 
			}
			else if (t == this) {
				System.err.println("Warning: response " + this.toString() + " can be sent without request");
				continue;
			}
			else {
				set.addAll(t.getLastRequests(resp));
			}
		}
		if (set.isEmpty()) {
			System.err.println("error: no req found for " + resp );
		}
		return set;
	}

	/**
	 * Apply the function in fun, to update the values that replace the 
	 * **values** in output message.
	 */
	protected void ApplyLaw() {
		if (start.size() != step.size() && start.size() != fun.size()) {
			System.err.println("start, step and fun have to have the same size.");
			return;
		}
		for (int i = 0; i < start.size(); ++i) {
			Expression e = new ExpressionBuilder(fun.get(i))
					.variables("x")
					.build()
					.setVariable("x", start.get(i));
			values.set(i, e.evaluate());
		}
		start.set(index, start.get(index) + step.get(index));
		index++;
		if (index == values.size()) {
			index = 0;
		}
	}

}
