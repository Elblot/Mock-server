package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
//import java.lang.Math;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public abstract class Transition {

	protected String name;
	//private String label;
	//private String[] parameters;


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

	protected String regex = "";

	public String getName() {
		return name;
	}

	public void setName(String newname) {
		name = newname;
	}

	public String getFrom() {
		//System.out.println(from);
		return from;
	}

	public void setFrom(String newFrom) {
		from = newFrom;
	}

	public String getTo() {
		//System.out.println(to);
		return to;
	}

	public void setTo(String newTo) {
		to = newTo;
	}

	public State getSource() {
		return source;
	}

	public void setSource(State newsource) {
		source = newsource;
	}

	public State getTarget() {
		return target;
	}

	public void setTarget(State newtarget) {
		target = newtarget;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int i) {
		weight = i;
	}

	public void incWeight() {
		weight++;
	}

	public int getRepetition() {
		return repetition;
	}

	public void setRepetition(int r) {
		repetition = r;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long d) {
		delay = d;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}

	public String getBody() {
		return body;
	}


	public boolean isInput() {
		if (name.startsWith("?")){
			return true;
		}
		return false;
	}

	public boolean isOutput() {
		if (name.startsWith("!")){
			return true;
		}
		return false;
	}

	public boolean equals(String t) {
		return name.equals(t);
	}

	public String toString() {
		return name;				
	}

	public String getRegex() {
		return regex;
	}

	/**
	 * @param resp
	 * @return the set of request that have resp for response in the model.
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
