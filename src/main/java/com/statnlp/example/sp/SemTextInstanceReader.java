/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.statnlp.example.sp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.example.equationparse.EquationInstance;
import com.example.equationparse.EquationParserConfig;
import com.example.equationparse.Quantity;
import com.example.equationparse.Variable;
import com.example.mathsolver.LexiconReader;
import com.example.mathsolver.MathInstance;
import com.example.mathsolver.MathLexicon;
import com.example.mathsolver.MathSolverConfig;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.udojava.evalex.Expression;

/**
 * @author wei_lu
 *
 */
public class SemTextInstanceReader {

	public static double PRIOR_WEIGHT = 100.0;

	// public static ArrayList<SemanticUnit> _allUnits;

	private static int _maxHeight = -1;

	// public static void main(String args[])
	// throws IOException, ScriptException, ParserConfigurationException,
	// SAXException {
	// int foldNum = 5;
	// String fold_path = MathSolverConfig.dataDir + "commoncore/";
	// String file = MathSolverConfig.dataDir + "commoncore/questions.json";
	// ArrayList<ArrayList<Integer>> folds = ReadFile.extractFolds(fold_path,
	// foldNum, ".txt");
	// SemTextDataManager dm = new SemTextDataManager();
	// ArrayList<Integer> train_ids = new ArrayList<>();
	// ArrayList<Integer> test_ids = new ArrayList<>();
	// for (int i = 0; i < 5; i++) {
	// if (i == 0) {
	// test_ids.addAll(folds.get(i));
	// } else {
	// train_ids.addAll(folds.get(i));
	// }
	// }
	//
	// MathInstance[] train_instances =
	// SemTextInstanceReader.MathSolverReader(train_ids, file, dm, true);
	// }

	public static ArrayList<SemTextInstance> readPrior(int startId, String filename, SemTextDataManager dm)
			throws IOException {

		double priorWeight = 0;// 0.01;

		double max_entry = Double.NEGATIVE_INFINITY;
		double min_entry = Double.POSITIVE_INFINITY;

		int id = startId;
		ArrayList<SemTextInstance> instances = new ArrayList<>();

		File f = new File(filename);
		if (!f.exists()) {
			return instances;
		}

		BufferedReader scan = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		String line;
		while ((line = scan.readLine()) != null) {
			int bIndex = line.indexOf("[");
			int eIndex = line.indexOf("]");
			String word = line.substring(0, bIndex).trim();
			WordToken wordToken = new WordToken(word);
			String[] vecStr = line.substring(bIndex + 1, eIndex).split("\\s");
			double[] vecs = new double[vecStr.length];
			for (int k = 0; k < vecs.length; k++) {
				vecs[k] = Double.parseDouble(vecStr[k]);
				if (min_entry > vecs[k]) {
					min_entry = vecs[k];
				}
				if (max_entry < vecs[k]) {
					max_entry = vecs[k];
				}
				// vecs[k] = priorWeight * Math.exp(vecs[k]);
				vecs[k] = priorWeight * vecs[k];
				SemTextPriorInstance inst = new SemTextPriorInstance(id++, vecs[k], k, wordToken);
				instances.add(inst);
				// System.err.println(vecs[k]);
			}
		}
		scan.close();
		// System.err.println(instances.size()+" instances.");
		// System.err.println("max_entry="+max_entry);
		// System.err.println("min_entry="+min_entry);
		// System.exit(1);
		return instances;

	}

	public static ArrayList<SemTextInstance> read(String filename, SemTextDataManager dm, String ids_filename,
			boolean isTrain) throws IOException {
		// Scanner scan = new Scanner(new File(ids_filename));
		BufferedReader scan = new BufferedReader(new InputStreamReader(new FileInputStream(ids_filename), "UTF8"));
		ArrayList<Integer> ids = new ArrayList<>();
		String line;
		while ((line = scan.readLine()) != null) {
			int id = Integer.parseInt(line);
			ids.add(id);
		}
		// while(scan.hasNextLine()){
		// String line = scan.nextLine();
		// int id = Integer.parseInt(line);
		// ids.add(id);
		// }
		scan.close();

		ArrayList<SemTextInstance> instances = new ArrayList<>();

		scan = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		// scan = new Scanner(new File(filename));
		while ((line = scan.readLine()) != null) {

			if (line.startsWith("id:")) {
				int index = line.indexOf(":");
				int id = Integer.parseInt(line.substring(index + 1).trim());
				if (!ids.contains(id)) {
					continue;
				}
				String[] words = scan.readLine().substring(3).trim().split("\\s");
				for (String word : words) {
					if (word.equals(""))
						System.out.println("the word is whitespace!" + id);
				}
				WordToken[] wTokens = new WordToken[words.length];
				for (int k = 0; k < words.length; k++) {
					wTokens[k] = new WordToken(words[k]);
				}
				Sentence sent = new Sentence(wTokens);
				String mrl = scan.readLine().substring(4).trim();
				scan.readLine();
				ArrayList<String> prods_form = new ArrayList<>();
				while ((line = scan.readLine()).startsWith("*"))
					prods_form.add(line);
				SemanticForest tree = toTree(id, prods_form, dm);
				SemTextInstance inst = new SemTextInstance(id >= 0 ? id + 1 : id, id >= 0 ? 1.0 : 100.0, sent, tree,
						mrl);
				instances.add(inst);

				int height = tree.getHeight();
				if (_maxHeight < height) {
					_maxHeight = height;
				}

				if (id >= 0) {
					String sentence = sent.toString();
					ArrayList<SemanticForestNode> nodes = tree.getAllNodes();
					for (int k = 0; k < nodes.size(); k++) {
						if (nodes.get(k).isRoot())
							continue;
						SemanticUnit unit = nodes.get(k).getUnit();
						if (unit.isContextIndependent()) {
							boolean found_unit_phrase = false;
							ArrayList<String> phrases = dm.getPriorUnitToPhrases(unit);
							ArrayList<String> tmp = new ArrayList<>();
							for (String s : sentence.split(" ")) {
								tmp.add(s);
							}

							for (String phrase : phrases) {
								if (phrase.split(" ").length > 1 && sentence.indexOf(phrase) != -1
										|| tmp.contains(phrase)) {
									found_unit_phrase = true;
								}
							}
							if (!found_unit_phrase) {
								System.err.println("did not find the phrase in instance " + id);
								System.err.println("sentence: " + sentence + ", unit: " + unit);
								// System.exit(1);
							} else {
								// System.err.println(sentence);
								// System.err.println(phrases.toString());
							}
						}
					}
				}

			}
		}
		// while(scan.hasNextLine()){
		// String line = scan.nextLine();
		//
		// if(line.startsWith("id:")){
		// int index = line.indexOf(":");
		// int id = Integer.parseInt(line.substring(index+1).trim());
		// if(!ids.contains(id)){
		// continue;
		// }
		// String[] words = scan.nextLine().substring(3).trim().split("\\s");
		// WordToken[] wTokens = new WordToken[words.length];
		// for(int k = 0; k<words.length; k++){
		// wTokens[k] = new WordToken(words[k]);
		// }
		// Sentence sent = new Sentence(wTokens);
		// String mrl = scan.nextLine().substring(4).trim();
		// scan.nextLine();
		// ArrayList<String> prods_form = new ArrayList<String>();
		// while((line=scan.nextLine()).startsWith("*"))
		// prods_form.add(line);
		// SemanticForest tree = toTree(id, prods_form, dm);
		// SemTextInstance inst = new SemTextInstance(id>=0? id+1 : id, id>=0? 1.0 :
		// 100.0, sent, tree, mrl);
		// instances.add(inst);
		//
		// int height = tree.getHeight();
		// if(_maxHeight < height){
		// _maxHeight = height;
		// }
		// }
		// }
		scan.close();

		if (isTrain) {
			dm.fixSemanticUnits();
		}

		System.err.println("maxHeight=\t" + _maxHeight);

		return instances;
	}

	public static ArrayList<SemTextInstance> readInit(String filename, SemTextDataManager dm)
			throws NumberFormatException, IOException {

		ArrayList<SemTextInstance> instances = new ArrayList<>();

		// Scanner scan = new Scanner(new File(filename));
		BufferedReader scan = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		String line;
		while ((line = scan.readLine()) != null) {

			// System.err.println(line);

			if (line.startsWith("id:")) {
				int index = line.indexOf(":");
				int id = Integer.parseInt(line.substring(index + 1).trim());
				// System.err.println("OK"+id+"["+line+"]");
				String[] words = scan.readLine().substring(3).trim().split("\\s");
				WordToken[] wTokens = new WordToken[words.length];
				for (int k = 0; k < words.length; k++) {
					wTokens[k] = new WordToken(words[k]);
				}
				Sentence sent = new Sentence(wTokens);
				String mrl = scan.readLine().substring(4).trim();
				scan.readLine();
				ArrayList<String> prods_form = new ArrayList<>();
				while ((line = scan.readLine()).startsWith("*"))
					prods_form.add(line);
				SemanticForest tree = toTree(id, prods_form, dm);
				SemTextInstance inst = new SemTextInstance(id >= 0 ? id + 1 : id, id >= 0 ? 1.0 : PRIOR_WEIGHT, sent,
						tree, mrl);
				if (id < 0) {
					String phrase = sent.toString();
					SemanticUnit unit = tree.getRoot().getChildren()[0][0].getUnit();
					dm.addPriorUnitToPhrases(unit, phrase);
				}
				instances.add(inst);
			}
		}

		// while(scan.hasNextLine()){
		// String line = scan.nextLine();
		//
		// if(line.startsWith("id:")){
		// int index = line.indexOf(":");
		// int id = Integer.parseInt(line.substring(index+1).trim());
		// String[] words = scan.nextLine().substring(3).trim().split("\\s");
		// WordToken[] wTokens = new WordToken[words.length];
		// for(int k = 0; k<words.length; k++){
		// wTokens[k] = new WordToken(words[k]);
		// }
		// Sentence sent = new Sentence(wTokens);
		// String mrl = scan.nextLine().substring(4).trim();
		// scan.nextLine();
		// ArrayList<String> prods_form = new ArrayList<String>();
		// while((line=scan.nextLine()).startsWith("*"))
		// prods_form.add(line);
		// SemanticForest tree = toTree(id, prods_form, dm);
		// SemTextInstance inst = new SemTextInstance(id>=0? id+1 : id, id>=0? 1.0 :
		// PRIOR_WEIGHT, sent, tree, mrl);
		// instances.add(inst);
		// }
		// }
		scan.close();

		return instances;
	}

