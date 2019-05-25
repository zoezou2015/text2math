package com.example.mathsolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class LexiconReader {

	public static void main(String args[]) throws FileNotFoundException {
		String lexiconFile = "data/lexicon.txt";
		read(lexiconFile);
	}

	public static MathLexicon read(String filename) throws FileNotFoundException {
		Scanner scan = new Scanner(new File(filename));
		MathLexicon lexicon = new MathLexicon();

		while (scan.hasNextLine()) {

			String line = scan.nextLine().trim();
			if (line.equals("")) {
				continue;
			}
			if (line.equals("QUESTION")) {
				line = scan.nextLine().trim();
				if (line.startsWith("*ADD*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(true, "add", line.trim());
					}
					line = scan.nextLine();
				}
				if (line.startsWith("*SUB*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(true, "sub", line.trim());
					}
					line = scan.nextLine();
				}
				if (line.startsWith("*MUL*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(true, "mul", line.trim());
					}
					line = scan.nextLine();
				}

				if (line.startsWith("*DVI*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(true, "dvi", line.trim());
					}
				}
			} else if (line.equals("TEXT")) {
				line = scan.nextLine().trim();
				if (line.startsWith("*ADD*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(false, "add", line.trim());
					}
					line = scan.nextLine();
				}
				if (line.startsWith("*SUB*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(false, "sub", line.trim());
					}
					line = scan.nextLine();
				}
				if (line.startsWith("*MUL*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(false, "mul", line.trim());
					}
					line = scan.nextLine();
				}
				if (line.startsWith("*DVI*")) {
					while (!(line = scan.nextLine()).trim().equals(".")) {
						lexicon.addSingalWords(false, "dvi", line.trim());
					}
				}
			} else {
				System.err.println(line);
			}
		}
		// System.err.println(lexicon.getQuestionSignal());
		// System.err.println(lexicon.getTextQuestionSignal());
		scan.close();
		return lexicon;
	}

	public static String checkVaildLexicon(MathLexicon lexicon, ArrayList<String> numbers, String text,
			String questionSentence) {
		if (numbers.size() < 2) {
			return numbers.get(0) + "";
		}
		ArrayList<String> textSignal = lexicon.getTextQuestionSignal();
		ArrayList<String> questionSignal = lexicon.getQuestionSignal();
		boolean isQuestion = false;
		boolean once = false;
		String expression = "";
		if (!text.equals("")) {
			String[] text_token = text.split(" ");
			for (String t : text_token) {
				if (textSignal.indexOf(t.toLowerCase()) != -1) {
					String operation = lexicon.getOperationBySignal(isQuestion, t.toLowerCase());
					expression = numbers.get(0) + " " + operation + " " + numbers.get(1);
					once = true;
					break;
				}
			}
		}
		if (!once && !questionSignal.equals("")) {
			isQuestion = true;
			String[] q_token = questionSentence.split(" ");
			for (String q : q_token) {
				if (questionSignal.indexOf(q.toLowerCase()) != -1) {
					if (q.toLowerCase().equals("more") && text.equals("")) {
						expression = numbers.get(0) + " + " + numbers.get(1);
						return expression;
					}
					String operation = lexicon.getOperationBySignal(isQuestion, q.toLowerCase());
					expression = numbers.get(0) + " " + operation + " " + numbers.get(1);
					break;
				}
			}
		}
		return expression;
	}
}
