package com.example.mathsolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class MathLexicon implements Serializable {

	private static final long serialVersionUID = -8980029206686360428L;

	private ArrayList<String> addSignal;
	private ArrayList<String> subSignal;
	private ArrayList<String> mulSignal;
	private ArrayList<String> dviSignal;
	private ArrayList<String> questionSingal;
	private ArrayList<String> textSingal;

	private HashMap<String, String> questionSingal2op;
	private HashMap<String, String> textSingal2op;

	public MathLexicon() {
		this.addSignal = new ArrayList<>();
		this.subSignal = new ArrayList<>();
		this.mulSignal = new ArrayList<>();
		this.dviSignal = new ArrayList<>();

		this.questionSingal = new ArrayList<>();
		this.textSingal = new ArrayList<>();

		this.questionSingal2op = new HashMap<>();
		this.textSingal2op = new HashMap<>();
	}

	public void addSingalWords(boolean isQuestion, String operation, String singal) {
		if (isQuestion) {
			if (operation.equals("add")) {
				this.addSignal.add(singal);
				this.questionSingal.add(singal);
				this.questionSingal2op.put(singal, operation);
			} else if (operation.equals("sub")) {
				this.subSignal.add(singal);
				this.questionSingal.add(singal);
				this.questionSingal2op.put(singal, operation);
			} else if (operation.equals("mul")) {
				this.mulSignal.add(singal);
				this.questionSingal.add(singal);
				this.questionSingal2op.put(singal, operation);
			} else if (operation.equals("dvi")) {
				this.dviSignal.add(singal);
				this.questionSingal.add(singal);
				this.questionSingal2op.put(singal, operation);
			}
		} else {
			if (operation.equals("add")) {
				this.addSignal.add(singal);
				this.textSingal.add(singal);
				this.textSingal2op.put(singal, operation);
			} else if (operation.equals("sub")) {
				this.subSignal.add(singal);
				this.textSingal.add(singal);
				this.textSingal2op.put(singal, operation);
			} else if (operation.equals("mul")) {
				this.mulSignal.add(singal);
				this.textSingal.add(singal);
				this.textSingal2op.put(singal, operation);
			} else if (operation.equals("dvi")) {
				this.dviSignal.add(singal);
				this.textSingal.add(singal);
				this.textSingal2op.put(singal, operation);
			}
		}
	}

	public ArrayList<String> getQuestionSignal() {
		return this.questionSingal;
	}

	public ArrayList<String> getTextQuestionSignal() {
		return this.textSingal;
	}

	public String getOperationBySignal(boolean isQuestion, String signal) {
		String operation = "";
		if (isQuestion) {
			operation = this.questionSingal2op.get(signal);
		} else {
			operation = this.textSingal2op.get(signal);
		}
		if (operation.equals("add"))
			operation = "+";
		else if (operation.equals("sub")) {
			operation = "-";
		} else if (operation.equals("mul")) {
			operation = "*";
		} else if (operation.equals("dvi")) {
			operation = "/";
		}
		return operation;
	}

}
