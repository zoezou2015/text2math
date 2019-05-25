package com.example.equationparse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.script.ScriptException;

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

public class EquationParserMain {

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

	public static void main(String args[]) throws Exception {
		NetworkConfig.EquationParser = true;
		argParser(args);
		String g_filename = "data/hybridgrammar.txt";
		HybridGrammar g = HybridGrammarReader.read(g_filename);
		double total = 0.0;
		double equation_total = 0.0;
		double grounding_total = 0.0;
		double alt_total = 0.0;
		double alt_equation_total = 0.0;
		double alt_grounding_total = 0.0;
		int cv = 5;
		if (debug)
			cv = 1;
		for (int i = 0; i < cv; i++) {
			double[] accuracy = doTrainTest(i, g, debug);
			total += accuracy[0];
			equation_total += accuracy[1];
			grounding_total += accuracy[2];
			alt_total += accuracy[3];
			alt_equation_total += accuracy[4];
			alt_grounding_total += accuracy[5];
		}
		System.err.println("CV Equation Tree accuracy: " + total / cv);
		System.err.println("CV Equation accuracy: " + equation_total / cv);
		System.err.println("CV Equation + Grounding accuracy: " + grounding_total / cv);
		System.err.println("----------------------------");
		System.err.println("CV ALT Equation Tree accuracy: " + alt_total / cv);
		System.err.println("CV ALT Equation accuracy: " + alt_equation_total / cv);
		System.err.println("CV ALT Equation + Grounding accuracy: " + alt_grounding_total / cv);
	}

