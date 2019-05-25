package com.example.equationparse;

import java.io.Serializable;

public class Variable implements Serializable {

	int start;
	int end;
	String id;
	String grounding;

	/**
	 * char index
	 * 
	 * @param start:
	 *            inclusive
	 * @param end:
	 *            exclusive
	 * @param variable:
	 *            words from input sentence
	 */
	public Variable(String id, int start, int end, String grounding) {
		this.id = id;
		this.start = start;
		this.end = end;
		this.grounding = grounding;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGrounding() {
		return grounding;
	}

	public void setGrounding(String grounding) {
		this.grounding = grounding;
	}

	@Override
	public String toString() {
		return "Var: " + this.id + "; start: " + this.start + "; end: " + this.end + "; grounding: " + this.grounding;
	}
}
