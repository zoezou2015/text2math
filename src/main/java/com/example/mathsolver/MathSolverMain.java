package com.example.mathsolver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.script.ScriptException;

import com.example.equationparse.ReadFile;
import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.example.sp.HybridGrammar;
import com.statnlp.example.sp.HybridGrammarReader;
import com.statnlp.example.sp.SemTextDataManager;
import com.statnlp.example.sp.SemTextInstanceReader;
import com.statnlp.example.sp.SemanticForest;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.neural.NeuralConfigReader;
import com.udojava.evalex.Expression;

public class MathSolverMain {
	static int numIterations = 100;
	static double l2 = 0.01;
	static String optim = "lbfgs";
	static boolean debug = false;
	static boolean skipTest = false;
	static boolean printFeats = false;
	static boolean testOnTrain = false;
	static boolean precomputeTestFeatureIdx = false;
	static double learningRate = 0.01;
	static String modelPath = "";
	static String savePrefix = "";
	static String neuralSavePrefix = "";
	static String pretrainPath = "";
	static boolean fixPretrain = false;
	static boolean validation = false;
	static boolean isDecoding = false;
	static String testSet = "test";
	static int foldNum = 5;
	static boolean eval_solution = false;
	static String roy_subset = "";

	private final static double epsilon = 1E-10;

