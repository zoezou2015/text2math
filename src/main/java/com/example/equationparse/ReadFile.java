package com.example.equationparse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

public class ReadFile {

	public static void main(String[] args) throws IOException {

		// extractFolds(); // run once only
		// ReadWriteProblemText();
	}

	public static void extractInstanceIds() {
		String idFile = "data/ids.txt";
		List<String> ids = new ArrayList<>();
		File dir = new File(EquationParserConfig.dataDir);
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".txt")) {
				String index = file.getName().substring(0, file.getName().length() - 4);
				ids.add(index);
			}
		}
		Collections.shuffle(ids, new Random(0));
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(idFile));
			for (int id_index = 0; id_index < ids.size(); id_index++) {
				String idd = ids.get(id_index) + "\n";
				// System.out.println("train_ids"+train_ids);
				writer.write(idd);
			}
		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}
	}

	public static ArrayList<ArrayList<Integer>> extractFolds() throws IOException {
		ArrayList<ArrayList<Integer>> folds = new ArrayList<>();
		ArrayList<Integer> allIndices = new ArrayList<>();
		File dir = new File(EquationParserConfig.dataDir);
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".txt")) {
				// System.out.println("Reading "+file.getName());
				int index = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));
				allIndices.add(index);
			}
		}
		Collections.shuffle(allIndices, new Random(0));
		for (int i = 0; i < 5; ++i) {
			ArrayList<Integer> fold = new ArrayList<>();
			for (int j = (int) (i * allIndices.size() / 5.0); j < (int) ((i + 1) * allIndices.size() / 5.0); ++j) {
				fold.add(allIndices.get(j));
			}
			folds.add(fold);
		}
		return folds;
	}

	public static ArrayList<ArrayList<Integer>> extractFolds(String file, int fold) throws IOException {
		ArrayList<ArrayList<Integer>> folds = new ArrayList<>();
		for (int i = 0; i < fold; i++) {
			String fold_file = file + "fold" + i;
			BufferedReader scan = new BufferedReader(new InputStreamReader(new FileInputStream(fold_file), "UTF8"));
			ArrayList<Integer> ids = new ArrayList<>();
			String line;
			while ((line = scan.readLine()) != null) {
				int id = Integer.parseInt(line);
				ids.add(id);
			}
			scan.close();
			folds.add(ids);
		}
		// for (ArrayList<Integer> f : folds) {
		// for (int i : f)
		// System.out.println(i);
		// }
		return folds;
	}

	public static ArrayList<ArrayList<Integer>> extractFolds(String file, String subset, int fold, String suffix)
			throws IOException {
		ArrayList<ArrayList<Integer>> folds = new ArrayList<>();
		for (int i = 0; i < fold; i++) {

			String fold_file = file + "fold" + i + suffix;
			if (!subset.equals("")) {
				fold_file = file + subset + "/fold" + i + suffix;
			}
			BufferedReader scan = new BufferedReader(new InputStreamReader(new FileInputStream(fold_file), "UTF8"));
			ArrayList<Integer> ids = new ArrayList<>();
			String line;
			while ((line = scan.readLine()) != null) {
				int id = Integer.parseInt(line);
				ids.add(id);
			}
			scan.close();
			folds.add(ids);
		}
		// for (ArrayList<Integer> f : folds) {
		// for (int i : f)
		// System.out.println(i);
		// }
		return folds;
	}

	public static EquationInstance[] ReadInstances(ArrayList<Integer> ids, boolean isTraining)
			throws IOException, SAXException, ParserConfigurationException {
		ArrayList<EquationInstance> instances = new ArrayList<>();
		File dir = new File(EquationParserConfig.dataDir);
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".txt")) {
				int index = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));
				if (ids.contains(index)) {
					String filename = EquationParserConfig.dataDir + index + ".txt";
					String featurepath = EquationParserConfig.featureDir + index + ".txt.xml";
					@SuppressWarnings("deprecation")
					List<String> lines = FileUtils.readLines(new File(filename));
					String text = lines.get(0); // problem text
					String equation = lines.get(2); // equation
					String annotated_text = lines.get(4);

					String[] tokens = text.trim().split(" ");
					WordToken[] wTokens = new WordToken[tokens.length];
					for (int k = 0; k < tokens.length; k++) {
						wTokens[k] = new WordToken(tokens[k]);
					}
					Sentence input = new Sentence(wTokens);
					EquationInstance instance = new EquationInstance(instances.size() + 1, 1.0, input, null, equation,
							annotated_text, index);

					// read features generated by Stanford CoreNLP tool
					// File featureFile = new File(featurepath);
					// DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					// DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					// Document doc = dBuilder.parse(featureFile);
					// System.out.println("Root element :" +
					// doc.getDocumentElement().getNodeName());
					// NodeList nList = doc.getElementsByTagName("student");
					// System.out.println("----------------------------");

					instances.add(instance);
				}
			}
		}
		return instances.toArray(new EquationInstance[instances.size()]);
	}

	public static void ReadWriteProblemText() throws IOException {
		File dir = new File(EquationParserConfig.dataDir);
		FileWriter writer1 = new FileWriter(new File("data/filelist.txt"));
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".txt")) {
				int index = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));
				String filename = EquationParserConfig.dataDir + index + ".txt";
				@SuppressWarnings("deprecation")
				List<String> lines = FileUtils.readLines(new File(filename));
				String text = lines.get(0); // problem text
				String equation = lines.get(2); // equation
				String annotated_text = lines.get(4);
				String pathname = EquationParserConfig.textDir + index + ".txt";
				writer1.write("annotatedproblemtext/" + index + ".txt\n");
				FileWriter writer = new FileWriter(new File(pathname));
				writer.write(text + "\n");
				writer.close();
			}
		}
		writer1.close();
	}
}
