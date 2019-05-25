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
package com.statnlp.example.sp.main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.example.sp.GeoqueryEvaluator;
import com.statnlp.example.sp.HybridGrammar;
import com.statnlp.example.sp.HybridGrammarReader;
import com.statnlp.example.sp.SemTextDataManager;
import com.statnlp.example.sp.SemTextFeatureManager_Discriminative;
import com.statnlp.example.sp.SemTextInstance;
import com.statnlp.example.sp.SemTextInstanceReader;
import com.statnlp.example.sp.SemTextNetworkCompiler;
import com.statnlp.example.sp.SemanticForest;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.neural.NeuralConfigReader;

public class SemTextExperimenter_Discriminative_Neural {

	static boolean debug = false;
	static boolean skipTest = false;
	static boolean printFeats = false;
	static boolean testOnTrain = false;
	static boolean precomputeTestFeatureIdx = false;
	static String lang;
	static double learningRate = 0.01;
	static int numIterations = 100;
	static String modelPath = "";
	static String savePrefix = "";
	static String neuralSavePrefix = "";
	static String optim = "adagrad";
	static String pretrainPath = "";
	static boolean fixPretrain = false;
	static boolean validation = false;
	static boolean isDecoding = false;
	static String testSet = "test";

	private static void parse(String args[]) throws FileNotFoundException {
		// set default values
		NetworkConfig.REGULARIZE_NEURAL_FEATURES = false;
		NetworkConfig.USE_NEURAL_FEATURES = false;
		NetworkConfig.OPTIMIZE_NEURAL = false;
		NetworkConfig.REPLACE_ORIGINAL_EMISSION = false;
		NeuralConfigReader.readConfig("neural_server/neural.sp.config");
		NeuralConfig.FIX_EMBEDDING = false;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;

		int i = 0;
		while (i < args.length) {
			String opt = args[i];
			if (opt.equals("-config")) {
				NeuralConfigReader.readConfig(args[++i]);
			} else if (opt.equals("-lang")) {
				lang = args[++i];
				NeuralConfig.LANGUAGE = lang;
			} else if (opt.equals("-thread")) {
				NetworkConfig.NUM_THREADS = Integer.parseInt(args[++i]);
			} else if (opt.equals("-neural")) {
				NetworkConfig.USE_NEURAL_FEATURES = true;
				precomputeTestFeatureIdx = true;
			} else if (opt.equals("-lr")) {
				learningRate = Double.parseDouble(args[++i]);
				NeuralConfig.LEARNING_RATE = learningRate;
			} else if (opt.equals("-iter")) {
				numIterations = Integer.parseInt(args[++i]);
			} else if (opt.equals("-save-iter")) {
				NetworkConfig.SAVE_MODEL_AFTER_ITER = Integer.parseInt(args[++i]);
			} else if (opt.equals("-model")) {
				modelPath = args[++i];
			} else if (opt.equals("-neural-save-prefix")) {
				neuralSavePrefix = args[++i];
			} else if (opt.equals("-debug")) {
				debug = true;
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
			} else if (opt.equals("-l2")) {
				NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(args[++i]);
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
				testSet = args[++i]; // "test", "syn"
				isDecoding = true;
			} else {
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
			i++;
		}
	}

	private static void printSettings() {
		System.out.println("STATNLP CONFIGURATIONS:");
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

	public static void main(String args[]) throws Exception {

		System.err.println(SemTextExperimenter_Discriminative_Neural.class.getCanonicalName());

		parse(args);
		printSettings();

		String inst_filename = "data/geoquery/geoFunql-" + lang + ".corpus";
		String init_filename = "data/geoquery/geoFunql-" + lang + ".init.corpus";
		String g_filename = "data/hybridgrammar.txt";

		boolean isTrain = modelPath.equals("");

		String train_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/train-N600";// +args[1];

		if (validation) {
			train_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/train-400";
		}
		String test_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/test";

		if (debug) {
			train_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/train-N1";// +args[1];
			test_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/test-2";
		}
		if (validation) {
			test_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/dev-200";
		}

		boolean isGeoquery = true;

		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;

		SemTextDataManager dm = new SemTextDataManager();

		ArrayList<SemTextInstance> inits = SemTextInstanceReader.readInit(init_filename, dm);
		ArrayList<SemTextInstance> insts_train = SemTextInstanceReader.read(inst_filename, dm, train_ids, true);
		ArrayList<SemTextInstance> insts_test_clone = null;
		ArrayList<SemTextInstance> insts_test = null;

		if (testSet.equals("syn")) {
			String syn_inst_filename = "data/synonyms/geoFunql-" + lang + "-syn.corpus";
			insts_test_clone = SemTextInstanceReader.read(syn_inst_filename, dm, test_ids, false);
			insts_test = SemTextInstanceReader.read(syn_inst_filename, dm, test_ids, false);
		} else { // standard test set
			insts_test_clone = SemTextInstanceReader.read(inst_filename, dm, test_ids, false);
			insts_test = SemTextInstanceReader.read(inst_filename, dm, test_ids, false);
		}

		int size = insts_train.size();
		if (NetworkConfig.TRAIN_MODE_IS_GENERATIVE) {
			size += inits.size();
		}

		SemTextInstance train_instances[] = new SemTextInstance[size];
		for (int k = 0; k < insts_train.size(); k++) {
			train_instances[k] = insts_train.get(k);
			train_instances[k].setInstanceId(k);
			train_instances[k].setLabeled();
		}

		int testSize = insts_test.size();
		SemTextInstance all_instances[] = new SemTextInstance[size + testSize];
		int i = 0;
		for (; i < size; i++) {
			all_instances[i] = train_instances[i];
		}
		int lastId = all_instances[i - 1].getInstanceId();
		for (int j = 0; j < testSize; j++, i++) {
			all_instances[i] = insts_test_clone.get(j);
			all_instances[i].setInstanceId(lastId + j + 1);
			all_instances[i].setUnlabeled();
		}

		if (NetworkConfig.TRAIN_MODE_IS_GENERATIVE) {
			for (int k = 0; k < inits.size(); k++) {
				train_instances[k + insts_train.size()] = inits.get(k);
				train_instances[k + insts_train.size()].setInstanceId(k + insts_train.size());
				train_instances[k + insts_train.size()].setLabeled();
			}
		}

		System.err.println("Read.." + train_instances.length + " instances.");

		HybridGrammar g = HybridGrammarReader.read(g_filename);

		SemanticForest forest_global = SemTextInstanceReader.toForest(dm);

		GlobalNetworkParam param_G;
		if (!isTrain) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath));
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
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pretrainPath));
			pretrain_param_G = (GlobalNetworkParam) ois.readObject();
			ois.close();
			param_G.setPretrainParams(pretrain_param_G, fixPretrain);
		}

		SemTextFeatureManager_Discriminative fm = new SemTextFeatureManager_Discriminative(param_G, g, dm);

		SemTextNetworkCompiler compiler = new SemTextNetworkCompiler(g, forest_global, dm);

		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler)
				: DiscriminativeNetworkModel.create(fm, compiler);

		if (isTrain) {
			String modelName = lang;
			if (!savePrefix.equals("")) {
				modelName = savePrefix;
				if (!savePrefix.endsWith("." + lang)) {
					modelName += "." + lang;
				}
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
					neuralModelName = neuralSavePrefix;
					if (!neuralSavePrefix.endsWith("." + lang)) {
						neuralModelName += "." + lang;
					}
				}
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

		// skip decoding
		if (skipTest) {
			System.exit(0);
		}

		SemTextInstance test_instances[];
		Instance[] output_instances_unlabeled;

		test_instances = new SemTextInstance[insts_test.size()];
		for (int k = 0; k < test_instances.length; k++) {
			test_instances[k] = insts_test.get(k);
			test_instances[k].setInstanceId(k + 1);
			test_instances[k].setUnlabeled();
		}
		output_instances_unlabeled = model.decode(test_instances);

		if (printFeats) {
			System.out.println("Unknown features:");
			for (String f : fm.getParam_G().getUnknownFeatures()) {
				System.out.println(f);
			}
		}

		double total = output_instances_unlabeled.length;
		double corr = 0;

		GeoqueryEvaluator eval = new GeoqueryEvaluator();

		ArrayList<String> expts = new ArrayList<>();
		ArrayList<String> preds = new ArrayList<>();

		for (int k = 0; k < output_instances_unlabeled.length; k++) {
			Instance output_inst_U = output_instances_unlabeled[k];
			boolean r = output_inst_U.getOutput().equals(output_inst_U.getPrediction());
			System.err.println(output_inst_U.getInstanceId() + ":\t" + r);
			if (r) {
				corr++;
			}
			System.err.println("=INPUT=");
			System.err.println(output_inst_U.getInput());
			System.err.println("=OUTPUT=");
			System.err.println(output_inst_U.getOutput());
			System.err.println("=PREDICTION=");
			System.err.println(output_inst_U.getPrediction());

			String expt = eval.toGeoQuery((SemanticForest) output_inst_U.getOutput());
			String pred = eval.toGeoQuery((SemanticForest) output_inst_U.getPrediction());

			expts.add(expt);
			preds.add(pred);

			if (isGeoquery) {
				System.err.println("output:\t" + expt);
				System.err.println("predic:\t" + pred);
			}
		}

		System.err.println("text accuracy=" + corr / total + "=" + corr + "/" + total);
		eval.eval(preds, expts);

	}

}