	private static void argParser(String[] args) throws FileNotFoundException {
		int i = 0;
		NetworkConfig.REGULARIZE_NEURAL_FEATURES = false;
		NetworkConfig.USE_NEURAL_FEATURES = false;
		NetworkConfig.OPTIMIZE_NEURAL = false;
		NetworkConfig.REPLACE_ORIGINAL_EMISSION = false;
		NeuralConfigReader.readConfig("neural_server/neural.sp.config");
		NeuralConfig.FIX_EMBEDDING = false;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;

		NetworkConfig.RATIO_STOP_CRIT = false;
		while (i < args.length) {
			String opt = args[i];
			// System.out.println(opt);
			if (opt.equals("-iter")) {
				numIterations = Integer.parseInt(args[++i]);
			} else if (opt.equals("-config")) {
				NeuralConfigReader.readConfig(args[++i]);
			} else if (opt.equals("-thread")) {
				NetworkConfig.NUM_THREADS = Integer.parseInt(args[++i]);
			} else if (opt.equals("-fold")) {
				foldNum = Integer.parseInt(args[++i]);
			} else if (opt.equals("-dataset")) {
				MathSolverConfig.dataset = args[++i];
				if (MathSolverConfig.dataset.equals("commoncore"))
					foldNum = 6;
				else if (MathSolverConfig.dataset.equals("illinois")) {
					foldNum = 5;
					MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH = 5;
				} else if (MathSolverConfig.dataset.equals("roy")) {
					foldNum = 5;
					MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH = 8;
				} else if (MathSolverConfig.dataset.equals("single")) {
					foldNum = 5;
					MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH = 5;
				} else if (MathSolverConfig.dataset.equals("multi")) {
					foldNum = 5;
					MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH = 5;
				} else if (MathSolverConfig.dataset.equals("addsub")) {
					foldNum = 3;
					MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH = 5;
				} else if (MathSolverConfig.dataset.equals("all")) {
					foldNum = 5;
					MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH = 5;
				}
			} else if (opt.equals("-neural")) {
				NetworkConfig.USE_NEURAL_FEATURES = true;
				precomputeTestFeatureIdx = true;
			} else if (opt.equals("-l2")) {
				l2 = Double.parseDouble(args[++i]);
				NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
			} else if (opt.equals("-lr")) {
				learningRate = Double.parseDouble(args[++i]);
				NeuralConfig.LEARNING_RATE = learningRate;
			} else if (opt.equals("-save-iter")) {
				NetworkConfig.SAVE_MODEL_AFTER_ITER = Integer.parseInt(args[++i]);
			} else if (opt.equals("-model")) {
				modelPath = args[++i];
			} else if (opt.equals("-neural-save-prefix")) {
				neuralSavePrefix = args[++i];
			} else if (opt.equals("-train")) {
				skipTest = true;
			} else if (opt.equals("-print-feats")) {
				printFeats = true;
			} else if (opt.equals("-optim")) {
				optim = args[++i];
			} else if (opt.equals("-save-prefix")) {
				savePrefix = args[++i];
			} else if (opt.equals("-window")) {
				NetworkConfig.NEURAL_WINDOW_SIZE = Integer.parseInt(args[++i]);
			} else if (opt.equals("-optimize-neural")) {
				NetworkConfig.OPTIMIZE_NEURAL = true;
			} else if (opt.equals("-regularize-neural")) {
				NetworkConfig.REGULARIZE_NEURAL_FEATURES = true;
			} else if (opt.equals("-replace-emission")) {
				NetworkConfig.REPLACE_ORIGINAL_EMISSION = true;
			} else if (opt.equals("-fix-embedding")) {
				NeuralConfig.FIX_EMBEDDING = true;
			} else if (opt.equals("-embedding")) {
				NeuralConfig.EMBEDDING.set(0, args[++i]);
			} else if (opt.equals("-embedding-size")) {
				NeuralConfig.EMBEDDING_SIZE.set(0, Integer.parseInt(args[++i]));
			} else if (opt.equals("-num-layer")) {
				NeuralConfig.NUM_LAYER = Integer.parseInt(args[++i]);
			} else if (opt.equals("-hidden")) {
				NeuralConfig.HIDDEN_SIZE = Integer.parseInt(args[++i]);
			} else if (opt.equals("-activation")) {
				NeuralConfig.ACTIVATION = args[++i];
			} else if (opt.equals("-dropout")) {
				NeuralConfig.DROPOUT = Double.parseDouble(args[++i]);
			} else if (opt.equals("-pretrain")) {
				pretrainPath = args[++i];
			} else if (opt.equals("-fix-pretrain")) {
				fixPretrain = true;
			} else if (opt.equals("-sequential-touch")) {
				NetworkConfig.PARALLEL_FEATURE_EXTRACTION = false;
			} else if (opt.equals("-ratio-stop-criterion")) {
				NetworkConfig.RATIO_STOP_CRIT = true;
			} else if (opt.equals("-precompute-test-feature-index")) {
				precomputeTestFeatureIdx = true;
			} else if (opt.equals("-validation")) {
				validation = true;
			} else if (opt.equals("-decode")) {
				// testSet = args[++i]; // "test", "syn"
				isDecoding = true;
			} else if (opt.equals("-gram")) {
				MathSolverConfig._SEMANTIC_PARSING_NGRAM = Integer.parseInt(args[++i]);
			} else if (opt.equals("-gold-num")) {
				MathSolverConfig.USE_GOLD_NUMBER = true;
			} else if (opt.equals("-pred-num")) {
				MathSolverConfig.USE_PREDICT_NUMBER = true;
			} else if (opt.equals("-eval-sol")) {
				eval_solution = true;
			} else if (opt.equals("-no-pos")) {
				MathSolverConfig.USE_POS_FEAT = false;
			} else if (opt.equals("-no-lex")) {
				MathSolverConfig.USE_LEXICON = false;
			} else if (opt.equals("-suffix-x")) {
				MathSolverConfig.USE_SUFFIX_X = true;
			} else if (opt.equals("-no-x")) {
				MathSolverConfig.NO_X = true;
			} else if (opt.equals("-no-num")) {
				MathSolverConfig.NUM_CONSTRAINT = false;
			} else if (opt.equals("-equal-root")) {
				MathSolverConfig.EQUAL_ROOT = true;
			} else if (opt.equals("-no-reverse")) {
				MathSolverConfig.NO_REVERSE_OP = true;
			} else if (opt.equals("-debug")) {
				debug = true;
			} else if (opt.equals("-test-on-train")) {
				testOnTrain = true;
			} else {
				System.err.println("Unknown option: " + args[i] + " index of: " + i);
				System.exit(1);
			}
			i++;
		}

		System.out.println("STATNLP CONFIGURATIONS:");
		System.out.println(
				"* Mode: " + (debug ? "Debug" : validation ? "Validation" : isDecoding ? "Decoding" : "Training"));
		System.out.println("* Dataset: " + MathSolverConfig.dataset);
		System.out.println("* #fold: " + foldNum);
		System.out.println("* #iterations: " + numIterations);
		System.out.println("* Optimizer: " + optim);
		if (!optim.equals("lbfgs")) {
			System.out.println("* Learning rate " + learningRate);
		}
		System.out.println("* Extract from test " + precomputeTestFeatureIdx);

		System.out.println("* Regularization " + NetworkConfig.L2_REGULARIZATION_CONSTANT);
		System.out.println("* Neural features: " + NetworkConfig.USE_NEURAL_FEATURES);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			System.out.println("* Regularize neural: " + NetworkConfig.REGULARIZE_NEURAL_FEATURES);
			System.out.println("* Optimize neural: " + NetworkConfig.OPTIMIZE_NEURAL);
			System.out.println("* Replace emission: " + NetworkConfig.REPLACE_ORIGINAL_EMISSION);
		}
		if (!pretrainPath.equals("")) {
			System.out.println("* Pretrain model: " + pretrainPath + (fixPretrain ? " (fixed)" : " (fine-tuned)"));
		}
		System.out.println("");

