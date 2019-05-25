package com.example.mathsolver;

import java.util.ArrayList;

import com.example.equationparse.Variable;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.example.sp.SemanticForest;

public class MathInstance extends Instance {

	private static final long serialVersionUID = 1L;

	private Sentence input;
	private SemanticForest output;
	private SemanticForest prediction;
	private ArrayList<Variable> variables;
	String problemtext;
	int index;
	String formated_equation;
	String orig_equation;
	double solution;
	String text;
	String questionSentence;
	ArrayList<SemanticForest> identicalOutput = new ArrayList<>();
	private ArrayList<String> numbers = new ArrayList<>();

	public MathInstance(int instanceId, double weight, Sentence input, SemanticForest tree, String formated_equation,
			String orig_equation, double solution, int index) {
		super(instanceId, weight);
		this.input = input;
		this.output = tree;
		this.formated_equation = formated_equation;
		this.orig_equation = orig_equation;
		this.solution = solution;
		this.index = index;
	}

	@Override
	public Sentence getInput() {
		return this.input;
	}

	public int getIndex() {
		return this.index;
	}

	public String getFormatedEquation() {
		return this.formated_equation;
	}

	public String getOriginalEquation() {
		return this.orig_equation;
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

	public void addNumbers(String number) {
		this.numbers.add(number);
	}

	public void setNumbers(ArrayList<String> numbers) {
		this.numbers.addAll(numbers);
	}

	public ArrayList<String> getNumberList() {
		return this.numbers;
	}

	public void addIdenticalOutput(SemanticForest identical) {
		this.identicalOutput.add(identical);
	}

	public void setIdenticalOutput(ArrayList<SemanticForest> identicalOutput) {
		this.identicalOutput = identicalOutput;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setQuestionSentence(String questionSentence) {
		this.questionSentence = questionSentence;
	}

	public String getQuestionSentence() {
		return this.questionSentence;
	}

	public String getText() {
		return this.text;
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

	public double getSolution() {
		return this.solution;
	}

	@Override
	public void setPrediction(Object prediction) {
		this.prediction = (SemanticForest) prediction;
	}

	@Override
	public MathInstance duplicate() {
		MathInstance instance = new MathInstance(this._instanceId, this._weight, this.input, output, formated_equation,
				orig_equation, solution, index);
		instance.setPrediction(this.prediction);
		instance.setVariables(this.variables);
		instance.setIdenticalOutput(this.identicalOutput);
		instance.setNumbers(this.numbers);
		instance.setQuestionSentence(this.questionSentence);
		instance.setText(this.text);
		return instance;
	};

}
