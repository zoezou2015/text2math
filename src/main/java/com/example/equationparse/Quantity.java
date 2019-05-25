package com.example.equationparse;

import java.io.Serializable;
import java.util.ArrayList;

public class Quantity implements Serializable {

	private static final long serialVersionUID = -6182273701998083731L;
	private String sentence;
	private int id;
	private ArrayList<Double> quantities;
	private ArrayList<Integer> charOffsetBegins;
	private ArrayList<Integer> charOffsetEnds;
	private ArrayList<Integer> gold;
	private ArrayList<Integer> pred;

	public Quantity(int id, String sentence, ArrayList<Double> quantities, ArrayList<Integer> charOffsetBegins,
			ArrayList<Integer> charOffsetEnds, ArrayList<Integer> gold, ArrayList<Integer> pred) {
		this.id = id;
		this.sentence = sentence;
		this.quantities = quantities;
		this.charOffsetBegins = charOffsetBegins;
		this.charOffsetEnds = charOffsetEnds;
		this.gold = gold;
		this.pred = pred;
	}

	public int getInstanceId() {
		return this.id;
	}

	public String getSentence() {
		return this.sentence;
	}

	public int getGold(int index) {
		return this.gold.get(index);
	}

	public int getPred(int index) {
		return this.pred.get(index);
	}

	public double getValue(int index) {
		return this.quantities.get(index);
	}

	public int getQuantitySize() {
		if (this.pred.size() != this.gold.size())
			throw new RuntimeException("Pred and Gold should have the same size!");
		return this.pred.size();
	}

	public ArrayList<Double> getQuantities() {
		return this.quantities;
	}

	public ArrayList<Integer> getCharOffsetBegins() {
		return this.charOffsetBegins;
	}

	public ArrayList<Integer> getCharOffsetEnds() {
		return this.charOffsetEnds;
	}

}