	private static void argParser(String[] args) throws FileNotFoundException {
		int i = 0;
		NetworkConfig.REGULARIZE_NEURAL_FEATURES = false;
		NetworkConfig.USE_NEURAL_FEATURES = false;
		NetworkConfig.OPTIMIZE_NEURAL = false;
		NetworkConfig.REPLACE_ORIGINAL_EMISSION = false;
		NeuralConfigReader.readConfig("neural_server/neural.sp.config");
		NeuralConfig.FIX_EMBEDDING = false;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;

		NetworkConfig.RATIO_STOP_CRIT = true;
		while (i < args.length) {
			String opt = args[i];
			// System.out.println(opt);
			if (opt.equals("-iter")) {
				numIterations = Integer.parseInt(args[++i]);
			} else if (opt.equals("-config")) {
				NeuralConfigReader.readConfig(args[++i]);
			} else if (opt.equals("-thread")) {
				NetworkConfig.NUM_THREADS = Integer.parseInt(args[++i]);
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
				EquationParserConfig._SEMANTIC_PARSING_NGRAM = Integer.parseInt(args[++i]);
			} else if (opt.equals("-gold-num")) {
				EquationParserConfig.USE_GOLD_NUMBER = true;
			} else if (opt.equals("-pred-num")) {
				EquationParserConfig.USE_PREDICT_NUMBER = true;
			} else if (opt.equals("-no-pos")) {
				EquationParserConfig.USE_POS_FEAT = false;
			} else if (opt.equals("-debug")) {
				debug = true;
			} else {
				System.err.println("Unknown option: " + args[i] + " index of: " + i);
				System.exit(1);
			}
			i++;
		}

		// System.out.println("STATNLP CONFIGURATIONS:");
		System.out.println(
				"* Mode: " + (debug ? "Debug" : validation ? "Validation" : isDecoding ? "Decoding" : "Training"));
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

	public static double[] doTrainTest(int testFold, HybridGrammar g, boolean debug) throws Exception {
		String fold_path = EquationParserConfig.dataDir + "folds/";
		ArrayList<ArrayList<Integer>> folds = ReadFile.extractFolds(fold_path, foldNum);
		SemTextDataManager dm = new SemTextDataManager();
		ArrayList<Integer> train_ids = new ArrayList<>();
		ArrayList<Integer> test_ids = new ArrayList<>();
		for (int i = 0; i < foldNum; i++) {
			if (i == testFold) {
				test_ids.addAll(folds.get(i));
			} else {
				train_ids.addAll(folds.get(i));
			}
		}
		boolean isTrain = modelPath.equals("");

		EquationInstance[] train_instances = SemTextInstanceReader.equationParsingReader(train_ids, dm, true);
		EquationInstance[] train_instances_clone = SemTextInstanceReader.equationParsingReader(train_ids, dm, false);
		EquationInstance[] test_instances = SemTextInstanceReader.equationParsingReader(test_ids, dm, false);
		EquationInstance[] test_instances_clone = SemTextInstanceReader.equationParsingReader(test_ids, dm, false);

		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;

		SemanticForest forest_global = SemTextInstanceReader.toForest(dm);
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
		EquationInstance all_instances[] = new EquationInstance[size + testSize];
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
			String pretrain_model_path = modelPath + "." + testFold + ".99.model";
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
			String preTrainModel = pretrainPath + "." + testFold + ".99.model";
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(preTrainModel));
			pretrain_param_G = (GlobalNetworkParam) ois.readObject();
			ois.close();
			param_G.setPretrainParams(pretrain_param_G, fixPretrain);
		}
		EquationParserFeatureManager fm = new EquationParserFeatureManager(param_G, g, dm);
		EquationParserNetworkCompiler compiler = new EquationParserNetworkCompiler(g, forest_global, dm);

		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler);

		// model.train(train_instances, numIterations);
		if (isTrain) {
			// String modelName = lang;
			String modelName = "";

			if (!savePrefix.equals("")) {
				modelName = savePrefix + "." + testFold;
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
					neuralModelName = neuralSavePrefix + "." + testFold;
					// if (!neuralSavePrefix.endsWith("." + lang)) {
					// neuralModelName += "." + lang;
					// }

				}
				// System.out.println(neuralModelName);
				model.loadPretrainNN(neuralModelName);
			}
		}
		if (printFeats) {
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

		// evaluate(train_instances_clone, model);

		double[] accuracy = evaluate(test_instances, model);
		return accuracy;
	}

	private static double[] evaluate(EquationInstance[] test_instances, NetworkModel model)
			throws InterruptedException, ScriptException {
		Instance[] output_instances_unlabeled;

		output_instances_unlabeled = model.decode(test_instances);
		double[] accuracy = new double[6];
		double total = output_instances_unlabeled.length;
		double tree_corr = 0.0;
		double equation_corr = 0.0;
		double grounding_corr = 0.0;
		double alt_tree_corr = 0.0;
		double alt_equation_corr = 0.0;
		double alt_grounding_corr = 0.0;
		for (int k = 0; k < output_instances_unlabeled.length; k++) {
			Instance output_inst_U = output_instances_unlabeled[k];
			boolean r = output_inst_U.getOutput().equals(output_inst_U.getPrediction());
			boolean equation_r = false;
			boolean grounding_r = false;
			int index = ((EquationInstance) output_inst_U).getIndex();
			// r = false;
			System.err.println(((EquationInstance) output_inst_U).getIndex());
			String gold_equation = ((SemanticForest) output_inst_U.getOutput()).toEquation();
			String pred_equation = ((SemanticForest) output_inst_U.getPrediction()).toEquation();
			ArrayList<Variable> gold_grounding = ((EquationInstance) output_inst_U).getVariables();
			ArrayList<Variable> pred_grounding = ((SemanticForest) output_inst_U.getPrediction()).getAllVariables();
			SemanticForest prediction = (SemanticForest) output_inst_U.getPrediction();
			SemanticForest output = (SemanticForest) output_inst_U.getOutput();
			if (r) {
				tree_corr++;
				alt_tree_corr++;
				equation_r = evaluate_number((SemanticForest) output_inst_U.getPrediction(),
						(SemanticForest) output_inst_U.getOutput());
				if (equation_r) {
					equation_corr++;
					alt_equation_corr++;
				}
				grounding_r = evaluate_grounding((SemanticForest) output_inst_U.getPrediction(),
						(EquationInstance) output_inst_U);
				if (grounding_r) {
					grounding_corr++;
					alt_grounding_corr++;
				}
			}
			if (!r) {

				// if ((index == 999126 &&
				// pred_equation.equals("V-0.006999999999999999*V=10842.0"))
				// || (index == 999066 && pred_equation.equals("V-2.0*V=6885.24"))) {
				// r = true;
				// if (r) {
				// tree_corr++;
				// equation_corr++;
				// grounding_corr++;
				// equation_r = true;
				// grounding_r = true;
				// // grounding_r = evaluate_grounding((SemanticForest)
				// // output_inst_U.getPrediction(),
				// // (EquationInstance) output_inst_U);
				// // if (grounding_r) {
				// // grounding_corr++;
				// // }
				// }
				// }
				if (prediction.getAllVariables().size() == ((EquationInstance) output_inst_U).getVariables().size()) {
					r = SemTextInstanceReader.checkEquivalentEquation(gold_equation, pred_equation);

					if (r) {
						alt_tree_corr++;
						alt_equation_corr++;
						equation_r = true;
						grounding_r = evaluate_grounding((SemanticForest) output_inst_U.getPrediction(),
								(EquationInstance) output_inst_U);
						if (grounding_r) {
							alt_grounding_corr++;
						}
					}
				}

			}
			System.err.println("Equation Tree: " + r + "\n" + "Equation: " + equation_r + "\n" + "Equation Grounding: "
					+ grounding_r);
			System.err.println("=INPUT=");
			System.err.println(output_inst_U.getInput());
			System.err.println("=Equation=");
			System.err.println(((EquationInstance) output_inst_U).getEquation());
			// System.err.println("=OUTPUT=");
			// System.err.println(output_inst_U.getOutput());
			// System.err.println("=PREDICTION=");
			// System.err.println(output_inst_U.getPrediction());
			System.err.println("=OUTPUT Equation=");
			System.err.println(((SemanticForest) output_inst_U.getOutput()).toEquation());
			System.err.println(((SemanticForest) output_inst_U.getOutput()).getAllNumbers());
			System.err.println("=PREDICTION Equation=");
			System.err.println(((SemanticForest) output_inst_U.getPrediction()).toEquation());
			System.err.println(((SemanticForest) output_inst_U.getPrediction()).getAllNumbers());
			System.err.println("=Gold Variable Grounding=");
			for (Variable var : gold_grounding) {
				System.err.println(var);
			}
			System.err.println("=Pred Variable Grounding=");
			for (Variable var : pred_grounding) {
				System.err.println(var);
			}

		}
		accuracy[0] = tree_corr / total;
		accuracy[1] = equation_corr / total;
		accuracy[2] = grounding_corr / total;

		accuracy[3] = alt_tree_corr / total;
		accuracy[4] = alt_equation_corr / total;
		accuracy[5] = alt_grounding_corr / total;
		System.err.println("equation tree accuracy=" + tree_corr / total + "=" + tree_corr + "/" + total);
		System.err.println("equation accuracy=" + equation_corr / total + "=" + equation_corr + "/" + total);
		System.err
				.println("equation grounding accuracy=" + grounding_corr / total + "=" + grounding_corr + "/" + total);
		System.err.println("----------------------------");
		System.err.println("Alt equation tree accuracy=" + alt_tree_corr / total + "=" + alt_tree_corr + "/" + total);
		System.err
				.println("Alt equation accuracy=" + alt_equation_corr / total + "=" + alt_equation_corr + "/" + total);
		System.err.println("Alt equation grounding accuracy=" + alt_grounding_corr / total + "=" + alt_grounding_corr
				+ "/" + total);
		System.err.println("****************************");
		return accuracy;
	}

	private static boolean evaluate_number(SemanticForest predition, SemanticForest output) {
		ArrayList<Double> gold_number_list = output.getAllNumbers();
		ArrayList<Double> pred_number_list = predition.getAllNumbers();
		if (gold_number_list.size() != pred_number_list.size())
			return false;
		for (int i = 0; i < gold_number_list.size(); i++) {
			if (Math.abs(gold_number_list.get(i) - pred_number_list.get(i)) >= 0.000001)
				return false;
		}
		return true;
	}

	private static boolean evaluate_grounding(SemanticForest prediction, EquationInstance instance) {
		ArrayList<Variable> gold_grounding = instance.getVariables();
		ArrayList<Variable> pred_grounding = prediction.getAllVariables();
		if (gold_grounding.size() == pred_grounding.size()) {
			if (gold_grounding.size() == 1) {
				Variable var1 = gold_grounding.get(0);
				Variable var2 = pred_grounding.get(0);
				if (var1.getStart() > var2.getEnd() || var2.getStart() > var1.getEnd())
					return false;
				else
					return true;
			} else if (gold_grounding.size() == 2) {

				Variable var1 = gold_grounding.get(0);
				Variable var2 = pred_grounding.get(0);
				Variable var3 = gold_grounding.get(1);
				Variable var4 = pred_grounding.get(1);
				boolean flag12 = var1.getStart() >= var2.getEnd() || var2.getStart() >= var1.getEnd();
				boolean flag34 = var3.getStart() >= var4.getEnd() || var4.getStart() >= var3.getEnd();
				boolean flag14 = var1.getStart() >= var4.getEnd() || var4.getStart() >= var1.getEnd();
				boolean flag23 = var2.getStart() >= var3.getEnd() || var3.getStart() >= var2.getEnd();
				if ((flag12 || flag34) && (flag14 || flag23)) {
					return false;
				}
				return true;
			}

		} else if (gold_grounding.size() < pred_grounding.size()) {
			Variable var1 = gold_grounding.get(0);
			Variable var2 = pred_grounding.get(0);
			Variable var3 = pred_grounding.get(1);
			if (var1.getStart() >= var2.getEnd() || var2.getStart() >= var1.getEnd()) {
				;
			} else {
				return true;
			}
			if (var1.getStart() >= var3.getEnd() || var3.getStart() >= var1.getEnd()) {
				;
			} else {
				return true;
			}
		}
		return true;
	}

	private static boolean evaluate_grounding(SemanticForest prediction, EquationInstance instance,
			boolean isAlternative) {
		ArrayList<Variable> gold_grounding = instance.getVariables();
		ArrayList<Variable> pred_grounding = prediction.getAllVariables();
		if (gold_grounding.size() == pred_grounding.size()) {
			if (gold_grounding.size() == 1) {
				Variable var1 = gold_grounding.get(0);
				Variable var2 = pred_grounding.get(0);
				if (var1.getStart() > var2.getEnd() || var2.getStart() > var1.getEnd())
					return false;
				else
					return true;
			} else if (gold_grounding.size() == 2) {
				if (!isAlternative) {
					for (int i = 0; i < 2; i++) {
						Variable var1 = gold_grounding.get(i);
						Variable var2 = pred_grounding.get(i);
						if (var1.getStart() >= var2.getEnd() || var2.getStart() >= var1.getEnd()) {
							return false;
						}
					}
					return true;
				} else {

					Variable var1 = gold_grounding.get(0);
					Variable var2 = pred_grounding.get(0);
					Variable var3 = gold_grounding.get(1);
					Variable var4 = pred_grounding.get(1);
					boolean flag12 = var1.getStart() >= var2.getEnd() || var2.getStart() >= var1.getEnd();
					boolean flag34 = var3.getStart() >= var4.getEnd() || var4.getStart() >= var3.getEnd();
					boolean flag14 = var1.getStart() >= var4.getEnd() || var4.getStart() >= var1.getEnd();
					boolean flag23 = var2.getStart() >= var3.getEnd() || var3.getStart() >= var2.getEnd();
					if ((flag12 || flag34) && (flag14 || flag23)) {
						return false;
					}
					return true;
				}
			}
		} else if (gold_grounding.size() < pred_grounding.size()) {
			Variable var1 = gold_grounding.get(0);
			Variable var2 = pred_grounding.get(0);
			Variable var3 = pred_grounding.get(1);
			if (var1.getStart() >= var2.getEnd() || var2.getStart() >= var1.getEnd()) {
				;
			} else {
				return true;
			}
			if (var1.getStart() >= var3.getEnd() || var3.getStart() >= var1.getEnd()) {
				;
			} else {
				return true;
			}
		}
		return true;
	}

}