		if (NetworkConfig.USE_NEURAL_FEATURES) {
			System.out.println("NEURAL NET CONFIGURATIONS:");
			System.out.println("* Language: " + NeuralConfig.LANGUAGE);
			System.out.println("* Word embedding: " + NeuralConfig.EMBEDDING.get(0));
			System.out.println("* Embedding size (0 means OneHot): " + NeuralConfig.EMBEDDING_SIZE.get(0));
			System.out.println("* Fix embedding: " + NeuralConfig.FIX_EMBEDDING);
			System.out.println("* Input window size: " + NetworkConfig.NEURAL_WINDOW_SIZE);
			System.out.println("* Number of layer: " + NeuralConfig.NUM_LAYER);
			System.out.println("* Hidden size: " + NeuralConfig.HIDDEN_SIZE);
			System.out.println("* Dropout: " + NeuralConfig.DROPOUT);
		}
	}

	public static void main(String args[]) throws Exception {
		NetworkConfig.MathSolver = true;
		argParser(args);
		String g_filename = "data/hybridgrammar.txt";
		String lexiconFile = "data/lexicon.txt";
		HybridGrammar g = HybridGrammarReader.read(g_filename);
		MathLexicon lexicon = LexiconReader.read(lexiconFile);
		if (MathSolverConfig.dataset.equals("roy")) {
			for (String subset : MathSolverConfig.roy_dataset) {

				double total = 0.0;
				double answer_total = 0.0;
				int cv = foldNum;
				if (debug)
					cv = 1;
				for (int i = 0; i < cv; i++) {
					double[] accuracy = doTrainTest(i, g, lexicon, subset, debug);
					total += accuracy[0];
					answer_total += accuracy[1];
				}
				System.err.println("== CV accuracy " + subset.toUpperCase() + " ==");
				System.err.println("CV Equation Tree accuracy: " + total / cv);
				System.err.println("CV Answer accuracy: " + answer_total / cv);
			}
		} else {
			double total = 0.0;
			double answer_total = 0.0;
			int cv = foldNum;
			if (debug)
				cv = 1;
			for (int i = 3; i < cv; i++) {
				double[] accuracy = doTrainTest(i, g, lexicon, "", debug);
				total += accuracy[0];
				answer_total += accuracy[1];
			}
			System.err.println("CV Equation Tree accuracy: " + total / cv);
			System.err.println("CV Answer accuracy: " + answer_total / cv);
		}
		// String lexicon_expression = " 231 - 312 ";
		// String[] num = { "231", "312" };
		// ArrayList<String> numbers = new ArrayList<>();
		// for (String nString : num) {
		// numbers.add(nString);
		// }
		// double lexicon_answer = calculateMathExpression(lexicon_expression);
		// ArrayList<String> reverse_numbers = new ArrayList<>();
		// for (int num_idx = numbers.size() - 1; num_idx >= 0; num_idx--)
		// reverse_numbers.add(numbers.get(num_idx));
		// String new_expression = fitExpressionPatternInOrder(reverse_numbers,
		// lexicon_expression);
		// lexicon_expression = new_expression;
		// System.err.println("lexicon after: " + lexicon_expression);

	}

	public static double[] doTrainTest(int testFold, HybridGrammar g, MathLexicon lexicon, String subset, boolean debug)
			throws Exception {
		String fold_path = MathSolverConfig.dataDir + MathSolverConfig.dataset + "/";
		String file = MathSolverConfig.dataDir + MathSolverConfig.dataset + "/questions.json";
		ArrayList<ArrayList<Integer>> folds = ReadFile.extractFolds(fold_path, subset, foldNum, ".txt");
		ArrayList<Integer> train_ids = new ArrayList<>();
		ArrayList<Integer> test_ids = new ArrayList<>();
		for (int i = 0; i < foldNum; i++) {
			if (i == testFold) {
				test_ids.addAll(folds.get(i));
			} else {
				train_ids.addAll(folds.get(i));
			}
		}
		for (int test_id : test_ids) {
			if (train_ids.indexOf(test_id) != -1)
				System.out.println(test_id);
		}
		boolean isTrain = modelPath.equals("");

		SemTextDataManager dm = new SemTextDataManager();
		MathInstance[] test_instances = null;
		MathInstance[] train_instances = null;
		MathInstance[] train_instances_clone = null;
		MathInstance[] test_instances_clone = null;
		if (MathSolverConfig.dataset.equals("single") || MathSolverConfig.dataset.equals("multi")
				|| MathSolverConfig.dataset.equals("addsub") || MathSolverConfig.dataset.equals("all")) { // for roy
			test_instances = SemTextInstanceReader.MathSolverReader_All(test_ids, file, dm, false);
			train_instances = SemTextInstanceReader.MathSolverReader_All(train_ids, file, dm, true);
			train_instances_clone = SemTextInstanceReader.MathSolverReader_All(train_ids, file, dm, false);
			test_instances_clone = SemTextInstanceReader.MathSolverReader_All(test_ids, file, dm, false);
		} else if (subset.equals("")) {
			test_instances = SemTextInstanceReader.MathSolverReader(test_ids, file, dm, false);
			train_instances = SemTextInstanceReader.MathSolverReader(train_ids, file, dm, true);
			train_instances_clone = SemTextInstanceReader.MathSolverReader(train_ids, file, dm, false);
			test_instances_clone = SemTextInstanceReader.MathSolverReader(test_ids, file, dm, false);
		} else { // for roy dataset
			test_instances = SemTextInstanceReader.MathSolverReader_Roy(test_ids, file, dm, false);
			train_instances = SemTextInstanceReader.MathSolverReader_Roy(train_ids, file, dm, true);
			train_instances_clone = SemTextInstanceReader.MathSolverReader_Roy(train_ids, file, dm, false);
			test_instances_clone = SemTextInstanceReader.MathSolverReader_Roy(test_ids, file, dm, false);
		}

		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;

		SemanticForest forest_global = SemTextInstanceReader.toForest(dm);
		// System.out.println(forest_global);
		// System.exit(1);
		if (debug) {
			// train_instances = new MathInstance[] { train_instances[0] };
			train_instances = Arrays.copyOf(train_instances, 1);
			// train_instances_clone = new MathInstance[] { train_instances_clone[0] };
			train_instances_clone = Arrays.copyOfRange(train_instances_clone, 0, 1);
			// test_instances = new MathInstance[] { test_instances[0] };
			test_instances = Arrays.copyOfRange(test_instances, 0, 1);
			numIterations = 10;
		}
		int size = train_instances.length;
		int testSize = test_instances.length;
		MathInstance all_instances[] = new MathInstance[size + testSize];
		int i = 0;
		for (; i < size; i++) {
			all_instances[i] = train_instances[i];
		}
		int lastId = all_instances[i - 1].getInstanceId();
		for (int j = 0; j < testSize; j++, i++) {
			all_instances[i] = test_instances_clone[j];
			all_instances[i].setInstanceId(lastId + j + 1);
			all_instances[i].setUnlabeled();
		}
		GlobalNetworkParam param_G;
		if (!isTrain) {
			// String pretrain_model_path = modelPath + "." + testFold + ".99.model";
			String pretrain_model_path = modelPath + subset + "." + testFold + ".99.model";
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pretrain_model_path));
			param_G = (GlobalNetworkParam) ois.readObject();
			ois.close();
		} else {
			if (optim.equals("adam")) {
				param_G = new GlobalNetworkParam(
						OptimizerFactory.getGradientDescentFactoryUsingAdaM(learningRate, 0.9, 0.999, 1e-8));
			} else if (optim.equals("adagrad")) {
				param_G = new GlobalNetworkParam(OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(learningRate));
			} else if (optim.equals("adadelta+adagrad")) {
				param_G = new GlobalNetworkParam(OptimizerFactory
						.getGradientDescentFactoryUsingSmoothedAdaDeltaThenAdaGrad(0.01, 0.95, 1e-6, 0.75));
			} else { // defaults to LBFGS
				param_G = new GlobalNetworkParam();
			}
		}
		GlobalNetworkParam pretrain_param_G = null;
		boolean preTrained = !pretrainPath.equals("");
		if (preTrained) {
			String preTrainModel = pretrainPath + subset + "." + testFold + ".99.model";
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(preTrainModel));
			pretrain_param_G = (GlobalNetworkParam) ois.readObject();
			ois.close();
			param_G.setPretrainParams(pretrain_param_G, fixPretrain);
		}
		MathSolverFeatureManager fm = new MathSolverFeatureManager(param_G, g, dm);
		MathSolverNetworkCompiler compiler = new MathSolverNetworkCompiler(g, forest_global, dm);

		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler);

		// model.train(train_instances, numIterations);
		if (isTrain) {
			// String modelName = lang;
			String modelName = "";

			if (!savePrefix.equals("")) {
				modelName = savePrefix + subset + "." + testFold;
				;
				// if (!savePrefix.endsWith("." + lang)) {
				// modelName += "." + lang;
				// }

			}

			if (isDecoding) {
				numIterations = 0;
				System.out.println("Setting #iter to 0 for touch..");
			}

			if (precomputeTestFeatureIdx) {
				// This will train the model strictly on the 600 training instances.
				// However, it also allocates indices for features that can be extracted from
				// the test instances.
				// This has no effect for non-neural features (unseen discrete features will be
				// ignored during prediction),
				// but will affect neural features (such neural weights are always computable by
				// neural network)
				model.train(all_instances, size, numIterations, modelName);
			} else {
				model.train(train_instances, size, numIterations, modelName);
			}
			if (isDecoding) {
				String neuralModelName = "";
				if (!neuralSavePrefix.equals("")) {
					neuralModelName = neuralSavePrefix + subset + "." + testFold;
					// if (!neuralSavePrefix.endsWith("." + lang)) {
					// neuralModelName += "." + lang;
					// }

				}
				// System.out.println(neuralModelName);
				model.loadPretrainNN(neuralModelName);
			}
		}
		if (printFeats) {// printFeats true
			GlobalNetworkParam paramG = fm.getParam_G();
			System.out.println("Num features: " + paramG.countFeatures());
			System.out.println("Features:");
			HashMap<String, HashMap<String, HashMap<String, Integer>>> featureIntMap = paramG.getFeatureIntMap();
			for (String featureType : (featureIntMap.keySet())) {

				HashMap<String, HashMap<String, Integer>> outputInputMap = featureIntMap.get(featureType);
				System.out.println(featureType + " (" + outputInputMap.size() + ")");
				for (String output : (outputInputMap.keySet())) {
					System.out.println("\t" + output);
					HashMap<String, Integer> inputMap = outputInputMap.get(output);
					for (String input : (inputMap.keySet())) {
						int featureId = inputMap.get(input);
						System.out
								.println("\t\t" + input + " " + featureId + " " + fm.getParam_G().getWeight(featureId));
					}
				}
			}
		}
		if (testOnTrain)
			evaluate(train_instances_clone, model, lexicon);

		double[] accuracy = evaluate(test_instances, model, lexicon);
		return accuracy;
	}

	private static double[] evaluate(MathInstance[] test_instances, NetworkModel model, MathLexicon lexicon)
			throws InterruptedException, ScriptException {
		Instance[] output_instances_unlabeled;

		output_instances_unlabeled = model.decode(test_instances);
		double[] accuracy = new double[4];
		double total = output_instances_unlabeled.length;
		double tree_corr = 0.0;
		double answer_corr = 0.0;
		double alt_tree_corr = 0.0;
		double alt_answer_corr = 0.0;
		for (int k = 0; k < output_instances_unlabeled.length; k++) {
			Instance output_inst_U = output_instances_unlabeled[k];
			boolean r = output_inst_U.getOutput().equals(output_inst_U.getPrediction());
			boolean equation_r = false;
			int index = ((MathInstance) output_inst_U).getIndex();
			System.err.println("Instance ID: " + index);
			SemanticForest prediction = (SemanticForest) output_inst_U.getPrediction();
			SemanticForest output = (SemanticForest) output_inst_U.getOutput();
			ArrayList<Double> pred_numbers = ((SemanticForest) output_inst_U.getPrediction()).getAllNumbers();
			ArrayList<Double> gold_numbers = output.getAllNumbers();
			boolean isAllInteger = isAllInt(gold_numbers);
			if (r) {
				tree_corr++;
			}

			String expression = ((SemanticForest) output_inst_U.getPrediction()).toMathExpression();
			expression = expression.replace("=", "");

			double pred_answer = -1.0;
			double gold_answer = ((MathInstance) output_inst_U).getSolution();
			if (eval_solution) {
				if (gold_numbers.size() == pred_numbers.size() && pred_numbers.size() == 3) {
					System.err.println("X=" + expression);
					pred_answer = calculateMathExpression(expression, pred_numbers);
					if (pred_answer <= 0 || (pred_answer - (int) pred_answer) != 0) {
						pred_answer = calculateMathExpression(expression, pred_numbers);
						String spacedExpression = ((SemanticForest) output_inst_U.getPrediction())
								.toSpacedMathExpression();
						// System.out.println(spacedExpression);
						ArrayList<String> numbers = ((MathInstance) output_inst_U).getNumberList();
						String new_expression = fitExpressionPatternInOrder(numbers, spacedExpression);
						pred_answer = calculateMathExpression(new_expression, pred_numbers);
						expression = new_expression;
					} else if (r) {
						String spacedExpression = ((SemanticForest) output_inst_U.getPrediction())
								.toSpacedMathExpression();
						// System.out.println(spacedExpression);
						ArrayList<String> numbers = ((MathInstance) output_inst_U).getNumberList();
						if (!checkNumberOrderInExpression(numbers, spacedExpression)) {
							String new_expression = fitExpressionPatternInOrder(numbers, spacedExpression);
							pred_answer = calculateMathExpression(new_expression, pred_numbers);
							expression = new_expression;
						}
					}
				} else if (gold_numbers.size() == pred_numbers.size() && pred_numbers.size() == 2) {
					ArrayList<String> numbers = ((MathInstance) output_inst_U).getNumberList();
					String text = ((MathInstance) output_inst_U).getText();
					String questionSentence = ((MathInstance) output_inst_U).getQuestionSentence();
					String lexicon_expression = LexiconReader.checkVaildLexicon(lexicon, numbers, text,
							questionSentence);
					if (!lexicon_expression.equals("")) {
						// System.err.println("lexicon before: " + lexicon_expression);
						double lexicon_answer = calculateMathExpression(lexicon_expression, pred_numbers);
						if ((lexicon_answer < 0 && lexicon_expression.contains("-"))
								|| (lexicon_expression.contains("/") && isAllInteger
										&& (lexicon_answer - (int) lexicon_answer) != 0)) {
							ArrayList<String> reverse_numbers = new ArrayList<>();
							for (int num_idx = numbers.size() - 1; num_idx >= 0; num_idx--) {
								reverse_numbers.add(numbers.get(num_idx));
							}
							String new_expression = fitExpressionPatternInOrder(reverse_numbers, lexicon_expression);
							lexicon_expression = new_expression;
						}
						if (lexicon_expression.contains("/") && isAllInteger && !isInt(lexicon_answer)) {
							// System.out.println("here: " + index);

						}
						expression = lexicon_expression;
						// System.err.println("lexicon after: " + expression);
					}
					// System.err.println("X=" + expression);
					pred_answer = calculateMathExpression(expression, pred_numbers);
					if ((pred_answer <= 0 && expression.contains("-"))
							|| (expression.contains("/") && isAllInteger && (pred_answer - (int) pred_answer) != 0)) {
						// pred_answer = calculateMathExpression(expression);
						String spacedExpression = ((SemanticForest) output_inst_U.getPrediction())
								.toSpacedMathExpression();
						// System.out.println(spacedExpression);

						String new_expression = fitExpressionPatternInOrder(numbers, spacedExpression);
						pred_answer = calculateMathExpression(new_expression, pred_numbers);
						expression = new_expression;
						if ((pred_answer < 0 || (pred_answer - (int) pred_answer) != 0)) {
							ArrayList<String> reverse_numbers = new ArrayList<>();
							for (int num_idx = numbers.size() - 1; num_idx >= 0; num_idx--) {
								reverse_numbers.add(numbers.get(num_idx));
							}
							new_expression = fitExpressionPatternInOrder(reverse_numbers, spacedExpression);
							pred_answer = calculateMathExpression(new_expression, pred_numbers);
							expression = new_expression;
						}
					}

				} else if (gold_numbers.size() != pred_numbers.size()) {
					ArrayList<String> numbers = ((MathInstance) output_inst_U).getNumberList();
					String text = ((MathInstance) output_inst_U).getText();
					String questionSentence = ((MathInstance) output_inst_U).getQuestionSentence();
					String lexicon_expression = LexiconReader.checkVaildLexicon(lexicon, numbers, text,
							questionSentence);
					if (!lexicon_expression.equals("")) {
						// System.err.println("lexicon before: " + lexicon_expression);
						double lexicon_answer = calculateMathExpression(lexicon_expression, pred_numbers);
						if ((lexicon_answer < 0 || (lexicon_answer - (int) lexicon_answer) != 0)) {
							ArrayList<String> reverse_numbers = new ArrayList<>();
							for (int num_idx = numbers.size() - 1; num_idx >= 0; num_idx--) {
								reverse_numbers.add(numbers.get(num_idx));
							}
							String new_expression = fitExpressionPatternInOrder(reverse_numbers, lexicon_expression);
							lexicon_expression = new_expression;
						}
						expression = lexicon_expression;
						// System.err.println("lexicon after: " + expression);
					}
					// System.err.println("X=" + expression);
					pred_answer = calculateMathExpression(expression, pred_numbers);
					if ((pred_answer <= 0 || (pred_answer - (int) pred_answer) != 0)) { // pred_answer != gold_answer ||
						// pred_answer = calculateMathExpression(expression);
						String spacedExpression = ((SemanticForest) output_inst_U.getPrediction())
								.toSpacedMathExpression();
						// System.out.println(spacedExpression);

						String new_expression = fitExpressionPatternInOrder(numbers, spacedExpression);
						pred_answer = calculateMathExpression(new_expression, pred_numbers);
						expression = new_expression;
						if ((pred_answer < 0 || (pred_answer - (int) pred_answer) != 0)) {
							ArrayList<String> reverse_numbers = new ArrayList<>();
							for (int num_idx = numbers.size() - 1; num_idx >= 0; num_idx--) {
								reverse_numbers.add(numbers.get(num_idx));
							}
							new_expression = fitExpressionPatternInOrder(reverse_numbers, spacedExpression);
							pred_answer = calculateMathExpression(new_expression, pred_numbers);
							expression = new_expression;
						}
					}

				} else {
					pred_answer = calculateMathExpression(expression, pred_numbers);
				}
				if (pred_answer < 0 && expression.indexOf("-") != -1) {
					pred_answer = -1 * pred_answer;
				}
				if (Math.abs(gold_answer - pred_answer) < 10e-3) {
					answer_corr++;
					equation_r = true;
				}
			}
			System.err.println("Equation Tree: " + r);
			System.err.println("Equation: " + equation_r);
			System.err.println("=Input=");
			System.err.println(output_inst_U.getInput());
			System.err.println("=Equation=");
			System.err.println(((MathInstance) output_inst_U).getOriginalEquation());
			System.err.println("=OUTPUT=");
			System.err.println(output_inst_U.getOutput());
			System.err.println("=PREDICTION=");
			System.err.println(output_inst_U.getPrediction());
			System.err.println("=Gold Equation=");
			System.err.println(((SemanticForest) output_inst_U.getOutput()).toMathExpression());
			// System.err.println(((SemanticForest)
			// output_inst_U.getOutput()).getAllNumbers());
			System.err.println("Gold Solution: " + gold_answer);
			System.err.println("=Pred Equation=");
			System.err.println("X=" + expression);
			// System.err.println(((SemanticForest)
			// output_inst_U.getPrediction()).getAllNumbers());
			System.err.println("Pred Solution: " + pred_answer);
		}
		accuracy[0] = tree_corr / total;
		accuracy[1] = answer_corr / total;

		accuracy[2] = alt_tree_corr / total;
		accuracy[3] = alt_answer_corr / total;
		System.err.println("equation tree accuracy=" + tree_corr / total + "=" + tree_corr + "/" + total);
		System.err.println("equation accuracy=" + answer_corr / total + "=" + answer_corr + "/" + total);
		System.err.println("****************************");
		return accuracy;

	}

	private static String fitExpressionPatternInOrder(ArrayList<String> numbers, String expression) {
		expression = expression.replace("=", "");
		// System.out.println(expression);
		String[] token = expression.split(" ");
		StringBuilder sb = new StringBuilder();
		int index = 0;

		for (String t : token) {

			t = t.trim();
			// System.out.println("t[" + t + "]");
			if (containMathOp(t, Arrays.asList('+', '-', '*', '/', '(', ')', '$', '#'))) {
				sb.append(t);
			} else if (t.equals("  ") || t.equals(" ") || t.equals("")) {
				continue;
			} else {
				if (index < numbers.size()) {
					sb.append(numbers.get(index));
					index++;
				}
			}
		}

		return sb.toString();
	}

	private static boolean containMathOp(String token, List<Character> keys) {
		for (int index = 0; index < token.length(); ++index) {
			if (keys.contains(token.charAt(index))) {
				return true;
			}
		}
		return false;
	}

	private static boolean checkNumberOrderInExpression(ArrayList<String> numbers, String expression) {
		expression = expression.replace("=", "");
		String[] token = expression.split(" ");
		int index = 0;
		for (String t : token) {
			t = t.trim();
			if (containMathOp(t, Arrays.asList('+', '-', '*', '/', '(', ')', '$', '#'))) {

			} else if (t.equals(" ") || t.equals("")) {
				continue;
			} else {
				if (Double.parseDouble(t) == Double.parseDouble(numbers.get(index)))
					index++;
				else
					return false;
			}
		}
		return true;
	}

	private static double calculateMathExpression(String expression, ArrayList<Double> numbers) {
		if (!isValidMathExpression(expression, numbers.size())) {
			return Double.NEGATIVE_INFINITY;
		}
		boolean sub_reverse = false;
		boolean dvi_reverse = false;

		if (expression.indexOf("#") != -1) {
			int index = expression.indexOf("#");
			expression = expression.replace("#", "-");
			sub_reverse = true;
			// return -1.0;
		} else if (expression.indexOf("$") != -1) {
			int index = expression.indexOf("$");
			expression = expression.replace("$", "/");
			dvi_reverse = true;
			// return -1.0;
		}
		try {
			Expression pred_expression = new Expression(expression);
			pred_expression.setPrecision(5);
			BigDecimal pred_result = pred_expression.eval();
			double sol_pred = pred_result.doubleValue();
			if (sub_reverse) {
				sol_pred = sol_pred < 0 ? -sol_pred : sol_pred;
			}
			if (dvi_reverse) {
				sol_pred = sol_pred == 0 ? 0.0 : 1 / sol_pred;
			}
			return sol_pred;
		} catch (Exception e) {
			return -1.0;
		}
	}

	private static boolean isValidMathExpression(String str, int numberSize) {
		int count = 0;
		for (char c : str.toCharArray()) {
			if (Character.isAlphabetic(c) || c == '=' || c == 'X')
				return false;
			if (c == '+' || c == '-' || c == '*' || c == '/' || c == '$' || c == '#')
				count++;
		}
		if (count != numberSize - 1)
			return false;
		return true;
	}

	public static boolean isAllInt(ArrayList<Double> numbers) {
		for (double n : numbers) {
			if (!isInt(n))
				return false;
		}
		return true;
	}

	public static boolean isInt(double d) {
		return Math.abs(Math.floor(d) - d) < epsilon;
	}
}