	public static HashMap<Integer, Quantity> readNumberOutput(String filepath) throws IOException {
		BufferedReader scan = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), "UTF8"));
		String line;
		HashMap<Integer, Quantity> id2number = new HashMap<>();
		while ((line = scan.readLine()) != null) {
			if (Character.isDigit(line.charAt(0))) {
				int index = line.indexOf(":");
				int id = Integer.parseInt(line.substring(0, index - 1));
				// System.out.println(id);
				String sentence = line.substring(index + 2).trim();
				ArrayList<Double> quantities = new ArrayList<>();
				ArrayList<Integer> charOffsetBegins = new ArrayList<>();
				ArrayList<Integer> charOffsetEnds = new ArrayList<>();
				ArrayList<Integer> gold = new ArrayList<>();
				ArrayList<Integer> pred = new ArrayList<>();
				line = scan.readLine().trim();
				if (line.startsWith("Quantities")) {
					String subline = line.substring(14, line.length() - 1);
					String pattern = "\\[\\=\\s\\-?\\d*\\.\\d*\\s\\]\\:\\(\\d*,\\s\\d*\\)";
					Pattern r = Pattern.compile(pattern);
					Matcher m = r.matcher(subline);
					int count = 0;
					// System.out.println(subline);
					ArrayList<String> segments = new ArrayList<>();
					while (m.find()) {
						count++;
						// System.out.println("Match number " + count);
						// System.out.println("start(): " + m.start());
						// System.out.println("end(): " + m.end());
						String singleton = subline.substring(m.start(), m.end());
						// System.out.println(singleton);
						segments.add(singleton);
					}
					for (int i = 0; i < count; i++) {
						String[] s = segments.get(i).split(":");
						double number = Double.parseDouble(s[0].substring(2, s[0].length() - 1));
						// System.out.println(s[0] + " " + number);
						String[] subs = s[1].substring(1, s[1].length() - 1).trim().split(",");
						// System.out.println(s[1] + " " + subs[0] + " " + subs[1]);
						quantities.add(number);
						charOffsetBegins.add(Integer.parseInt(subs[0].trim()));
						charOffsetEnds.add(Integer.parseInt(subs[1].trim()));
					}
				}
				line = scan.readLine().trim();
				if (line.startsWith("Gold")) {
					line = scan.readLine().trim();
					String goldString = line.substring(2, line.length() - 2);
					String[] goldFlag = goldString.split(",");
					// System.out.println(goldFlag[0]);
					for (String g : goldFlag) {
						// System.out.println(g);
						gold.add(Integer.parseInt(g.trim()));
					}
				}

				line = scan.readLine().trim();
				if (line.startsWith("Pred")) {
					line = scan.readLine().trim();
					String predString = line.substring(2, line.length() - 2);
					String[] predFlag = predString.split(",");
					// System.out.println(predString);
					for (String p : predFlag) {
						pred.add(Integer.parseInt(p.trim()));
					}
				}
				Quantity quantity = new Quantity(id, sentence, quantities, charOffsetBegins, charOffsetEnds, gold,
						pred);
				id2number.put(id, quantity);
			}
		}
		return id2number;
	}

	public static ArrayList<SemanticForest> getAlternateSolution(int index, SemTextDataManager dm) throws IOException {
		File dir = new File(EquationParserConfig.dataDir);
		String altFilename = index + ".alt";
		ArrayList<SemanticForest> outputs = new ArrayList<>();
		for (File file : dir.listFiles()) {
			if (file.getName().equals(altFilename)) {
				BufferedReader scan = new BufferedReader(
						new InputStreamReader(new FileInputStream(EquationParserConfig.dataDir + altFilename), "UTF8"));
				String line;
				while ((line = scan.readLine()) != null) {
					String equation = line.trim();
					SemanticForest tree = toTree(index, equation, dm);
					outputs.add(tree);
				}
			}
		}
		return outputs;
	}

	public static EquationInstance[] equationParsingReader(ArrayList<Integer> ids, SemTextDataManager dm,
			boolean isTrain) throws IOException, ParserConfigurationException, SAXException {
		ArrayList<EquationInstance> instances = new ArrayList<>();
		String filepath = "data/Numoccur.out";
		HashMap<Integer, Quantity> id2number = readNumberOutput(filepath);
		// System.out.println(id2number.keySet().size());
		File dir = new File(EquationParserConfig.dataDir);
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".txt")) {
				int index = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));
				if (ids.contains(index)) {
					String filename = EquationParserConfig.dataDir + index + ".txt";
					String annfile = EquationParserConfig.dataDir + index + ".ann";
					@SuppressWarnings("deprecation")
					List<String> lines = FileUtils.readLines(new File(filename));
					String text = lines.get(0); // problem text
					String equation = lines.get(2); // equation
					String annotated_text = lines.get(4);
					ArrayList<SemanticForest> altOutputs = getAlternateSolution(index, dm);

					// read features generated by Stanford CoreNLP tool
					ArrayList<String> lemmas = new ArrayList<>();
					ArrayList<Integer> charOffsetBegins = new ArrayList<>();
					ArrayList<Integer> charOffsetEnds = new ArrayList<>();
					ArrayList<String> posTags = new ArrayList<>();
					ArrayList<String> ners = new ArrayList<>();
					String featurepath = EquationParserConfig.featureDir + index + ".txt.xml";
					File featureFile = new File(featurepath);
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(featureFile);
					// System.out.println("Root element :" +
					// doc.getDocumentElement().getNodeName());
					NodeList nList = doc.getElementsByTagName("sentence");
					// System.out.println("----------------------------");
					// System.out.println(nList.getLength());
					Node nNode = nList.item(0);
					// System.out.println("\nCurrent Element :" + nNode.getNodeName());
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						// System.out.println("sentence id : " + eElement.getAttribute("id"));
						for (int i = 0; i < eElement.getElementsByTagName("lemma").getLength(); i++) {
							// System.out.println(
							// "Word : " + eElement.getElementsByTagName("word").item(i).getTextContent());
							// System.out.println(
							// "Lemma : " +
							// eElement.getElementsByTagName("lemma").item(i).getTextContent());
							// System.out.println("CharacterOffsetBegin : "
							// +
							// eElement.getElementsByTagName("CharacterOffsetBegin").item(i).getTextContent());
							// System.out.println("CharacterOffsetEnd : "
							// +
							// eElement.getElementsByTagName("CharacterOffsetEnd").item(i).getTextContent());
							// System.out
							// .println("POS : " +
							// eElement.getElementsByTagName("POS").item(i).getTextContent());
							lemmas.add(eElement.getElementsByTagName("lemma").item(i).getTextContent());
							charOffsetBegins.add(Integer.parseInt(
									eElement.getElementsByTagName("CharacterOffsetBegin").item(i).getTextContent()));
							charOffsetEnds.add(Integer.parseInt(
									eElement.getElementsByTagName("CharacterOffsetEnd").item(i).getTextContent()));
							posTags.add(eElement.getElementsByTagName("POS").item(i).getTextContent());
							ners.add(eElement.getElementsByTagName("NER").item(i).getTextContent());

						}
					}
					// System.out.println(index);

					// process problem text
					String[] tokens = text.trim().split(" ");
					WordToken[] wTokens = new WordToken[tokens.length];
					if (id2number.containsKey(index)) {
						// Quantity quantity = id2number.get(index);
					} else {
						throw new RuntimeException("this instance does not have quantity info: " + index);
					}
					Quantity quantity = id2number.get(index);
					ArrayList<Integer> q_charBegins = quantity.getCharOffsetBegins();
					ArrayList<Integer> q_charEnds = quantity.getCharOffsetEnds();
					String sentence = quantity.getSentence();
					// System.out.println("id:" + index);
					// System.out.println(sentence);
					// System.out.println(equation);
					// System.out.println(q_charBegins);
					// System.out.println(q_charEnds);
					int count = 0;
					int currPosition = 0;
					for (int k = 0; k < tokens.length; k++) {
						wTokens[k] = new WordToken(tokens[k]);
						wTokens[k].setLemma(lemmas.get(k));
						wTokens[k].setCharOffsetBegin(currPosition);
						wTokens[k].setCharOffsetEnd(currPosition + tokens[k].length());
						wTokens[k].setPOS(posTags.get(k));
						wTokens[k].setNER(ners.get(k));

						// System.out.println(tokens[k]);
						// System.out.println(lemmas.get(k));
						// System.out.println(currPosition);
						// System.out.println(currPosition + tokens[k].length());
						// System.out.println(currPosition);
						// System.out.println(currPosition + tokens[k].length());
						// System.out.println(wTokens[k]);
						if (q_charBegins.indexOf(currPosition) >= 0) {
							int q_index = q_charBegins.indexOf(currPosition);
							if (q_charBegins.indexOf(currPosition) == q_charEnds
									.indexOf(currPosition + tokens[k].length())
									|| q_charEnds.get(q_index) < (currPosition + tokens[k].length())) {
								int gold = quantity.getGold(q_index);
								int pred = quantity.getPred(q_index);
								double val = quantity.getValue(q_index);
								if (gold == 1) {
									wTokens[k].setGoldNumber(true);
								} else {
									wTokens[k].setGoldNumber(false);
								}
								if (pred == 1) {
									wTokens[k].setPredNumber(true);

								} else {
									wTokens[k].setPredNumber(false);
								}
								wTokens[k].setNumber(true);
								wTokens[k].setNumberVal(val);
								count++;
								// System.out.println(charOffsetBegins.get(k));
								// System.out.println(charOffsetEnds.get(k));
								// System.out.println(q_charBegins.get(q_index));
								// System.out.println(q_charEnds.get(q_index));
							}
						}
						currPosition += tokens[k].length() + 1;
					}

					if (count != quantity.getQuantitySize()) {
						throw new RuntimeException("every number should be considered! " + count);
					}
					ArrayList<Variable> variables = readGroundingVariables(annfile);
					Sentence input = new Sentence(wTokens);
					SemanticForest tree = toTree(index, equation, dm);
					EquationInstance instance = new EquationInstance(instances.size() + 1, 1.0, input, tree, equation,
							annotated_text, index);
					instance.setVariables(variables);
					// instance.setIdenticalOutput(altOutputs);
					// System.exit(1);
					if (isTrain) {
						instance.setLabeled();
					} else {
						instance.setUnlabeled();
					}
					instances.add(instance);
					int height = tree.getHeight();
					if (_maxHeight < height) {
						_maxHeight = height;
					}

				}
			}
		}

		if (isTrain) {
			dm.fixSemanticUnits();
		}

		System.err.println("maxHeight=\t" + _maxHeight);

		return instances.toArray(new EquationInstance[instances.size()]);
	}

	public static MathInstance[] MathSolverReader(ArrayList<Integer> ids, String file, SemTextDataManager dm,
			boolean isTrain) throws IOException, ParserConfigurationException, SAXException {
		int total_length = 0;
		ArrayList<MathInstance> instances = new ArrayList<>();
		String jsonString = FileUtils.readFileToString(new File(file));
		JSONArray testProblems = new JSONArray(jsonString);
		int count = 0;
		String lexiconFile = "data/lexicon.txt";
		MathLexicon lexicon = LexiconReader.read(lexiconFile);
		for (int i = 0; i < testProblems.length(); i++) {
			JSONObject test = testProblems.getJSONObject(i);

			int index = test.getInt("iIndex");
			if (!ids.contains(index))
				continue;

			String lEquations = ((JSONArray) test.get("lEquations")).get(0).toString().trim();
			double lSolutions = Double.parseDouble(((JSONArray) test.get("lSolutions")).get(0).toString());
			String orig_sQuestion = test.getString("sQuestion");
			String sQuestion = test.getString("sQuestion").trim();
			ArrayList<Integer> lAlignments = new ArrayList<>();
			for (int J = 0; J < ((JSONArray) test.get("lAlignments")).length(); J++)
				lAlignments.add(Integer.parseInt((((JSONArray) test.get("lAlignments"))).get(J).toString().trim()));

			// System.out.println("----------------------------");
			// System.out.println("id: " + index);
			// System.out.println("solution: " + lSolutions);
			// System.out.println("alignment: " + lAlignments);
			// System.out.println("equation: " + lEquations);
			// System.out.println("new equation: " + formatedEquation);
			// System.out.println("question: " + sQuestion);
			String formatedEquation = reformatEquation(lEquations, lAlignments);
			ArrayList<String> numbers = new ArrayList<>();
			char[] chars = orig_sQuestion.toCharArray();
			ArrayList<Integer> number_pos = new ArrayList<>();
			int num_position = 0;
			boolean OnceIf = true;
			for (int c = 0; c < chars.length; c++) {

				if (lAlignments.indexOf(c) != -1) {
					StringBuilder num_sb = new StringBuilder();
					while (chars[c] != ' ') {
						if (chars[c] == '$')
							continue;

						num_sb.append(chars[c]);
						c++;
					}
					String numString = num_sb.toString();
					if (numString.endsWith(".") || numString.endsWith(","))
						numString = numString.substring(0, numString.length() - 1);
					else if (numString.endsWith("-day")) {
						numString = numString.substring(0, numString.length() - 4);
					}
					// System.out.println(numString);
					// System.out.println(Double.parseDouble(numString));
					number_pos.add(num_position);
					num_position++;
					numbers.add(numString);
				}

				if (Character.isDigit(chars[c]) && chars[c - 1] != 'p' && chars[c - 2] != 'm' && OnceIf) {
					num_position++;
					OnceIf = false;
				}
			}
			StringBuilder text_sb = new StringBuilder();
			StringBuilder question_sb = new StringBuilder();

			String featurepath = MathSolverConfig.dataDir + MathSolverConfig.dataset + "/feature/" + index + ".txt.xml";
			File featureFile = new File(featurepath);
			ArrayList<String> tokens = new ArrayList<>();
			ArrayList<String> lemmas = new ArrayList<>();
			ArrayList<Integer> charOffsetBegins = new ArrayList<>();
			ArrayList<Integer> charOffsetEnds = new ArrayList<>();
			ArrayList<String> posTags = new ArrayList<>();
			ArrayList<String> ners = new ArrayList<>();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(featureFile);
			// System.out.println("Root element :" +
			// doc.getDocumentElement().getNodeName());
			NodeList nList = doc.getElementsByTagName("tokens");
			// System.out.println(nList.getLength());
			// System.out.println("\nCurrent Element :" + nNode.getNodeName());
			for (int numNode = 0; numNode < nList.getLength(); numNode++) {
				Node nNode = nList.item(numNode);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					// System.out.println("sentence id : " + eElement.getAttribute("id"));
					for (int ele = 0; ele < eElement.getElementsByTagName("lemma").getLength(); ele++) {
						// System.out
						// .println("Word : " +
						// eElement.getElementsByTagName("word").item(ele).getTextContent());
						// System.out.println(
						// "Lemma : " +
						// eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						// System.out.println("CharacterOffsetBegin : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent());
						// System.out.println("CharacterOffsetEnd : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent());
						// System.out
						// .println("POS : " +
						// eElement.getElementsByTagName("POS").item(ele).getTextContent());
						tokens.add(eElement.getElementsByTagName("word").item(ele).getTextContent());
						if (numNode < nList.getLength() - 1) {
							text_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						} else if (numNode == nList.getLength() - 1) {
							question_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						}
						lemmas.add(eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						charOffsetBegins.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent()));
						charOffsetEnds.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent()));
						posTags.add(eElement.getElementsByTagName("POS").item(ele).getTextContent());
						// ners.add(eElement.getElementsByTagName("NER").item(ele).getTextContent());
					}
				}
			}
			String text = text_sb.toString().trim();
			String questionSentence = question_sb.toString().trim();

			String[] words = sQuestion.trim().split(" ");
			if (words.length > tokens.size())
				throw new RuntimeException();
			int currPosition = 0;
			num_position = 0;
			WordToken[] wTokens = new WordToken[tokens.size()];
			ArrayList<String> added = new ArrayList<>();
			for (int k = 0; k < tokens.size(); k++) {
				// int index = word_tags[k].indexOf("_");
				// String tag = word_tags[k].substring(index + 1);
				wTokens[k] = new WordToken(tokens.get(k));
				wTokens[k].setLemma(lemmas.get(k));
				wTokens[k].setCharOffsetBegin(currPosition);
				wTokens[k].setCharOffsetEnd(currPosition + tokens.get(k).length());
				wTokens[k].setPOS(posTags.get(k));
				// wTokens[k].setNER(ners.get(k));
				if (isNumeric(tokens.get(k))) {
					if (MathSolverConfig.NUM_CONSTRAINT) {
						if (number_pos.indexOf(num_position) != -1) {
							wTokens[k].setGoldNumber(true);
							double value = Double.parseDouble(tokens.get(k));
							wTokens[k].setNumberVal(value);
							added.add(tokens.get(k));
						}
						num_position++;
					} else {
						wTokens[k].setGoldNumber(true);
						double value = Double.parseDouble(tokens.get(k));
						wTokens[k].setNumberVal(value);
						added.add(tokens.get(k));
					}
				} else if (index == 1335 && tokens.get(k).equals("5-day")) {
					if (MathSolverConfig.NUM_CONSTRAINT) {
						if (number_pos.indexOf(num_position) != -1) {
							wTokens[k].setGoldNumber(true);
							double value = 5.0;
							wTokens[k].setNumberVal(value);
							added.add("5");
						}
						num_position++;
					} else {
						wTokens[k].setGoldNumber(true);
						double value = 5.0;
						wTokens[k].setNumberVal(value);
						added.add("5");
					}
				}
				currPosition += tokens.get(k).length() + 1;
			}

			Sentence input = new Sentence(wTokens);
			total_length += input.length();
			if (MathSolverConfig.NUM_CONSTRAINT && !added.equals(numbers)) { // index == 1335
				System.out.println(input);
				System.out.println(index);
				System.out.println(added);
				System.out.println("numbers: " + numbers);
				System.out.println("express: " + lEquations);
				// System.exit(1);
			}

			String expression = formatedEquation;

			// System.err.println("exp:" + expression);
			SemanticForest tree = toMathExpressionTree(index, expression, dm);
			// System.err.println(tree);
			MathInstance instance = new MathInstance(instances.size() + 1, 1.0, input, tree, formatedEquation,
					lEquations, lSolutions, index);
			instance.setNumbers(numbers);
			instance.setText(text);
			instance.setQuestionSentence(questionSentence);
			// String nnew = LexiconReader.checkVaildLexicon(lexicon, numbers, text,
			// questionSentence);
			// if (!nnew.equals("")) {
			// System.out.println("----------------------------");
			// // System.out.println("id: " + index);
			// // System.out.println("solution: " + lSolutions);
			// // System.out.println("alignment: " + lAlignments);
			// System.out.println("equation: " + lEquations);
			// // System.out.println("new equation: " + formatedEquation);
			// System.out.println("question: " + sQuestion);
			// System.out.println("TEXT: " + text);
			// System.out.println("QUES: " + questionSentence);
			// System.out.println("lexicon: " + nnew);
			//
			// }
			int height = tree.getHeight();
			if (_maxHeight < height) {
				_maxHeight = height;
			}
			if (isTrain) {
				instance.setLabeled();
			} else {
				instance.setUnlabeled();
			}
			instances.add(instance);
		}
		if (isTrain) {
			dm.fixSemanticUnits();
		}

		System.err.println("maxHeight=\t" + _maxHeight);
		System.err.println(instances.size() + "...instances");
		System.err.println((total_length + 0.0) / instances.size() + "...instances");
		return instances.toArray(new MathInstance[instances.size()]);
	}

	public static MathInstance[] MathSolverReader_Roy(ArrayList<Integer> ids, String file, SemTextDataManager dm,
			boolean isTrain) throws IOException, ParserConfigurationException, SAXException {
		ArrayList<MathInstance> instances = new ArrayList<>();
		String jsonString = FileUtils.readFileToString(new File(file));
		JSONArray testProblems = new JSONArray(jsonString);
		int count = 0;
		String lexiconFile = "data/lexicon.txt";
		String filelist = "data/mathsolver/roy/filelist.txt";
		String problemtext = "data/mathsolver/roy/problemtext/";
		MathLexicon lexicon = LexiconReader.read(lexiconFile);
		for (int i = 0; i < testProblems.length(); i++) {
			JSONObject test = testProblems.getJSONObject(i);

			int index = test.getInt("iIndex");
			if (!ids.contains(index))
				continue;

			String lEquations = ((JSONArray) test.get("lEquations")).get(0).toString().trim();
			double lSolutions = Double.parseDouble(((JSONArray) test.get("lSolutions")).get(0).toString());
			String orig_sQuestion = test.getString("sQuestion");
			String sQuestion = test.getString("sQuestion").trim();
			ArrayList<Integer> lAlignments = new ArrayList<>();
			ArrayList<Integer> sortedlAlignments = new ArrayList<>();
			ArrayList<Double> allNumbers = new ArrayList<>();
			for (int J = 0; J < ((JSONArray) test.get("lAlignments")).length(); J++) {
				lAlignments.add(Integer.parseInt((((JSONArray) test.get("lAlignments"))).get(J).toString().trim()));
				sortedlAlignments
						.add(Integer.parseInt((((JSONArray) test.get("lAlignments"))).get(J).toString().trim()));
				Collections.sort(sortedlAlignments);
			}
			for (int a = 0; a < ((JSONArray) test.get("quants")).length(); a++)
				allNumbers.add(Double.parseDouble((((JSONArray) test.get("quants"))).get(a).toString().trim()));

			String formatedEquation = reformatEquationRoy(lEquations, lAlignments, allNumbers);
			// System.out.println("----------------------------");
			// System.out.println("id: " + index);
			// System.out.println("solution: " + lSolutions);
			// System.out.println("alignment: " + lAlignments);
			// System.out.println("equation: " + lEquations);
			// System.out.println("new equation: " + formatedEquation);
			// System.out.println("question: " + sQuestion);

			ArrayList<String> numbers = new ArrayList<>();
			for (int align : sortedlAlignments) {
				numbers.add(allNumbers.get(align) + "");
			}
			ArrayList<Double> double_num = new ArrayList<>();
			for (int align : sortedlAlignments) {
				double_num.add(allNumbers.get(align));
			}
			char[] chars = orig_sQuestion.toCharArray();
			ArrayList<Integer> number_pos = new ArrayList<>();
			StringBuilder text_sb = new StringBuilder();
			StringBuilder question_sb = new StringBuilder();

			String featurepath = MathSolverConfig.dataDir + MathSolverConfig.dataset + "/feature/" + index + ".txt.xml";
			File featureFile = new File(featurepath);
			ArrayList<String> tokens = new ArrayList<>();
			ArrayList<String> lemmas = new ArrayList<>();
			ArrayList<Integer> charOffsetBegins = new ArrayList<>();
			ArrayList<Integer> charOffsetEnds = new ArrayList<>();
			ArrayList<String> posTags = new ArrayList<>();
			ArrayList<String> ners = new ArrayList<>();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(featureFile);
			// System.out.println("Root element :" +
			// doc.getDocumentElement().getNodeName());
			NodeList nList = doc.getElementsByTagName("tokens");
			// System.out.println(nList.getLength());
			// System.out.println("\nCurrent Element :" + nNode.getNodeName());
			for (int numNode = 0; numNode < nList.getLength(); numNode++) {
				Node nNode = nList.item(numNode);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					// System.out.println("sentence id : " + eElement.getAttribute("id"));
					for (int ele = 0; ele < eElement.getElementsByTagName("lemma").getLength(); ele++) {
						// System.out
						// .println("Word : " +
						// eElement.getElementsByTagName("word").item(ele).getTextContent());
						// System.out.println(
						// "Lemma : " +
						// eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						// System.out.println("CharacterOffsetBegin : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent());
						// System.out.println("CharacterOffsetEnd : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent());
						// System.out
						// .println("POS : " +
						// eElement.getElementsByTagName("POS").item(ele).getTextContent());
						tokens.add(eElement.getElementsByTagName("word").item(ele).getTextContent());
						if (numNode < nList.getLength() - 1) {
							text_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						} else if (numNode == nList.getLength() - 1) {
							question_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						}
						lemmas.add(eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						charOffsetBegins.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent()));
						charOffsetEnds.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent()));
						posTags.add(eElement.getElementsByTagName("POS").item(ele).getTextContent());
						// ners.add(eElement.getElementsByTagName("NER").item(ele).getTextContent());
					}
				}
			}
			String text = text_sb.toString().trim();
			String questionSentence = question_sb.toString().trim();

			String[] words = sQuestion.trim().split(" ");
			if (words.length > tokens.size())
				throw new RuntimeException("\n" + words.length + ":" + Arrays.asList(words).toString() + "\n"
						+ tokens.size() + ":" + tokens.toString());
			int currPosition = 0;
			WordToken[] wTokens = new WordToken[tokens.size()];
			ArrayList<Double> added = new ArrayList<>();
			ArrayList<WordToken> numberCandidate = new ArrayList<>();
			for (int k = 0; k < tokens.size(); k++) {
				// int index = word_tags[k].indexOf("_");
				// String tag = word_tags[k].substring(index + 1);
				wTokens[k] = new WordToken(tokens.get(k));
				wTokens[k].setLemma(lemmas.get(k));
				wTokens[k].setCharOffsetBegin(currPosition);
				wTokens[k].setCharOffsetEnd(currPosition + tokens.get(k).length());
				wTokens[k].setPOS(posTags.get(k));
				// wTokens[k].setNER(ners.get(k));
				if (isNumeric(tokens.get(k))) {
					numberCandidate.add(wTokens[k]);
					// if (double_num.contains(Double.parseDouble(tokens.get(k)))) {
					// wTokens[k].setGoldNumber(true);
					// double value = Double.parseDouble(tokens.get(k));
					// wTokens[k].setNumberVal(value);
					// added.add(Double.parseDouble(tokens.get(k)));
					// }
				} else if (index == 822 && tokens.get(k).equals("5.0-day")) {
					numberCandidate.add(wTokens[k]);
					// wTokens[k].setGoldNumber(true);
					// double value = 5.0;
					// wTokens[k].setNumberVal(value);
					// added.add(5.0);

				} else if (index == 1232857319 && tokens.get(k).equals("3-day")) {
					numberCandidate.add(wTokens[k]);
					// wTokens[k].setGoldNumber(true);
					// double value = 3.0;
					// wTokens[k].setNumberVal(value);
					// added.add(3.0);

				}
				currPosition += tokens.get(k).length() + 1;
			}
			for (int sl : sortedlAlignments) {
				numberCandidate.get(sl).setGoldNumber(true);
				if (numberCandidate.get(sl).getName().endsWith("5.0-day")) {
					double value = 5.0;
					numberCandidate.get(sl).setNumberVal(value);
					added.add(5.0);
				} else if (numberCandidate.get(sl).getName().endsWith("3-day")) {
					double value = 3.0;
					numberCandidate.get(sl).setNumberVal(value);
					added.add(3.0);
				} else if (numberCandidate.get(sl).getName().contains(",")) {
					String token_str = numberCandidate.get(sl).getName().replace(",", "");
					double value = Double.parseDouble(token_str);
					numberCandidate.get(sl).setNumberVal(value);
					added.add(value);
				} else {
					double value = Double.parseDouble(numberCandidate.get(sl).getName());
					numberCandidate.get(sl).setNumberVal(value);
					added.add(Double.parseDouble(numberCandidate.get(sl).getName()));
				}
			}
			Sentence input = new Sentence(wTokens);
			if (!added.equals(double_num)) { // index == 1335
				System.out.println(input);
				System.out.println(index);
				System.out.println(added);
				System.out.println("numbers: " + double_num);
				System.out.println("all numbers: " + allNumbers);
				System.out.println("express: " + lEquations);
			}

			String expression = formatedEquation;

			// System.err.println(expression);
			SemanticForest tree = toMathExpressionTree(index, expression, dm);
			// SemanticForest tree = null;
			MathInstance instance = new MathInstance(instances.size() + 1, 1.0, input, tree, formatedEquation,
					lEquations, lSolutions, index);
			instance.setNumbers(numbers);
			instance.setText(text);
			instance.setQuestionSentence(questionSentence);
			// String nnew = LexiconReader.checkVaildLexicon(lexicon, numbers, text,
			// questionSentence);
			// if (!nnew.equals("")) {
			// System.out.println("----------------------------");
			// // System.out.println("id: " + index);
			// // System.out.println("solution: " + lSolutions);
			// // System.out.println("alignment: " + lAlignments);
			// System.out.println("equation: " + lEquations);
			// // System.out.println("new equation: " + formatedEquation);
			// System.out.println("question: " + sQuestion);
			// System.out.println("TEXT: " + text);
			// System.out.println("QUES: " + questionSentence);
			// System.out.println("lexicon: " + nnew);
			//
			// }
			int height = tree.getHeight();
			if (_maxHeight < height) {
				_maxHeight = height;
			}
			if (isTrain) {
				instance.setLabeled();
			} else {
				instance.setUnlabeled();
			}

			instances.add(instance);
		}
		if (isTrain) {
			dm.fixSemanticUnits();
		}
		System.err.println("maxHeight=\t" + _maxHeight);
		System.err.println(instances.size() + "...instances");
		return instances.toArray(new MathInstance[instances.size()]);
	}

	public static MathInstance[] MathSolverReader_Math23K(ArrayList<Integer> ids, String file, SemTextDataManager dm,
			boolean isTrain) throws IOException, ParserConfigurationException, SAXException {
		ArrayList<MathInstance> instances = new ArrayList<>();
		String jsonString = FileUtils.readFileToString(new File(file));
		JSONArray testProblems = new JSONArray(jsonString);
		int count = 0;
		String lexiconFile = "data/lexicon.txt";
		String filelist = "data/mathsolver/roy/filelist.txt";
		String problemtext = "data/mathsolver/roy/problemtext/";
		MathLexicon lexicon = LexiconReader.read(lexiconFile);
		for (int i = 0; i < testProblems.length(); i++) {
			JSONObject test = testProblems.getJSONObject(i);

			int index = test.getInt("iIndex");
			if (!ids.contains(index))
				continue;

			String lEquations = test.getString("lEquations");
			double lSolutions = Double.parseDouble(((JSONArray) test.get("lSolutions")).get(0).toString());
			String orig_sQuestion = test.getString("sQuestion");
			String sQuestion = test.getString("sQuestion").trim();
			ArrayList<Integer> lAlignments = new ArrayList<>();
			ArrayList<Integer> sortedlAlignments = new ArrayList<>();
			ArrayList<Double> quants = new ArrayList<>();
			ArrayList<Double> allNumbers = new ArrayList<>();
			for (int J = 0; J < ((JSONArray) test.get("lAlignments")).length(); J++) {
				lAlignments.add(Integer.parseInt((((JSONArray) test.get("lAlignments"))).get(J).toString().trim()));
				sortedlAlignments
						.add(Integer.parseInt((((JSONArray) test.get("lAlignments"))).get(J).toString().trim()));
				Collections.sort(sortedlAlignments);
			}
			for (int J = 0; J < ((JSONArray) test.get("quants")).length(); J++) {
				quants.add(Double.parseDouble((((JSONArray) test.get("quants"))).get(J).toString().trim()));
			}
			if (sortedlAlignments.size() == 1) {
				System.out.println("----------------------------");
				System.out.println("id: " + index);
				System.out.println("solution: " + lSolutions);
				System.out.println("alignment: " + lAlignments);
				System.out.println("equation: " + lEquations);
				// System.out.println("new equation: " + formatedEquation);
				System.out.println("question: " + sQuestion);
			}
			for (int a = 0; a < ((JSONArray) test.get("quants")).length(); a++)
				allNumbers.add(Double.parseDouble((((JSONArray) test.get("quants"))).get(a).toString().trim()));

			String formatedEquation = reformatEquationAll(lEquations, lAlignments, allNumbers);

			ArrayList<String> numbers = new ArrayList<>();
			for (int align : sortedlAlignments) {
				numbers.add(allNumbers.get(align) + "");
			}
			ArrayList<Double> double_num = new ArrayList<>();
			for (int align : sortedlAlignments) {
				double_num.add(allNumbers.get(align));
			}
			char[] chars = orig_sQuestion.toCharArray();
			ArrayList<Integer> number_pos = new ArrayList<>();
			StringBuilder text_sb = new StringBuilder();
			StringBuilder question_sb = new StringBuilder();

			String featurepath = MathSolverConfig.dataDir + MathSolverConfig.dataset + "/feature/" + index + ".txt.xml";
			File featureFile = new File(featurepath);
			ArrayList<String> tokens = new ArrayList<>();
			ArrayList<String> lemmas = new ArrayList<>();
			ArrayList<Integer> charOffsetBegins = new ArrayList<>();
			ArrayList<Integer> charOffsetEnds = new ArrayList<>();
			ArrayList<String> posTags = new ArrayList<>();
			ArrayList<String> ners = new ArrayList<>();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(featureFile);
			// System.out.println("Root element :" +
			// doc.getDocumentElement().getNodeName());
			NodeList nList = doc.getElementsByTagName("tokens");
			// System.out.println(nList.getLength());
			// System.out.println("\nCurrent Element :" + nNode.getNodeName());
			for (int numNode = 0; numNode < nList.getLength(); numNode++) {
				Node nNode = nList.item(numNode);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					// System.out.println("sentence id : " + eElement.getAttribute("id"));
					for (int ele = 0; ele < eElement.getElementsByTagName("lemma").getLength(); ele++) {
						// System.out
						// .println("Word : " +
						// eElement.getElementsByTagName("word").item(ele).getTextContent());
						// System.out.println(
						// "Lemma : " +
						// eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						// System.out.println("CharacterOffsetBegin : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent());
						// System.out.println("CharacterOffsetEnd : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent());
						// System.out
						// .println("POS : " +
						// eElement.getElementsByTagName("POS").item(ele).getTextContent());
						tokens.add(eElement.getElementsByTagName("word").item(ele).getTextContent());
						if (numNode < nList.getLength() - 1) {
							text_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						} else if (numNode == nList.getLength() - 1) {
							question_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						}
						lemmas.add(eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						charOffsetBegins.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent()));
						charOffsetEnds.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent()));
						posTags.add(eElement.getElementsByTagName("POS").item(ele).getTextContent());
						// ners.add(eElement.getElementsByTagName("NER").item(ele).getTextContent());
					}
				}
			}
			String text = text_sb.toString().trim();
			String questionSentence = question_sb.toString().trim();

			String[] words = sQuestion.trim().split(" ");
			if (words.length > tokens.size())
				throw new RuntimeException("\n" + words.length + ":" + Arrays.asList(words).toString() + "\n"
						+ tokens.size() + ":" + tokens.toString());
			int currPosition = 0;
			WordToken[] wTokens = new WordToken[tokens.size()];
			ArrayList<Double> added = new ArrayList<>();
			ArrayList<WordToken> numberCandidate = new ArrayList<>();
			for (int k = 0; k < tokens.size(); k++) {
				// int index = word_tags[k].indexOf("_");
				// String tag = word_tags[k].substring(index + 1);
				wTokens[k] = new WordToken(tokens.get(k));
				wTokens[k].setLemma(lemmas.get(k));
				wTokens[k].setCharOffsetBegin(currPosition);
				wTokens[k].setCharOffsetEnd(currPosition + tokens.get(k).length());
				wTokens[k].setPOS(posTags.get(k));
				// wTokens[k].setNER(ners.get(k));
				if (isNumeric(tokens.get(k))) {
					numberCandidate.add(wTokens[k]);
					// if (double_num.contains(Double.parseDouble(tokens.get(k)))) {
					// wTokens[k].setGoldNumber(true);
					// double value = Double.parseDouble(tokens.get(k));
					// wTokens[k].setNumberVal(value);
					// added.add(Double.parseDouble(tokens.get(k)));
					// }
				} else if (index == 880 && tokens.get(k).equals("5-day")) {
					numberCandidate.add(wTokens[k]);
					// wTokens[k].setGoldNumber(true);
					// double value = 5.0;
					// wTokens[k].setNumberVal(value);
					// added.add(5.0);

				} else if (index == 1232857319 && tokens.get(k).equals("3-day")) {
					numberCandidate.add(wTokens[k]);
					// wTokens[k].setGoldNumber(true);
					// double value = 3.0;
					// wTokens[k].setNumberVal(value);
					// added.add(3.0);

				}
				currPosition += tokens.get(k).length() + 1;
			}
			if (MathSolverConfig.NUM_CONSTRAINT) {
				for (int sl : sortedlAlignments) {
					numberCandidate.get(sl).setGoldNumber(true);
					if (numberCandidate.get(sl).getName().endsWith("5-day")) {
						double value = 5.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(5.0);
					} else if (numberCandidate.get(sl).getName().endsWith("3-day")) {
						double value = 3.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(3.0);
					} else if (numberCandidate.get(sl).getName().contains(",")) {
						String token_str = numberCandidate.get(sl).getName().replace(",", "");
						double value = Double.parseDouble(token_str);
						numberCandidate.get(sl).setNumberVal(value);
						added.add(value);
					} else {
						double value = Double.parseDouble(numberCandidate.get(sl).getName());
						numberCandidate.get(sl).setNumberVal(value);
						added.add(Double.parseDouble(numberCandidate.get(sl).getName()));
					}
				}
			} else {
				for (int sl = 0; sl < numberCandidate.size(); sl++) {
					numberCandidate.get(sl).setGoldNumber(true);
					if (numberCandidate.get(sl).getName().endsWith("5-day")) {
						double value = 5.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(5.0);
					} else if (numberCandidate.get(sl).getName().endsWith("3-day")) {
						double value = 3.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(3.0);
					} else if (numberCandidate.get(sl).getName().contains(",")) {
						String token_str = numberCandidate.get(sl).getName().replace(",", "");
						double value = Double.parseDouble(token_str);
						numberCandidate.get(sl).setNumberVal(value);
						added.add(value);
					} else {
						double value = Double.parseDouble(numberCandidate.get(sl).getName());
						numberCandidate.get(sl).setNumberVal(value);
						added.add(Double.parseDouble(numberCandidate.get(sl).getName()));
					}
				}
			}
			Sentence input = new Sentence(wTokens);
			if (MathSolverConfig.NUM_CONSTRAINT && !added.equals(double_num)) { // index == 1335
				System.out.println(input);
				System.out.println(index);
				System.out.println(added);
				System.out.println("numbers: " + double_num);
				System.out.println("all numbers: " + allNumbers);
				System.out.println("express: " + lEquations);
			}
			if (MathSolverConfig.NUM_CONSTRAINT && added.size() != lAlignments.size()) {
				System.out.println(input);
				System.out.println(index);
				System.out.println(added);
				System.out.println("numbers: " + double_num);
				System.out.println("all numbers: " + allNumbers);
				System.out.println("express: " + lEquations);
			}
			String expression = formatedEquation;

			// System.out.println(expression);
			SemanticForest tree = toMathExpressionTree(index, expression, dm);
			// SemanticForest tree = null;
			// System.out.println(tree);

			MathInstance instance = new MathInstance(instances.size() + 1, 1.0, input, tree, formatedEquation,
					lEquations, lSolutions, index);
			instance.setNumbers(numbers);
			instance.setText(text);
			instance.setQuestionSentence(questionSentence);
			// String nnew = LexiconReader.checkVaildLexicon(lexicon, numbers, text,
			// questionSentence);
			// if (!nnew.equals("")) {
			// System.out.println("----------------------------");
			// // System.out.println("id: " + index);
			// // System.out.println("solution: " + lSolutions);
			// // System.out.println("alignment: " + lAlignments);
			// System.out.println("equation: " + lEquations);
			// // System.out.println("new equation: " + formatedEquation);
			// System.out.println("question: " + sQuestion);
			// System.out.println("TEXT: " + text);
			// System.out.println("QUES: " + questionSentence);
			// System.out.println("lexicon: " + nnew);
			//
			// }
			int height = tree.getHeight();
			if (_maxHeight < height) {
				_maxHeight = height;
			}
			if (isTrain) {
				instance.setLabeled();
			} else {
				instance.setUnlabeled();
			}

			instances.add(instance);
		}
		if (isTrain) {
			dm.fixSemanticUnits();
		}
		System.err.println("maxHeight=\t" + _maxHeight);
		System.err.println(instances.size() + "...instances");
		return instances.toArray(new MathInstance[instances.size()]);
	}

	public static MathInstance[] MathSolverReader_All(ArrayList<Integer> ids, String file, SemTextDataManager dm,
			boolean isTrain) throws IOException, ParserConfigurationException, SAXException {
		ArrayList<MathInstance> instances = new ArrayList<>();
		String jsonString = FileUtils.readFileToString(new File(file));
		JSONArray testProblems = new JSONArray(jsonString);
		int count = 0;
		String lexiconFile = "data/lexicon.txt";
		String filelist = "data/mathsolver/roy/filelist.txt";
		String problemtext = "data/mathsolver/roy/problemtext/";
		MathLexicon lexicon = LexiconReader.read(lexiconFile);
		for (int i = 0; i < testProblems.length(); i++) {
			JSONObject test = testProblems.getJSONObject(i);

			int index = test.getInt("iIndex");
			if (!ids.contains(index))
				continue;

			String lEquations = test.getString("lEquations");
			double lSolutions = Double.parseDouble(((JSONArray) test.get("lSolutions")).get(0).toString());
			String orig_sQuestion = test.getString("sQuestion");
			String sQuestion = test.getString("sQuestion").trim();
			ArrayList<Integer> lAlignments = new ArrayList<>();
			ArrayList<Integer> sortedlAlignments = new ArrayList<>();
			ArrayList<Double> quants = new ArrayList<>();
			ArrayList<Double> allNumbers = new ArrayList<>();
			for (int J = 0; J < ((JSONArray) test.get("lAlignments")).length(); J++) {
				lAlignments.add(Integer.parseInt((((JSONArray) test.get("lAlignments"))).get(J).toString().trim()));
				sortedlAlignments
						.add(Integer.parseInt((((JSONArray) test.get("lAlignments"))).get(J).toString().trim()));
				Collections.sort(sortedlAlignments);
			}
			for (int J = 0; J < ((JSONArray) test.get("quants")).length(); J++) {
				quants.add(Double.parseDouble((((JSONArray) test.get("quants"))).get(J).toString().trim()));
			}
			if (sortedlAlignments.size() == 1) {
				System.out.println("----------------------------");
				System.out.println("id: " + index);
				System.out.println("solution: " + lSolutions);
				System.out.println("alignment: " + lAlignments);
				System.out.println("equation: " + lEquations);
				// System.out.println("new equation: " + formatedEquation);
				System.out.println("question: " + sQuestion);
			}
			for (int a = 0; a < ((JSONArray) test.get("quants")).length(); a++)
				allNumbers.add(Double.parseDouble((((JSONArray) test.get("quants"))).get(a).toString().trim()));

			String formatedEquation = reformatEquationAll(lEquations, lAlignments, allNumbers);

			ArrayList<String> numbers = new ArrayList<>();
			for (int align : sortedlAlignments) {
				numbers.add(allNumbers.get(align) + "");
			}
			ArrayList<Double> double_num = new ArrayList<>();
			for (int align : sortedlAlignments) {
				double_num.add(allNumbers.get(align));
			}
			char[] chars = orig_sQuestion.toCharArray();
			ArrayList<Integer> number_pos = new ArrayList<>();
			StringBuilder text_sb = new StringBuilder();
			StringBuilder question_sb = new StringBuilder();

			String featurepath = MathSolverConfig.dataDir + MathSolverConfig.dataset + "/feature/" + index + ".txt.xml";
			File featureFile = new File(featurepath);
			ArrayList<String> tokens = new ArrayList<>();
			ArrayList<String> lemmas = new ArrayList<>();
			ArrayList<Integer> charOffsetBegins = new ArrayList<>();
			ArrayList<Integer> charOffsetEnds = new ArrayList<>();
			ArrayList<String> posTags = new ArrayList<>();
			ArrayList<String> ners = new ArrayList<>();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(featureFile);
			// System.out.println("Root element :" +
			// doc.getDocumentElement().getNodeName());
			NodeList nList = doc.getElementsByTagName("tokens");
			// System.out.println(nList.getLength());
			// System.out.println("\nCurrent Element :" + nNode.getNodeName());
			for (int numNode = 0; numNode < nList.getLength(); numNode++) {
				Node nNode = nList.item(numNode);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					// System.out.println("sentence id : " + eElement.getAttribute("id"));
					for (int ele = 0; ele < eElement.getElementsByTagName("lemma").getLength(); ele++) {
						// System.out
						// .println("Word : " +
						// eElement.getElementsByTagName("word").item(ele).getTextContent());
						// System.out.println(
						// "Lemma : " +
						// eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						// System.out.println("CharacterOffsetBegin : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent());
						// System.out.println("CharacterOffsetEnd : "
						// +
						// eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent());
						// System.out
						// .println("POS : " +
						// eElement.getElementsByTagName("POS").item(ele).getTextContent());
						tokens.add(eElement.getElementsByTagName("word").item(ele).getTextContent());
						if (numNode < nList.getLength() - 1) {
							text_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						} else if (numNode == nList.getLength() - 1) {
							question_sb.append(eElement.getElementsByTagName("word").item(ele).getTextContent() + " ");
						}
						lemmas.add(eElement.getElementsByTagName("lemma").item(ele).getTextContent());
						charOffsetBegins.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetBegin").item(ele).getTextContent()));
						charOffsetEnds.add(Integer.parseInt(
								eElement.getElementsByTagName("CharacterOffsetEnd").item(ele).getTextContent()));
						posTags.add(eElement.getElementsByTagName("POS").item(ele).getTextContent());
						// ners.add(eElement.getElementsByTagName("NER").item(ele).getTextContent());
					}
				}
			}
			String text = text_sb.toString().trim();
			String questionSentence = question_sb.toString().trim();

			String[] words = sQuestion.trim().split(" ");
			if (words.length > tokens.size())
				throw new RuntimeException("\n" + words.length + ":" + Arrays.asList(words).toString() + "\n"
						+ tokens.size() + ":" + tokens.toString());
			int currPosition = 0;
			WordToken[] wTokens = new WordToken[tokens.size()];
			ArrayList<Double> added = new ArrayList<>();
			ArrayList<WordToken> numberCandidate = new ArrayList<>();
			for (int k = 0; k < tokens.size(); k++) {
				// int index = word_tags[k].indexOf("_");
				// String tag = word_tags[k].substring(index + 1);
				wTokens[k] = new WordToken(tokens.get(k));
				wTokens[k].setLemma(lemmas.get(k));
				wTokens[k].setCharOffsetBegin(currPosition);
				wTokens[k].setCharOffsetEnd(currPosition + tokens.get(k).length());
				wTokens[k].setPOS(posTags.get(k));
				// wTokens[k].setNER(ners.get(k));
				if (isNumeric(tokens.get(k))) {
					numberCandidate.add(wTokens[k]);
					// if (double_num.contains(Double.parseDouble(tokens.get(k)))) {
					// wTokens[k].setGoldNumber(true);
					// double value = Double.parseDouble(tokens.get(k));
					// wTokens[k].setNumberVal(value);
					// added.add(Double.parseDouble(tokens.get(k)));
					// }
				} else if (index == 880 && tokens.get(k).equals("5-day")) {
					numberCandidate.add(wTokens[k]);
					// wTokens[k].setGoldNumber(true);
					// double value = 5.0;
					// wTokens[k].setNumberVal(value);
					// added.add(5.0);

				} else if (index == 1232857319 && tokens.get(k).equals("3-day")) {
					numberCandidate.add(wTokens[k]);
					// wTokens[k].setGoldNumber(true);
					// double value = 3.0;
					// wTokens[k].setNumberVal(value);
					// added.add(3.0);

				}
				currPosition += tokens.get(k).length() + 1;
			}
			if (MathSolverConfig.NUM_CONSTRAINT) {
				for (int sl : sortedlAlignments) {
					numberCandidate.get(sl).setGoldNumber(true);
					if (numberCandidate.get(sl).getName().endsWith("5-day")) {
						double value = 5.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(5.0);
					} else if (numberCandidate.get(sl).getName().endsWith("3-day")) {
						double value = 3.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(3.0);
					} else if (numberCandidate.get(sl).getName().contains(",")) {
						String token_str = numberCandidate.get(sl).getName().replace(",", "");
						double value = Double.parseDouble(token_str);
						numberCandidate.get(sl).setNumberVal(value);
						added.add(value);
					} else {
						double value = Double.parseDouble(numberCandidate.get(sl).getName());
						numberCandidate.get(sl).setNumberVal(value);
						added.add(Double.parseDouble(numberCandidate.get(sl).getName()));
					}
				}
			} else {
				for (int sl = 0; sl < numberCandidate.size(); sl++) {
					numberCandidate.get(sl).setGoldNumber(true);
					if (numberCandidate.get(sl).getName().endsWith("5-day")) {
						double value = 5.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(5.0);
					} else if (numberCandidate.get(sl).getName().endsWith("3-day")) {
						double value = 3.0;
						numberCandidate.get(sl).setNumberVal(value);
						added.add(3.0);
					} else if (numberCandidate.get(sl).getName().contains(",")) {
						String token_str = numberCandidate.get(sl).getName().replace(",", "");
						double value = Double.parseDouble(token_str);
						numberCandidate.get(sl).setNumberVal(value);
						added.add(value);
					} else {
						double value = Double.parseDouble(numberCandidate.get(sl).getName());
						numberCandidate.get(sl).setNumberVal(value);
						added.add(Double.parseDouble(numberCandidate.get(sl).getName()));
					}
				}
			}
			Sentence input = new Sentence(wTokens);
			if (MathSolverConfig.NUM_CONSTRAINT && !added.equals(double_num)) { // index == 1335
				System.out.println(input);
				System.out.println(index);
				System.out.println(added);
				System.out.println("numbers: " + double_num);
				System.out.println("all numbers: " + allNumbers);
				System.out.println("express: " + lEquations);
			}
			if (MathSolverConfig.NUM_CONSTRAINT && added.size() != lAlignments.size()) {
				System.out.println(input);
				System.out.println(index);
				System.out.println(added);
				System.out.println("numbers: " + double_num);
				System.out.println("all numbers: " + allNumbers);
				System.out.println("express: " + lEquations);
			}
			String expression = formatedEquation;

			// System.out.println(expression);
			SemanticForest tree = toMathExpressionTree(index, expression, dm);
			// SemanticForest tree = null;
			// System.out.println(tree);

			MathInstance instance = new MathInstance(instances.size() + 1, 1.0, input, tree, formatedEquation,
					lEquations, lSolutions, index);
			instance.setNumbers(numbers);
			instance.setText(text);
			instance.setQuestionSentence(questionSentence);
			// String nnew = LexiconReader.checkVaildLexicon(lexicon, numbers, text,
			// questionSentence);
			// if (!nnew.equals("")) {
			// System.out.println("----------------------------");
			// // System.out.println("id: " + index);
			// // System.out.println("solution: " + lSolutions);
			// // System.out.println("alignment: " + lAlignments);
			// System.out.println("equation: " + lEquations);
			// // System.out.println("new equation: " + formatedEquation);
			// System.out.println("question: " + sQuestion);
			// System.out.println("TEXT: " + text);
			// System.out.println("QUES: " + questionSentence);
			// System.out.println("lexicon: " + nnew);
			//
			// }
			int height = tree.getHeight();
			if (_maxHeight < height) {
				_maxHeight = height;
			}
			if (isTrain) {
				instance.setLabeled();
			} else {
				instance.setUnlabeled();
			}

			instances.add(instance);
		}
		if (isTrain) {
			dm.fixSemanticUnits();
		}
		System.err.println("maxHeight=\t" + _maxHeight);
		System.err.println(instances.size() + "...instances");
		return instances.toArray(new MathInstance[instances.size()]);
	}

	public static String reformatEquation(String equation, ArrayList<Integer> alignments) {
		if (MathSolverConfig.NO_REVERSE_OP) {
			if (MathSolverConfig.USE_SUFFIX_X) {
				equation = equation.replace("X=", "");
				equation = equation + "=X";
			}
			if (MathSolverConfig.NO_X) {
				equation = equation.replace("X=", "");
			}
			if (MathSolverConfig.EQUAL_ROOT) {
				equation = equation.replace("X=", "");
				equation = equation + "=";
			}
			return equation;
		}
		if (isSorted(alignments)) {
			if (MathSolverConfig.USE_SUFFIX_X) {
				equation = equation.replace("X=", "");
				equation = equation + "=X";
			}
			if (MathSolverConfig.NO_X) {
				equation = equation.replace("X=", "");
			}
			if (MathSolverConfig.EQUAL_ROOT) {
				equation = equation.replace("X=", "");
				equation = equation + "=";
			}
			return equation;
		}

		// equation = equation.replace("X=", "");
		int index = 0;
		String sign = "";
		// System.out.println(equation);
		if (alignments.size() == 2) {
			if (equation.indexOf("+") >= 0) {
				index = equation.indexOf("+");
				sign = "+";
			} else if (equation.indexOf("*") >= 0) {
				index = equation.indexOf("*");
				sign = "*";
			} else if (equation.indexOf("-") >= 0) {
				index = equation.indexOf("-");
				sign = "#"; // reverse subtraction
			} else if (equation.indexOf("/") >= 0) {
				index = equation.indexOf("/");
				sign = "$"; // reverse division
			}
			String firstNum = equation.substring(3, index);
			String secondNum = equation.substring(index + 1, equation.length() - 1);
			String new_equation = "X=" + secondNum + sign + firstNum;
			if (MathSolverConfig.USE_SUFFIX_X) {
				new_equation = secondNum + sign + firstNum + "=X";
			}
			if (MathSolverConfig.NO_X) {
				new_equation = secondNum + sign + firstNum;
			}
			if (MathSolverConfig.EQUAL_ROOT) {
				new_equation = secondNum + sign + firstNum + "=";
			}
			return new_equation;

		} else if (alignments.size() == 3) {

			ArrayList<Integer> sortedAlignemnts = new ArrayList<>();
			sortedAlignemnts.addAll(alignments);
			Collections.sort(sortedAlignemnts);
			ArrayList<Integer> mapping = new ArrayList<>();
			for (int idx : sortedAlignemnts) {
				int map = alignments.indexOf(idx) + 1;
				mapping.add(map);
			}

			if (equation.indexOf("*") == -1 && equation.indexOf("/") == -1) {

				ArrayList<String> numbers = extractNumbers(equation);
				// System.out.println(numbers);
				String new_equation = "X=((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")";
				if (MathSolverConfig.USE_SUFFIX_X) {
					new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")=X";
				}
				if (MathSolverConfig.NO_X) {
					new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")";
				}
				if (MathSolverConfig.EQUAL_ROOT) {
					new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")=";
				}
				return new_equation;
			} else if (equation.indexOf("*") >= 0) {
				// System.out.println(alignments);
				// System.out.println(sortedAlignemnts);
				// System.out.println(mapping.toString());
				// System.out.println(equation);
				index = equation.indexOf("*");
				sign = "*";
				String firstNum = equation.substring(3, index);
				String secondNum = equation.substring(index + 1, equation.length() - 1);
				String new_equation = "X=(" + secondNum + sign + firstNum + ")";
				if (MathSolverConfig.USE_SUFFIX_X) {
					new_equation = "(" + secondNum + sign + firstNum + ")=X";
				}
				if (MathSolverConfig.NO_X) {
					new_equation = "(" + secondNum + sign + firstNum + ")";
				}
				if (MathSolverConfig.EQUAL_ROOT) {
					new_equation = "(" + secondNum + sign + firstNum + ")=";
				}
				return new_equation;
			} else if (equation.indexOf("/") >= 0) {
				// System.out.println(alignments);
				// System.out.println(sortedAlignemnts);
				// System.out.println(mapping.toString());
				// System.out.println(equation);
				index = equation.indexOf("/");
				sign = "$";
				String firstNum = equation.substring(3, index);
				String secondNum = equation.substring(index + 1, equation.length() - 1);
				String new_equation = "X=(" + secondNum + sign + firstNum + ")";
				if (MathSolverConfig.USE_SUFFIX_X) {
					new_equation = "(" + secondNum + sign + firstNum + ")=X";
				}
				if (MathSolverConfig.NO_X) {
					new_equation = "(" + secondNum + sign + firstNum + ")";
				}
				if (MathSolverConfig.EQUAL_ROOT) {
					new_equation = "(" + secondNum + sign + firstNum + ")=";
				}
				return new_equation;
			}
		}
		return "";
	}

	public static String reformatEquationRoy(String equation, ArrayList<Integer> alignments,
			ArrayList<Double> allNumbers) {

		if (isSorted(alignments)) {
			if (MathSolverConfig.USE_SUFFIX_X) {
				equation = equation.replace("X=", "");
				equation = equation + "=X";
			}
			if (MathSolverConfig.NO_X) {
				equation = equation.replace("X=", "");
			}
			return equation;
		}
		// System.out.println("----------------------------");
		// System.out.println("alignment: " + alignments);
		// System.out.println("equation: " + equation);

		// equation = equation.replace("X=", "");
		int index = 0;
		String sign = "";
		// System.out.println(equation);
		if (alignments.size() == 2) {
			if (equation.indexOf("+") >= 0) {
				index = equation.indexOf("+");
				sign = "+";
			} else if (equation.indexOf("*") >= 0) {
				index = equation.indexOf("*");
				sign = "*";
			} else if (equation.indexOf("-") >= 0) {
				index = equation.indexOf("-");
				sign = "#"; // reverse subtraction
			} else if (equation.indexOf("/") >= 0) {
				index = equation.indexOf("/");
				sign = "$"; // reverse division
			}
			String firstNum = equation.substring(3, index).trim();
			String secondNum = equation.substring(index + 1, equation.length() - 1).trim();
			if (!equation.contains("(")) {
				firstNum = equation.substring(2, index).trim();
				secondNum = equation.substring(index + 1).trim();
			}
			String new_equation = "X=" + secondNum + sign + firstNum;
			if (MathSolverConfig.USE_SUFFIX_X) {
				new_equation = secondNum + sign + firstNum + "=X";
			}
			if (MathSolverConfig.NO_X) {
				new_equation = secondNum + sign + firstNum;
			}
			return new_equation;
		} else if (alignments.size() == 3) {
			ArrayList<Integer> sortedAlignemnts = new ArrayList<>();
			sortedAlignemnts.addAll(alignments);
			Collections.sort(sortedAlignemnts);
			ArrayList<Integer> mapping = new ArrayList<>();
			for (int idx : sortedAlignemnts) {
				int map = alignments.indexOf(idx) + 1;
				mapping.add(map);
			}

			if (equation.indexOf("*") == -1 && equation.indexOf("/") == -1) {
				// System.out.println(alignments);
				// System.out.println(sortedAlignemnts);
				// System.out.println(mapping.toString());
				// System.out.println(equation);
				ArrayList<String> numbers = extractNumbers(equation);
				// System.out.println(numbers);
				if (alignments.get(0) == 0 && alignments.get(1) == 2 && alignments.get(2) == 1) {
					String new_equation = "X=((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")";
					if (MathSolverConfig.USE_SUFFIX_X) {
						new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")=X";
					}
					if (MathSolverConfig.NO_X) {
						new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")";
					}

					return new_equation;
				}
				if (alignments.get(0) == 2 && alignments.get(1) == 0 && alignments.get(2) == 1) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("-")) {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")#" + numbers.get(0)
								+ ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" + numbers.get(0)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" + numbers.get(0) + ")";
						}
						return new_equation;
					} else {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")";
						}
						return new_equation;
					}

				} else if (alignments.get(0) == 2 && alignments.get(1) == 1 && alignments.get(2) == 0) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("-")) {
						// String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")#"
						// + numbers.get(0)
						// + ")";
						// if (MathSolverConfig.USE_SUFFIX_X) {
						// new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
						// numbers.get(0)
						// + ")=X";
						// }
						// if (MathSolverConfig.NO_X) {
						// new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
						// numbers.get(0) + ")";
						// }
						// return new_equation;
					} else {
						String new_equation = "X=(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp + numbers.get(0)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp + numbers.get(0)
									+ "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp + numbers.get(0)
									+ "))";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}

				} else if (alignments.get(0) == 1 && alignments.get(1) == 2 && alignments.get(2) == 0) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("-")) {
						String new_equation = "X=(" + numbers.get(2) + "#(" + numbers.get(0) + firstOp + numbers.get(1)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {

					}
				} else if (alignments.get(0) == 1 && alignments.get(1) == 3 && alignments.get(2) == 2) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("-")) {
						String new_equation = "X=((" + numbers.get(0) + secOp + numbers.get(2) + ")" + firstOp
								+ numbers.get(1) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(0) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(1) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(0) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(1) + ")";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {

					}
				} else {
					System.out.println("----------------------------");
					System.out.println("alignment: " + alignments);
					System.out.println("equation: " + equation);
					throw new RuntimeException("You need to further reconstruct the equation!");
				}
			} else if (equation.indexOf("*") >= 0) {
				ArrayList<String> numbers = extractNumbers(equation);

				// int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new
				// Character[] { '*', '-', '+', '/' }));
				// String firstOp = Character.toString(equation.charAt(firstOpIdx));
				// int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
				// Arrays.asList(new Character[] { '*', '-', '+', '/' }));
				// String secOp = Character.toString(cp_equation.substring(firstOpIdx +
				// 1).charAt(secondOpIdx));
				// if (firstOp.equals("-")) {
				// // String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) +
				// ")#"
				// // + numbers.get(0)
				// // + ")";
				// // if (MathSolverConfig.USE_SUFFIX_X) {
				// // new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
				// // numbers.get(0)
				// // + ")=X";
				// // }
				// // if (MathSolverConfig.NO_X) {
				// // new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
				// // numbers.get(0) + ")";
				// // }
				// // return new_equation;
				// } else {
				// String new_equation = "X=(" + numbers.get(2) + "#(" + numbers.get(1) +
				// firstOp + numbers.get(0)
				// + "))";
				// if (MathSolverConfig.USE_SUFFIX_X) {
				// new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp +
				// numbers.get(0) + "))=X";
				// }
				// if (MathSolverConfig.NO_X) {
				// new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp +
				// numbers.get(0) + "))";
				// }
				// // System.out.println("new equation: " + new_equation);
				// return new_equation;
				// }

				if (alignments.get(0) == 2 && alignments.get(1) == 0 && alignments.get(2) == 1) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("*")) {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {

					}
				} else if (alignments.get(0) == 1 && alignments.get(1) == 2 && alignments.get(2) == 0) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("*")) {
						String new_equation = "X=(" + numbers.get(2) + secOp + "(" + numbers.get(0) + firstOp
								+ numbers.get(1) + "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + secOp + "(" + numbers.get(0) + firstOp
									+ numbers.get(1) + "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + secOp + "(" + numbers.get(0) + firstOp
									+ numbers.get(1) + "))";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {
						System.out.println("----------------------------");
						System.out.println("alignment: " + alignments);
						System.out.println("equation: " + equation);
						throw new RuntimeException("You need to further reconstruct the equation!");
					}
				} else if ((alignments.get(0) == 3 || alignments.get(0) == 2) && alignments.get(1) == 1
						&& alignments.get(2) == 0) {
					// System.out.println("----------------------------");
					// System.out.println("alignment: " + alignments);
					// System.out.println("equation: " + equation);
					// System.out.println("numb: " + numbers);
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("-")) {
						String new_equation = "X=((" + numbers.get(2) + "#" + numbers.get(1) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(2) + "#" + numbers.get(1) + ")" + firstOp + numbers.get(0)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(2) + "#" + numbers.get(1) + ")" + firstOp + numbers.get(0)
									+ ")";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {
						System.out.println("----------------------------");
						System.out.println("alignment: " + alignments);
						System.out.println("equation: " + equation);
						throw new RuntimeException("You need to further reconstruct the equation!");
					}
				} else if (alignments.get(0) == 3 && alignments.get(1) == 1 && alignments.get(2) == 2) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("+")) {
						String new_equation = "X=((" + numbers.get(1) + "+" + numbers.get(2) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + "+" + numbers.get(2) + ")" + firstOp + numbers.get(0)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + "+" + numbers.get(2) + ")" + firstOp + numbers.get(0)
									+ ")";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else {
					return equation;

				}
			} else if (equation.indexOf("/") >= 0) {
				ArrayList<String> numbers = extractNumbers(equation);

				if ((alignments.get(0) == 1 && alignments.get(1) == 2 && alignments.get(2) == 0)
						|| (alignments.get(0) == 1 && alignments.get(1) == 3 && alignments.get(2) == 0)
						|| (alignments.get(0) == 2 && alignments.get(1) == 3 && alignments.get(2) == 0)) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("/")) {
						String new_equation = "X=(" + numbers.get(2) + "$(" + numbers.get(0) + firstOp + numbers.get(1)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else if (alignments.get(0) == 1 && alignments.get(1) == 0 && alignments.get(2) == 2) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("/")) {
						String new_equation = "X=((" + numbers.get(1) + firstOp + numbers.get(0) + ")" + secOp
								+ numbers.get(2) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + firstOp + numbers.get(0) + ")" + secOp
									+ numbers.get(2) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + firstOp + numbers.get(0) + ")" + secOp
									+ numbers.get(2) + ")";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else if ((alignments.get(0) == 2 && alignments.get(1) == 0 && alignments.get(2) == 1)) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("/")) {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")$" + numbers.get(1)
								+ ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(2) + secOp + numbers.get(0) + ")$" + numbers.get(1)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(2) + secOp + numbers.get(0) + ")$" + numbers.get(1) + ")";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else if ((alignments.get(0) == 2 && alignments.get(1) == 1 && alignments.get(2) == 0)) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("/")) {
						String new_equation = "X=(" + numbers.get(2) + "$(" + numbers.get(1) + "#" + numbers.get(0)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(1) + "#" + numbers.get(0) + "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(1) + "#" + numbers.get(0) + "))";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else {
					return equation;
				}

			}
		} else if (alignments.size() >= 4) {
			// ArrayList<String> numbers = extractNumbers(equation);
			// System.out.println("----------------------------");
			// System.out.println("alignment: " + alignments);
			// System.out.println("equation: " + equation);
			// System.out.println("numb: " + numbers);
			return equation;
		}
		return "";
	}

	public static String reformatEquationAll(String equation, ArrayList<Integer> alignments,
			ArrayList<Double> allNumbers) {
		if (MathSolverConfig.NO_REVERSE_OP) {
			if (MathSolverConfig.USE_SUFFIX_X) {
				equation = equation.replace("X=", "");
				equation = equation + "=X";
			}
			if (MathSolverConfig.NO_X) {
				equation = equation.replace("X=", "");
			}
			if (MathSolverConfig.EQUAL_ROOT) {
				equation = equation.replace("X=", "");
				equation = equation + "=";
			}
			return equation;
		}
		if (isSorted(alignments)) {

			if (MathSolverConfig.USE_SUFFIX_X) {
				equation = equation.replace("X=", "");
				equation = equation + "=X";
			}
			if (MathSolverConfig.NO_X) {
				equation = equation.replace("X=", "");
			}
			if (MathSolverConfig.EQUAL_ROOT) {
				equation = equation.replace("X=", "");
				equation = equation + "=";
			}
			return equation;
		}
		// System.out.println("----------------------------");
		// System.out.println("alignment: " + alignments);
		// System.out.println("equation: " + equation);

		// equation = equation.replace("X=", "");
		int index = 0;
		String sign = "";
		// System.out.println(equation);
		if (alignments.size() == 2) {
			if (equation.indexOf("+") >= 0) {
				index = equation.indexOf("+");
				sign = "+";
			} else if (equation.indexOf("*") >= 0) {
				index = equation.indexOf("*");
				sign = "*";
			} else if (equation.indexOf("-") >= 0) {
				index = equation.indexOf("-");
				sign = "#"; // reverse subtraction
			} else if (equation.indexOf("/") >= 0) {
				index = equation.indexOf("/");
				sign = "$"; // reverse division
			}
			String firstNum = equation.substring(3, index).trim();
			String secondNum = equation.substring(index + 1, equation.length() - 1).trim();
			if (!equation.contains("(")) {
				firstNum = equation.substring(2, index).trim();
				secondNum = equation.substring(index + 1).trim();
			}

			String new_equation = "X=" + secondNum + sign + firstNum;
			if (MathSolverConfig.USE_SUFFIX_X) {
				new_equation = secondNum + sign + firstNum + "=X";
			}
			if (MathSolverConfig.NO_X) {
				new_equation = secondNum + sign + firstNum;
			}
			if (MathSolverConfig.EQUAL_ROOT) {
				new_equation = secondNum + sign + firstNum + "=";
			}

			return new_equation;
		} else if (alignments.size() == 3) {
			ArrayList<Integer> sortedAlignemnts = new ArrayList<>();
			sortedAlignemnts.addAll(alignments);
			Collections.sort(sortedAlignemnts);
			ArrayList<Integer> mapping = new ArrayList<>();
			for (int idx : sortedAlignemnts) {
				int map = alignments.indexOf(idx) + 1;
				mapping.add(map);
			}

			if (equation.indexOf("*") == -1 && equation.indexOf("/") == -1) {
				// System.out.println(alignments);
				// System.out.println(sortedAlignemnts);
				// System.out.println(mapping.toString());
				// System.out.println(equation);
				ArrayList<String> numbers = extractNumbers(equation);
				// System.out.println(numbers);
				if (alignments.get(0) == 0 && alignments.get(1) == 2 && alignments.get(2) == 1) {
					String new_equation = "X=((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")";
					if (MathSolverConfig.USE_SUFFIX_X) {
						new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")=X";
					}
					if (MathSolverConfig.NO_X) {
						new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")";
					}
					if (MathSolverConfig.EQUAL_ROOT) {
						new_equation = "((" + numbers.get(0) + "-" + numbers.get(2) + ")+" + numbers.get(1) + ")=";
					}
					return new_equation;
				}
				if (alignments.get(0) == 2 && alignments.get(1) == 0 && alignments.get(2) == 1) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("-")) {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")#" + numbers.get(0)
								+ ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" + numbers.get(0)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" + numbers.get(0) + ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" + numbers.get(0)
									+ ")=";
						}
						return new_equation;
					} else {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")=";
						}
						return new_equation;
					}

				} else if (alignments.get(0) == 2 && alignments.get(1) == 1 && alignments.get(2) == 0) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("-")) {
						// String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")#"
						// + numbers.get(0)
						// + ")";
						// if (MathSolverConfig.USE_SUFFIX_X) {
						// new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
						// numbers.get(0)
						// + ")=X";
						// }
						// if (MathSolverConfig.NO_X) {
						// new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
						// numbers.get(0) + ")";
						// }
						// return new_equation;
					} else {
						String new_equation = "X=(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp + numbers.get(0)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp + numbers.get(0)
									+ "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp + numbers.get(0)
									+ "))";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp + numbers.get(0)
									+ "))=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}

				} else if (alignments.get(0) == 1 && alignments.get(1) == 2 && alignments.get(2) == 0) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("-")) {
						String new_equation = "X=(" + numbers.get(2) + "#(" + numbers.get(0) + firstOp + numbers.get(1)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "(" + numbers.get(2) + "#(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {

					}
				} else if (alignments.get(0) == 1 && alignments.get(1) == 3 && alignments.get(2) == 2) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("-")) {
						String new_equation = "X=((" + numbers.get(0) + secOp + numbers.get(2) + ")" + firstOp
								+ numbers.get(1) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(0) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(1) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(0) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(1) + ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(0) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(1) + ")=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {

					}
				} else {
					System.out.println("----------------------------");
					System.out.println("alignment: " + alignments);
					System.out.println("equation: " + equation);
					throw new RuntimeException("You need to further reconstruct the equation!");
				}
			} else if (equation.indexOf("*") >= 0) {
				ArrayList<String> numbers = extractNumbers(equation);

				// int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new
				// Character[] { '*', '-', '+', '/' }));
				// String firstOp = Character.toString(equation.charAt(firstOpIdx));
				// int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
				// Arrays.asList(new Character[] { '*', '-', '+', '/' }));
				// String secOp = Character.toString(cp_equation.substring(firstOpIdx +
				// 1).charAt(secondOpIdx));
				// if (firstOp.equals("-")) {
				// // String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) +
				// ")#"
				// // + numbers.get(0)
				// // + ")";
				// // if (MathSolverConfig.USE_SUFFIX_X) {
				// // new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
				// // numbers.get(0)
				// // + ")=X";
				// // }
				// // if (MathSolverConfig.NO_X) {
				// // new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")#" +
				// // numbers.get(0) + ")";
				// // }
				// // return new_equation;
				// } else {
				// String new_equation = "X=(" + numbers.get(2) + "#(" + numbers.get(1) +
				// firstOp + numbers.get(0)
				// + "))";
				// if (MathSolverConfig.USE_SUFFIX_X) {
				// new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp +
				// numbers.get(0) + "))=X";
				// }
				// if (MathSolverConfig.NO_X) {
				// new_equation = "(" + numbers.get(2) + "#(" + numbers.get(1) + firstOp +
				// numbers.get(0) + "))";
				// }
				// // System.out.println("new equation: " + new_equation);
				// return new_equation;
				// }

				if (alignments.get(0) == 2 && alignments.get(1) == 0 && alignments.get(2) == 1) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("*")) {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(1) + secOp + numbers.get(2) + ")" + firstOp
									+ numbers.get(0) + ")=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {

					}
				} else if (alignments.get(0) == 1 && alignments.get(1) == 2 && alignments.get(2) == 0) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("*")) {
						String new_equation = "X=(" + numbers.get(2) + secOp + "(" + numbers.get(0) + firstOp
								+ numbers.get(1) + "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + secOp + "(" + numbers.get(0) + firstOp
									+ numbers.get(1) + "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + secOp + "(" + numbers.get(0) + firstOp
									+ numbers.get(1) + "))";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "(" + numbers.get(2) + secOp + "(" + numbers.get(0) + firstOp
									+ numbers.get(1) + "))=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {
						System.out.println("----------------------------");
						System.out.println("alignment: " + alignments);
						System.out.println("equation: " + equation);
						throw new RuntimeException("You need to further reconstruct the equation!");
					}
				} else if ((alignments.get(0) == 3 || alignments.get(0) == 2) && alignments.get(1) == 1
						&& alignments.get(2) == 0) {
					// System.out.println("----------------------------");
					// System.out.println("alignment: " + alignments);
					// System.out.println("equation: " + equation);
					// System.out.println("numb: " + numbers);
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("-")) {
						String new_equation = "X=((" + numbers.get(2) + "#" + numbers.get(1) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(2) + "#" + numbers.get(1) + ")" + firstOp + numbers.get(0)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(2) + "#" + numbers.get(1) + ")" + firstOp + numbers.get(0)
									+ ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(2) + "#" + numbers.get(1) + ")" + firstOp + numbers.get(0)
									+ ")=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					} else {
						System.out.println("----------------------------");
						System.out.println("alignment: " + alignments);
						System.out.println("equation: " + equation);
						throw new RuntimeException("You need to further reconstruct the equation!");
					}
				} else if (alignments.get(0) == 3 && alignments.get(1) == 1 && alignments.get(2) == 2) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '*' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '*' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("+")) {
						String new_equation = "X=((" + numbers.get(1) + "+" + numbers.get(2) + ")" + firstOp
								+ numbers.get(0) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + "+" + numbers.get(2) + ")" + firstOp + numbers.get(0)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + "+" + numbers.get(2) + ")" + firstOp + numbers.get(0)
									+ ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(1) + "+" + numbers.get(2) + ")" + firstOp + numbers.get(0)
									+ ")=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else {
					return equation;

				}
			} else if (equation.indexOf("/") >= 0) {
				ArrayList<String> numbers = extractNumbers(equation);

				if ((alignments.get(0) == 1 && alignments.get(1) == 2 && alignments.get(2) == 0)
						|| (alignments.get(0) == 1 && alignments.get(1) == 3 && alignments.get(2) == 0)
						|| (alignments.get(0) == 2 && alignments.get(1) == 3 && alignments.get(2) == 0)) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("/")) {
						String new_equation = "X=(" + numbers.get(2) + "$(" + numbers.get(0) + firstOp + numbers.get(1)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(0) + firstOp + numbers.get(1)
									+ "))=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else if (alignments.get(0) == 1 && alignments.get(1) == 0 && alignments.get(2) == 2) {
					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("/")) {
						String new_equation = "X=((" + numbers.get(1) + firstOp + numbers.get(0) + ")" + secOp
								+ numbers.get(2) + ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(1) + firstOp + numbers.get(0) + ")" + secOp
									+ numbers.get(2) + ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(1) + firstOp + numbers.get(0) + ")" + secOp
									+ numbers.get(2) + ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(1) + firstOp + numbers.get(0) + ")" + secOp
									+ numbers.get(2) + ")=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else if ((alignments.get(0) == 2 && alignments.get(1) == 0 && alignments.get(2) == 1)) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (firstOp.equals("/")) {
						String new_equation = "X=((" + numbers.get(1) + secOp + numbers.get(2) + ")$" + numbers.get(1)
								+ ")";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "((" + numbers.get(2) + secOp + numbers.get(0) + ")$" + numbers.get(1)
									+ ")=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "((" + numbers.get(2) + secOp + numbers.get(0) + ")$" + numbers.get(1) + ")";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "((" + numbers.get(2) + secOp + numbers.get(0) + ")$" + numbers.get(1)
									+ ")=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else if ((alignments.get(0) == 2 && alignments.get(1) == 1 && alignments.get(2) == 0)) {

					String cp_equation = equation;
					int firstOpIdx = indexOfFirstMathOp(cp_equation, Arrays.asList(new Character[] { '+', '-', '/' }));
					String firstOp = Character.toString(equation.charAt(firstOpIdx));
					int secondOpIdx = indexOfFirstMathOp(cp_equation.substring(firstOpIdx + 1),
							Arrays.asList(new Character[] { '+', '-', '/' }));
					String secOp = Character.toString(cp_equation.substring(firstOpIdx + 1).charAt(secondOpIdx));
					if (secOp.equals("/")) {
						String new_equation = "X=(" + numbers.get(2) + "$(" + numbers.get(1) + "#" + numbers.get(0)
								+ "))";
						if (MathSolverConfig.USE_SUFFIX_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(1) + "#" + numbers.get(0) + "))=X";
						}
						if (MathSolverConfig.NO_X) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(1) + "#" + numbers.get(0) + "))";
						}
						if (MathSolverConfig.EQUAL_ROOT) {
							new_equation = "(" + numbers.get(2) + "$(" + numbers.get(1) + "#" + numbers.get(0) + "))=";
						}
						// System.out.println("new equation: " + new_equation);
						return new_equation;
					}
				} else {
					return equation;
				}

			}
		} else if (alignments.size() >= 4) {
			// ArrayList<String> numbers = extractNumbers(equation);
			// System.out.println("----------------------------");
			// System.out.println("alignment: " + alignments);
			// System.out.println("equation: " + equation);
			// System.out.println("numb: " + numbers);
			return equation;
		}
		return "";
	}

	public static boolean isSorted(ArrayList<Integer> alignments) {
		boolean sorted = true;
		for (int i = 1; i < alignments.size(); i++) {
			if (alignments.get(i - 1).compareTo(alignments.get(i)) > 0)
				sorted = false;
		}

		return sorted;
	}

	public static ArrayList<String> extractNumbers(String equation) {
		ArrayList<String> numbers = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for (char c : equation.toCharArray()) {
			if (Character.isDigit(c) || c == '.') {
				sb.append(c);
			} else {
				if (sb.length() > 0) {
					numbers.add(sb.toString());
					sb = new StringBuilder();
				}

			}
		}
		// System.out.println(numbers);
		return numbers;
	}

	public static ArrayList<Variable> readGroundingVariables(String filename) throws IOException {
		ArrayList<Variable> variables = new ArrayList<>();
		BufferedReader scan = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		String line;
		while ((line = scan.readLine()) != null) {
			String[] segments = line.trim().split("\t");
			String[] indexes = segments[1].trim().split("\\s");
			int start = Integer.parseInt(indexes[1]);
			int end = Integer.parseInt(indexes[2]);
			String grounding = segments[2];
			Variable var = new Variable(indexes[0], start, end, grounding);
			variables.add(var);
		}
		return variables;
	}

	public static SemanticForest toMathExpressionTree(int id, String equation, SemTextDataManager dm) {

		// SemanticForestNode[] nodes = new SemanticForestNode[prods_form.size()];
		ArrayList<SemanticForestNode> nodes = new ArrayList<>();
		toMathExpressionSemanticNode(equation, 0, nodes, dm, 1);
		// set node ids, where parent's id must larger than its children's id
		// DFS parent node followed by children node
		for (int k = nodes.size() - 1; k >= 0; k--) {
			nodes.get(k).setId(nodes.size() - 1 - k);
		}

		// add the root unit.
		// nodes[0] is a root unit
		if (id >= 0) {
			dm.recordRootUnit(nodes.get(0).getUnit());
		} else {
			// initial corpus
			nodes.get(0).getUnit().setContextIndependent();
		}

		SemanticForestNode root = SemanticForestNode.createRootNode(MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH);
		root.setChildren(0, new SemanticForestNode[] { nodes.get(0) });

		// //if it's the prior instance, then set it as context independent.
		// if(id<0){
		// nodes[0].getUnit().setContextIndependent();
		// }

		if (id >= 0) {
			addValidUnitPairs(root, dm);
		}
		SemanticForest tree = new SemanticForest(root);
		return tree;
	}

	private static void toMathExpressionSemanticNode(String equation, int pos, ArrayList<SemanticForestNode> nodes,
			SemTextDataManager dm, int depth) {
		// System.out.println("before: " + equation);
		int maxDepth = MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH;
		equation = equation.trim();
		int index = equation.indexOf("=");
		SemanticUnit unit;
		SemanticForestNode node;
		if (!MathSolverConfig.EQUAL_ROOT && index != -1) {
			// System.out.println("are you here?");
			unit = toSemanticUnit("EQU", dm);// dm.toSemanticUnit("=");
			node = new SemanticForestNode(unit, maxDepth - depth);
			node.setValueString("=");
			nodes.add(node);
			toMathExpressionSemanticNode(equation.substring(0, index), pos + 1, nodes, dm, depth + 1);
			int num_children = nodes.get(pos + 1).countAllChildren() + 1;
			toMathExpressionSemanticNode(equation.substring(index + 1), pos + num_children, nodes, dm, depth + 1);
			node.setChildren(0, new SemanticForestNode[] { nodes.get(pos + 1) });
			node.setChildren(1, new SemanticForestNode[] { nodes.get(pos + num_children) });

			return;
		} else if (MathSolverConfig.EQUAL_ROOT && index != -1) {
			unit = toSemanticUnit("EQUAL_ROOT", dm);// dm.toSemanticUnit("=");
			node = new SemanticForestNode(unit, maxDepth - depth);
			node.setValueString("=");
			nodes.add(node);
			toMathExpressionSemanticNode(equation.substring(0, index), pos + 1, nodes, dm, depth + 1);
			node.setChildren(0, new SemanticForestNode[] { nodes.get(pos + 1) });
			// int num_children = nodes.get(pos + 1).countAllChildren() + 1;
			// toMathExpressionSemanticNode(equation.substring(index + 1), pos +
			// num_children, nodes, dm, depth + 1);

			// node.setChildren(1, new SemanticForestNode[] { nodes.get(pos + num_children)
			// });
			return;
		}
		if (equation.charAt(0) == '(' && equation.charAt(equation.length() - 1) == ')') {
			equation = equation.substring(1, equation.length() - 1);
		}
		// System.out.println("after: " + equation);
		index = indexOfMathOp(equation, Arrays.asList('+', '-', '*', '/', '#', '$'));
		if (index > 0) {
			// System.out.println("you are here");
			if (equation.charAt(index) == '+') {
				unit = toSemanticUnit("ADD", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("+");
			} else if (equation.charAt(index) == '#') {
				unit = toSemanticUnit("SUB_R", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("#");
				// System.err.println("#");
			} else if (equation.charAt(index) == '-') {
				unit = toSemanticUnit("SUB", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("-");
			} else if (equation.charAt(index) == '*') {
				unit = toSemanticUnit("MUL", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("*");
			} else if (equation.charAt(index) == '$') {
				unit = toSemanticUnit("DVI_R", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("$");
				// System.err.println("$");
			} else if (equation.charAt(index) == '/') {
				unit = toSemanticUnit("DVI", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("/");
			} else {
				throw new RuntimeException("unknown operations");
			}
			nodes.add(node);
			toMathExpressionSemanticNode(equation.substring(0, index), pos + 1, nodes, dm, depth + 1);
			int num_children = nodes.get(pos + 1).countAllChildren() + 1;
			toMathExpressionSemanticNode(equation.substring(index + 1), pos + num_children, nodes, dm, depth + 1);
			node.setChildren(0, new SemanticForestNode[] { nodes.get(pos + 1) });
			node.setChildren(1, new SemanticForestNode[] { nodes.get(pos + num_children) });
			return;
		} else {
			if (equation.contains("X")) {
				unit = toSemanticUnit("X", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setVarId(equation.trim());
				node.setValueString("X");
				nodes.add(node);
			} else {
				// System.out.println("num " + equation);
				unit = toSemanticUnit("NUM", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setNumValue(Double.parseDouble(equation.trim()));
				nodes.add(node);
				node.setValueString(equation.trim());
			}
		}
		// height starts from 1
		if (maxDepth - depth <= 0)
			throw new RuntimeException("The depth is " + depth + "!");
	}

	public static SemanticForest toTree(int id, String equation, SemTextDataManager dm) {

		// SemanticForestNode[] nodes = new SemanticForestNode[prods_form.size()];
		ArrayList<SemanticForestNode> nodes = new ArrayList<>();
		toSemanticNode(equation, 0, nodes, dm, 1);
		// set node ids, where parent's id must larger than its children's id
		// DFS parent node followed by children node
		for (int k = nodes.size() - 1; k >= 0; k--) {
			nodes.get(k).setId(nodes.size() - 1 - k);
		}

		// add the root unit.
		// nodes[0] is a root unit
		if (id >= 0) {
			dm.recordRootUnit(nodes.get(0).getUnit());
		} else {
			// initial corpus
			nodes.get(0).getUnit().setContextIndependent();
		}

		SemanticForestNode root = SemanticForestNode.createRootNode(EquationParserConfig._SEMANTIC_FOREST_MAX_DEPTH);
		root.setChildren(0, new SemanticForestNode[] { nodes.get(0) });

		// //if it's the prior instance, then set it as context independent.
		// if(id<0){
		// nodes[0].getUnit().setContextIndependent();
		// }

		if (id >= 0) {
			addValidUnitPairs(root, dm);
		}
		SemanticForest tree = new SemanticForest(root);
		return tree;
	}

	// create the global forest.
	// bottom-up approach.
	public static SemanticForest toForest(SemTextDataManager dm) {
		boolean checkValidPair = true;

		if (NetworkConfig.EquationParser) {
			checkValidPair = EquationParserConfig.checkValidPair;
			NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH = EquationParserConfig._SEMANTIC_FOREST_MAX_DEPTH;
		} else if (NetworkConfig.MathSolver) {
			checkValidPair = MathSolverConfig.checkValidPair;
			NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH = MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH;
		}

		ArrayList<SemanticUnit> units = dm.getAllUnits();

		ArrayList<SemanticForestNode> nodes_at_prev_depth = new ArrayList<>();

		for (int dIndex = 1; dIndex < NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH; dIndex++) {

			ArrayList<SemanticForestNode> nodes_at_curr_depth = new ArrayList<>();

			for (int wIndex = 0; wIndex < units.size(); wIndex++) {
				SemanticUnit unit = units.get(wIndex);
				SemanticForestNode node = new SemanticForestNode(unit, dIndex);

				if (node.arity() == 0) {
					// always adds it since it does not require children.
					nodes_at_curr_depth.add(node);
				} else if (node.arity() == 1) {
					ArrayList<SemanticForestNode> node_children_0 = new ArrayList<>();

					for (int k = 0; k < nodes_at_prev_depth.size(); k++) {
						SemanticForestNode node_child = nodes_at_prev_depth.get(k);
						if (checkValidPair) {
							if (dm.isValidUnitPair(node.getUnit(), node_child.getUnit(), 0))
								node_children_0.add(node_child);
						} else {
							// if (node_child.getUnit().getLHS().equals(node.getUnit().getRHS()[0]))
							node_children_0.add(node_child);
						}
					}

					if (node_children_0.size() == 0) {
						// ignore since the children is empty..
					} else {
						SemanticForestNode[] children0 = new SemanticForestNode[node_children_0.size()];
						for (int k = 0; k < children0.length; k++) {
							children0[k] = node_children_0.get(k);
						}
						node.setChildren(0, children0);
						nodes_at_curr_depth.add(node);
					}

				} else if (node.arity() == 2) {
					ArrayList<SemanticForestNode> node_children_0 = new ArrayList<>();
					ArrayList<SemanticForestNode> node_children_1 = new ArrayList<>();
					for (int k = 0; k < nodes_at_prev_depth.size(); k++) {
						SemanticForestNode node_child = nodes_at_prev_depth.get(k);
						if (checkValidPair) {
							if (dm.isValidUnitPair(node.getUnit(), node_child.getUnit(), 0))
								node_children_0.add(node_child);
						} else {
							// if (node_child.getUnit().getLHS().equals(node.getUnit().getRHS()[0]))
							node_children_0.add(node_child);
						}

						if (checkValidPair) {
							if (dm.isValidUnitPair(node.getUnit(), node_child.getUnit(), 1))
								node_children_1.add(node_child);
						} else {
							// if (node_child.getUnit().getLHS().equals(node.getUnit().getRHS()[1]))
							node_children_1.add(node_child);
						}
					}

					if (node_children_0.size() == 0 || node_children_1.size() == 0) {
						// ignore since some children can not be found..
					} else {
						SemanticForestNode[] children0 = new SemanticForestNode[node_children_0.size()];
						for (int k = 0; k < children0.length; k++) {
							children0[k] = node_children_0.get(k);
						}
						node.setChildren(0, children0);

						SemanticForestNode[] children1 = new SemanticForestNode[node_children_1.size()];
						for (int k = 0; k < children1.length; k++) {
							children1[k] = node_children_1.get(k);
						}
						node.setChildren(1, children1);

						nodes_at_curr_depth.add(node);
					}

				} else {
					throw new RuntimeException("The arity is " + node.arity());
				}
			}

			nodes_at_prev_depth = nodes_at_curr_depth;
		}

		System.err.println(nodes_at_prev_depth.size());

		SemanticForestNode root = SemanticForestNode.createRootNode(NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH);

		ArrayList<SemanticUnit> rootUnits = dm.getRootUnits();
		ArrayList<SemanticForestNode> rootNodes = new ArrayList<>();
		for (SemanticForestNode node : nodes_at_prev_depth) {
			SemanticUnit unit = node.getUnit();
			if (rootUnits.contains(unit)) {
				rootNodes.add(node);
			}
		}
		SemanticForestNode[] roots = new SemanticForestNode[rootNodes.size()];
		for (int k = 0; k < roots.length; k++)
			roots[k] = rootNodes.get(k);
		root.setChildren(0, roots);

		SemanticForest forest = new SemanticForest(root);

		return forest;
	}

	public static int indexOfMathOp(String equationString, List<Character> keys) {
		for (int index = 0; index < equationString.length(); ++index) {
			if (keys.contains(equationString.charAt(index))) {
				int open = 0, close = 0;
				for (int i = index; i >= 0; --i) {
					if (equationString.charAt(i) == ')')
						close++;
					if (equationString.charAt(i) == '(')
						open++;
				}
				if (open == close) {
					return index;
				}
			}
		}
		return -1;
	}

	public static int indexOfFirstMathOp(String equationString, List<Character> keys) {
		for (int index = 0; index < equationString.length(); ++index) {
			if (keys.contains(equationString.charAt(index))) {
				return index;
			}
		}
		return -1;
	}

	private static void toSemanticNode(String equation, int pos, ArrayList<SemanticForestNode> nodes,
			SemTextDataManager dm, int depth) {
		int maxDepth = EquationParserConfig._SEMANTIC_FOREST_MAX_DEPTH;
		equation = equation.trim();
		int index = equation.indexOf("=");
		SemanticUnit unit;
		SemanticForestNode node;
		if (index != -1) {
			unit = toSemanticUnit("EQU", dm);// dm.toSemanticUnit("=");
			node = new SemanticForestNode(unit, maxDepth - depth);
			node.setValueString("=");
			nodes.add(node);
			toSemanticNode(equation.substring(0, index), pos + 1, nodes, dm, depth + 1);
			int num_children = nodes.get(pos + 1).countAllChildren() + 1;
			toSemanticNode(equation.substring(index + 1), pos + num_children, nodes, dm, depth + 1);
			node.setChildren(0, new SemanticForestNode[] { nodes.get(pos + 1) });
			node.setChildren(1, new SemanticForestNode[] { nodes.get(pos + num_children) });
			return;
		}
		if (equation.charAt(0) == '(' && equation.charAt(equation.length() - 1) == ')') {
			equation = equation.substring(1, equation.length() - 1);
		}
		index = indexOfMathOp(equation, Arrays.asList('+', '-', '*', '/'));
		if (index > 0) {
			if (equation.charAt(index) == '+') {
				unit = toSemanticUnit("ADD", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("+");
			} else if (equation.charAt(index) == '-' && equation.charAt(index + 1) == 'r') {
				unit = toSemanticUnit("SUB_R", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("-r");
			} else if (equation.charAt(index) == '-') {
				unit = toSemanticUnit("SUB", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("-");
			} else if (equation.charAt(index) == '*') {
				unit = toSemanticUnit("MUL", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("*");
			} else if (equation.charAt(index) == '/' && equation.charAt(index + 1) == 'r') {
				unit = toSemanticUnit("DVI_R", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("/r");
			} else if (equation.charAt(index) == '/') {
				unit = toSemanticUnit("DVI", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setValueString("/");
			} else {
				throw new RuntimeException("unknown operations");
			}
			nodes.add(node);
			toSemanticNode(equation.substring(0, index), pos + 1, nodes, dm, depth + 1);
			int num_children = nodes.get(pos + 1).countAllChildren() + 1;
			toSemanticNode(equation.substring(index + 1), pos + num_children, nodes, dm, depth + 1);
			node.setChildren(0, new SemanticForestNode[] { nodes.get(pos + 1) });
			node.setChildren(1, new SemanticForestNode[] { nodes.get(pos + num_children) });
			return;
		} else {
			if (equation.contains("V")) {
				unit = toSemanticUnit("VAR", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setVarId(equation.trim());
				node.setValueString("V");
				nodes.add(node);
			} else {
				unit = toSemanticUnit("NUM", dm);
				node = new SemanticForestNode(unit, maxDepth - depth);
				node.setNumValue(Double.parseDouble(equation.trim()));
				nodes.add(node);
				node.setValueString(equation.trim());
			}
		}
		// height starts from 1
		if (maxDepth - depth <= 0)
			throw new RuntimeException("The depth is " + depth + "!");
	}

	public static SemanticForest toTree(int id, ArrayList<String> prods_form, SemTextDataManager dm) {

		SemanticForestNode[] nodes = new SemanticForestNode[prods_form.size()];
		toSemanticNode(prods_form, 0, nodes, dm, 1);

		for (int k = nodes.length - 1; k >= 0; k--) {
			nodes[k].setId(nodes.length - 1 - k);
		}

		// add the root unit.
		if (id >= 0) {
			dm.recordRootUnit(nodes[0].getUnit());
		} else {
			nodes[0].getUnit().setContextIndependent();
		}

		SemanticForestNode root = SemanticForestNode.createRootNode(NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH);
		root.setChildren(0, new SemanticForestNode[] { nodes[0] });

		// //if it's the prior instance, then set it as context independent.
		// if(id<0){
		// nodes[0].getUnit().setContextIndependent();
		// }

		if (id >= 0) {
			addValidUnitPairs(root, dm);
		}

		SemanticForest tree = new SemanticForest(root);

		return tree;

	}

	private static void addValidUnitPairs(SemanticForestNode parent, SemTextDataManager dm) {
		SemanticForestNode[][] children = parent.getChildren();
		for (int k = 0; k < children.length; k++) {
			SemanticForestNode child = children[k][0];
			dm.addValidUnitPair(parent.getUnit(), child.getUnit(), k);
			addValidUnitPairs(child, dm);
		}
	}

	private static void toSemanticNode(ArrayList<String> prods_form, int pos, SemanticForestNode[] nodes,
			SemTextDataManager dm, int depth) {

		int maxDepth = NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH;

		String prod_form = prods_form.get(pos);
		SemanticUnit unit = toSemanticUnit(prod_form, dm);
		if (maxDepth - depth <= 0)
			throw new RuntimeException("The depth is " + depth + "!");

		SemanticForestNode node = new SemanticForestNode(unit, maxDepth - depth);
		nodes[pos] = node;

		if (node.arity() == 0) {
		} else if (node.arity() == 1) {
			toSemanticNode(prods_form, pos + 1, nodes, dm, depth + 1);
			node.setChildren(0, new SemanticForestNode[] { nodes[pos + 1] });
		} else if (node.arity() == 2) {
			toSemanticNode(prods_form, pos + 1, nodes, dm, depth + 1);
			int num_children = nodes[pos + 1].countAllChildren() + 1;
			toSemanticNode(prods_form, pos + num_children, nodes, dm, depth + 1);
			node.setChildren(0, new SemanticForestNode[] { nodes[pos + 1] });
			node.setChildren(1, new SemanticForestNode[] { nodes[pos + num_children] });
		}

	}

	private static SemanticUnit toSemanticUnit(String form, SemTextDataManager dm) {
		int arity = NodeTypes.valueOf(form).getArity();
		SemanticUnit unit = dm.toSemanticUnit(form);
		unit.setArity(arity);
		return unit;
	}

	// *n:Query -> ({ answer ( *n:State ) })
	// private static SemanticUnit toSemanticUnit(String form, SemTextDataManager
	// dm) {
	//
	// String mrl = form;
	// int index;
	//
	// index = form.indexOf("({");
	// String rhs_string = form.substring(index + 2).trim();
	// index = rhs_string.lastIndexOf("})");
	// rhs_string = rhs_string.substring(0, index).trim();
	// // System.err.println(rhs_string);
	// String[] rhs_tokens = rhs_string.split("\\s");
	// // System.exit(1);
	//
	// index = form.indexOf("->");
	// String lhs = form.substring(0, index).trim();
	// form = form.substring(index + 2).trim();
	// form = form.substring(2, form.length() - 2);
	// index = form.lastIndexOf("(");
	// String name[];
	// if (index == -1) {
	// name = new String[] { form };
	// } else {
	// name = form.substring(0, index).trim().split("\\(");
	// }
	// String[] tokens = form.substring(index + 1).trim().split("\\s");
	// ArrayList<String> tokens_s = new ArrayList<>();
	// for (String token : tokens) {
	// if (token.startsWith("*")) {
	// tokens_s.add(token);
	// }
	// }
	// String[] rhs = new String[tokens_s.size()];
	// for (int k = 0; k < rhs.length; k++) {
	// rhs[k] = tokens_s.get(k);
	// }
	// SemanticUnit unit = dm.toSemanticUnit(lhs, name, rhs, mrl, rhs_tokens);
	// // rhs: debug
	// // if (name.length > 1 || rhs.length > 1) {
	// // System.out.println("mrl: "+mrl);
	// // System.out.println("lhs: "+lhs);
	// // System.out.println("name: "+Arrays.toString(name));
	// // System.out.println("rhs: "+Arrays.toString(rhs));
	// // System.out.println("rhs_tokens: "+Arrays.toString(rhs_tokens));
	// // }
	// return unit;
	//
	// }

	public static boolean checkEquivalentEquation(String gold, String prediction) throws ScriptException {
		String uni_gold = uniformEquationString(gold);
		ArrayList<Double> gold_coe = getFactors(uni_gold);
		String uni_prediction = uniformEquationString(prediction);
		ArrayList<Double> pred_coe = getFactors(uni_prediction);
		if (gold_coe.size() != pred_coe.size())
			return false;
		boolean coeflag = false;
		boolean twoVar = false;
		boolean reverse = false;
		// System.out.println(gold_coe);
		// System.out.println(pred_coe);
		// System.out.println(Math.abs(gold_coe.get(0) - pred_coe.get(0)));
		if (gold_coe.size() == 1) {
			if (Math.abs(gold_coe.get(0) - pred_coe.get(0)) > 0.00001) {
				// if (Math.abs(gold_coe.get(0) + pred_coe.get(0)) > 0.00001) {
				// coeflag = true;
				// reverse = true;
				// }
				coeflag = false;
			} else {
				coeflag = true;
			}
		} else {
			double gold_coe1 = gold_coe.get(0);
			double gold_coe2 = gold_coe.get(1);
			double pred_coe1 = pred_coe.get(0);
			double pred_coe2 = pred_coe.get(1);
			if (Math.abs(gold_coe1 - pred_coe1) < 0.00001 || Math.abs(gold_coe2 - pred_coe2) < 0.00001) {
				reverse = false;
				coeflag = true;
			} else if (Math.abs(gold_coe1 - pred_coe2) < 0.00001 || Math.abs(gold_coe2 - pred_coe1) < 0.00001) {
				// System.out.print("true");
				coeflag = true;
				reverse = true;
			} else {
			}
			twoVar = true;
			// if (!coeflag) {
			// if (Math.abs(gold_coe1 + pred_coe1) > 0.00001 || Math.abs(gold_coe2 +
			// pred_coe2) > 0.00001) {
			// coeflag = true;
			// reverse = true;
			// } else if (Math.abs(gold_coe1 + pred_coe2) > 0.00001 || Math.abs(gold_coe2 +
			// pred_coe1) > 0.00001) {
			// coeflag = true;
			// reverse = true;
			// } else {
			// }
			// }
		}
		if (!coeflag) {
			return false;
		}
		// boolean r = checkExpressionValue(uni_gold, uni_prediction, twoVar, reverse);
		// System.err.println(r);
		// return r;

		uni_gold = uni_gold.replaceAll("V", "1");
		uni_prediction = uni_prediction.replaceAll("V", "1");
		System.out.println(uni_gold);
		System.out.println(uni_prediction);
		// ScriptEngineManager mgr = new ScriptEngineManager();
		// ScriptEngine engine = mgr.getEngineByName("JavaScript");
		// double sol_gold = (Double) engine.eval(uni_gold);
		// double sol_pred = (Double) engine.eval(uni_prediction);
		//
		if (!isValidEquation(uni_gold) || !isValidEquation(uni_prediction))
			return false;
		Expression gold_expression = new Expression(uni_gold);
		gold_expression.setPrecision(5);
		Expression pred_expression = new Expression(uni_prediction);
		pred_expression.setPrecision(5);
		BigDecimal gold_result = gold_expression.eval();
		BigDecimal pred_result = pred_expression.eval();
		double sol_gold = gold_result.doubleValue();
		double sol_pred = pred_result.doubleValue();
		// System.out.println(gold_result.doubleValue());
		System.err.println(sol_gold);
		System.err.println(sol_pred);
		// if (reverse && Math.abs(sol_gold + sol_pred) < 0.0001) {
		// return true;
		// } else if (!reverse && Math.abs(sol_gold - sol_pred) < 0.0001) {
		// return true;
		// }
		if (Math.abs(sol_gold - sol_pred) < 0.0001 || Math.abs(sol_gold + sol_pred) < 0.0001) {
			return true;
		}
		return false;

	}

	public static boolean checkExpressionValue(String exp1, String exp2, boolean twoVar, boolean reverse) {
		Random rand = new Random(System.currentTimeMillis());

		int testTime = 100;
		while (testTime > 0) {
			String new_exp1 = exp1, new_exp2 = exp2;
			if (!twoVar) { // only one variable
				// int n = rand.nextInt(testTime) + 1
				int n = (int) (Math.random() * testTime + 1);
				String value = n + "";
				new_exp1 = new_exp1.replaceAll("V", value);
				new_exp2 = new_exp2.replaceAll("V", value);

			} else { // two variable
				if (reverse) { // V1, V2 are in the same order
					int index1 = new_exp1.indexOf('V');
					int index2 = new_exp2.indexOf('V');
					int n1 = rand.nextInt(1000) + 1;
					int n2 = rand.nextInt(1000) + 1;
					String value1 = n1 + "";
					String value2 = n2 + "";
					new_exp1 = new_exp1.substring(0, index1) + value1 + new_exp1.substring(index1 + 1);
					new_exp2 = new_exp2.substring(0, index2) + value1 + new_exp2.substring(index2 + 1);
					index1 = new_exp1.indexOf('V');
					index2 = new_exp2.indexOf('V');
					new_exp1 = new_exp1.substring(0, index1) + value1 + new_exp1.substring(index1 + 1);
					new_exp2 = new_exp2.substring(0, index2) + value1 + new_exp2.substring(index2 + 1);
					// System.out.println(new_exp1 + " " + value1);
					// System.out.println(new_exp2);

				} else {
					int index1 = new_exp1.indexOf('V');
					int index2 = new_exp2.indexOf('V');
					int n1 = rand.nextInt(1000) + 1;
					int n2 = rand.nextInt(1000) + 1;
					String value1 = n1 + "";
					String value2 = n2 + "";
					new_exp1 = new_exp1.substring(0, index1) + value1 + new_exp1.substring(index1 + 1);
					new_exp2 = new_exp2.substring(0, index2) + value1 + new_exp2.substring(index2 + 1);
					index1 = new_exp1.indexOf('V');
					index2 = new_exp2.indexOf('V');
					new_exp1 = new_exp1.substring(0, index1) + value1 + new_exp1.substring(index1 + 1);
					new_exp2 = new_exp2.substring(0, index2) + value1 + new_exp2.substring(index2 + 1);

				}

			}

			// ScriptEngineManager mgr = new ScriptEngineManager();
			// ScriptEngine engine = mgr.getEngineByName("JavaScript");
			// double sol_gold = (Double) engine.eval(uni_gold);
			// double sol_pred = (Double) engine.eval(uni_prediction);
			//
			if (!isValidEquation(new_exp1) || !isValidEquation(new_exp2))
				return false;

			Expression gold_expression = new Expression(new_exp1);
			gold_expression.setPrecision(5);
			Expression pred_expression = new Expression(new_exp2);
			pred_expression.setPrecision(5);
			BigDecimal gold_result = gold_expression.eval();
			BigDecimal pred_result = pred_expression.eval();
			double sol_gold = gold_result.doubleValue();
			double sol_pred = pred_result.doubleValue();
			if (Math.abs(sol_gold - sol_pred) < 0.0001 || Math.abs(sol_gold + sol_pred) < 0.0001) {
				// return true;
			} else {
				return false;
			}
			testTime--;
		}

		return true;
	}

	public static String uniformEquationString(String equation) {
		StringBuilder sb = new StringBuilder();
		equation = equation.trim();
		int index = equation.indexOf("=");
		sb.append(equation.substring(0, index));

		equation = equation.substring(index + 1);
		// System.out.println(equation);
		while (!equation.equals("")) {
			index = indexOfMathOp(equation, Arrays.asList('+', '-'));
			if (index > 0) {
				if (equation.charAt(index) == '+') {
					sb.append("-" + equation.substring(0, index));
					sb.append("-" + equation.substring(index + 1));
				} else if (equation.charAt(index) == '-') {
					sb.append("-" + equation.substring(0, index));
					sb.append("+" + equation.substring(index + 1));
				}
			} else {
				sb.append("-" + equation);
			}
			equation = "";
		}
		// sb.append("=0");
		// System.out.println(sb);

		return sb.toString();
	}

	// public static void getFactors(String equation, SemTextDataManager dm) {
	// SemanticForest tree = toTree(1, equation, dm);
	//
	// }

	public static ArrayList<Double> getFactors(String equation) {
		ArrayList<Double> coefficients = new ArrayList<>();
		char[] array = equation.toCharArray();
		int index = equation.indexOf("V");
		StringBuilder sb = new StringBuilder();
		if (index - 1 >= 0 && array[index - 1] == '*') {
			int pos = index - 2;
			while (pos >= 0) {
				if (array[pos] != '+' && array[pos] != '-' && array[pos] != '*' && array[pos] != '/') {
					sb.append(array[pos]);
				} else {
					break;
				}
				pos--;
			}
			if (sb.length() > 0 && isNumeric(sb.toString())) {
				double num = Double.parseDouble(sb.reverse().toString().trim());
				coefficients.add(num);
			}

		} else if (index + 1 < equation.length() && array[index + 1] == '*') {
			int pos = index + 2;
			while (pos < equation.length()) {
				if (array[pos] != '+' && array[pos] != '-' && array[pos] != '*' && array[pos] != '/') {
					sb.append(array[pos]);
				} else {
					break;
				}
				pos++;
			}
			if (sb.length() > 0 && isNumeric(sb.toString())) {
				double num = Double.parseDouble(sb.toString().trim());
				coefficients.add(num);
			}
		}
		if (index != -1 && coefficients.size() == 0)
			coefficients.add(1.0);
		equation = equation.substring(index + 1);
		index = equation.indexOf("V");
		array = equation.toCharArray();
		// System.out.println(equation);
		sb = new StringBuilder();
		if (index - 1 >= 0 && array[index - 1] == '*') {
			int pos = index - 2;
			while (pos >= 0) {
				if (array[pos] != '+' && array[pos] != '-' && array[pos] != '*' && array[pos] != '/') {
					sb.append(array[pos]);
				} else {
					break;
				}
				pos--;
			}
			if (sb.length() > 0 && isNumeric(sb.toString())) {
				double num = Double.parseDouble(sb.reverse().toString().trim());
				coefficients.add(num);
			}

		} else if (index + 1 < equation.length() && array[index + 1] == '*') {
			int pos = index + 2;
			while (pos < equation.length()) {
				if (array[pos] != '+' && array[pos] != '-' && array[pos] != '*' && array[pos] != '/') {
					sb.append(array[pos]);
				} else {
					break;
				}
				pos++;
			}
			if (sb.length() > 0 && isNumeric(sb.toString())) {
				double num = Double.parseDouble(sb.toString().trim());
				coefficients.add(num);
			}
		}
		if (index != -1 && coefficients.size() == 1 && sb.toString().length() == 0)
			coefficients.add(1.0);
		// System.out.print(coefficients);
		return coefficients;
	}

	public static boolean isValidEquation(String str) {
		for (char c : str.toCharArray()) {
			if (Character.isAlphabetic(c))
				return false;
		}
		return true;
	}

	public static boolean isNumeric(String str) {
		return str.matches("-?\\d+(.\\d+)?");
	}

	public static boolean isDollarValue(String str) {
		return str.indexOf("$") == -1 ? false : isNumeric(str.replace("$", ""));
	}

	public static double eval(final String str) {
		return new Object() {
			int pos = -1, ch;

			void nextChar() {
				ch = (++pos < str.length()) ? str.charAt(pos) : -1;
			}

			boolean eat(int charToEat) {
				while (ch == ' ')
					nextChar();
				if (ch == charToEat) {
					nextChar();
					return true;
				}
				return false;
			}

			double parse() {
				nextChar();
				double x = parseExpression();
				if (pos < str.length())
					throw new RuntimeException("Unexpected: " + (char) ch);
				return x;
			}

			// Grammar:
			// expression = term | expression `+` term | expression `-` term
			// term = factor | term `*` factor | term `/` factor
			// factor = `+` factor | `-` factor | `(` expression `)`
			// | number | functionName factor | factor `^` factor

			double parseExpression() {
				double x = parseTerm();
				for (;;) {
					if (eat('+'))
						x += parseTerm(); // addition
					else if (eat('-'))
						x -= parseTerm(); // subtraction
					else
						return x;
				}
			}

			double parseTerm() {
				double x = parseFactor();
				for (;;) {
					if (eat('*'))
						x *= parseFactor(); // multiplication
					else if (eat('/'))
						x /= parseFactor(); // division
					else
						return x;
				}
			}

			double parseFactor() {
				if (eat('+'))
					return parseFactor(); // unary plus
				if (eat('-'))
					return -parseFactor(); // unary minus

				double x;
				int startPos = this.pos;
				if (eat('(')) { // parentheses
					x = parseExpression();
					eat(')');
				} else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
					while ((ch >= '0' && ch <= '9') || ch == '.')
						nextChar();
					x = Double.parseDouble(str.substring(startPos, this.pos));
				} else if (ch >= 'a' && ch <= 'z') { // functions
					while (ch >= 'a' && ch <= 'z')
						nextChar();
					String func = str.substring(startPos, this.pos);
					x = parseFactor();
					if (func.equals("sqrt"))
						x = Math.sqrt(x);
					else if (func.equals("sin"))
						x = Math.sin(Math.toRadians(x));
					else if (func.equals("cos"))
						x = Math.cos(Math.toRadians(x));
					else if (func.equals("tan"))
						x = Math.tan(Math.toRadians(x));
					else
						throw new RuntimeException("Unknown function: " + func);
				} else {
					throw new RuntimeException("Unexpected: " + (char) ch);
				}

				if (eat('^'))
					x = Math.pow(x, parseFactor()); // exponentiation

				return x;
			}
		}.parse();
	}
}
