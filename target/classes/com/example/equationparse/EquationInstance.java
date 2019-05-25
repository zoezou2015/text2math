package com.example.equationparse;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.example.sp.SemanticForest;

public class EquationInstance extends Instance {

	private static final long serialVersionUID = -1772994277276646146L;
	private Sentence input;
	private SemanticForest output;
	private SemanticForest prediction;
	private ArrayList<Variable> variables;
	String annotated_text;
	int index;
	String equation;
	ArrayList<SemanticForest> identicalOutput = new ArrayList<>();

	public EquationInstance(int instanceId, double weight, Sentence input, SemanticForest tree, String equation,
			String annotated_text, int index) {
		super(instanceId, weight);
		this.input = input;
		this.output = tree;
		this.equation = equation;
		this.annotated_text = annotated_text;
		this.index = index;
	}

	@Override
	public Sentence getInput() {
		return this.input;
	}

	public int getIndex() {
		return this.index;
	}

	public String getEquation() {
		return this.equation;
	}

	public ArrayList<Variable> getVariables() {
		return variables;
	}

	public void setVariables(ArrayList<Variable> variables) {
		this.variables = variables;
	}

	public ArrayList<SemanticForest> getIdenticalOutput() {
		return identicalOutput;
	}

	public void addIdenticalOutput(SemanticForest identical) {
		this.identicalOutput.add(identical);
	}

	public void setIdenticalOutput(ArrayList<SemanticForest> identicalOutput) {
		this.identicalOutput = identicalOutput;
	}

	@Override
	public void removeOutput() {
		this.output = null;
	}

	@Override
	public SemanticForest getOutput() {
		return this.output;
	}

	@Override
	public SemanticForest getPrediction() {
		return this.prediction;
	}

	@Override
	public void removePrediction() {
		this.prediction = null;
	}

	@Override
	public int size() {
		return this.input.length();
	}

	@Override
	public boolean hasOutput() {
		return this.output != null;
	}

	@Override
	public boolean hasPrediction() {
		return this.prediction != null;
	}

	@Override
	public void setPrediction(Object prediction) {
		this.prediction = (SemanticForest) prediction;
	}

	@Override
	public EquationInstance duplicate() {
		EquationInstance instance = new EquationInstance(this._instanceId, this._weight, this.input, output, equation,
				annotated_text, index);
		instance.setPrediction(this.prediction);
		instance.setVariables(this.variables);
		instance.setIdenticalOutput(this.identicalOutput);
		return instance;
	};

}
