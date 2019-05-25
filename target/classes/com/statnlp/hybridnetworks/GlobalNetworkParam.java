/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

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
package com.statnlp.hybridnetworks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.statnlp.commons.ml.opt.LBFGS;
import com.statnlp.commons.ml.opt.LBFGS.ExceptionWithIflag;
import com.statnlp.commons.ml.opt.MathsVector;
import com.statnlp.commons.ml.opt.Optimizer;
import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.neural.NNCRFGlobalNetworkParam;
import com.statnlp.neural.RemoteNN;

//TODO: other optimization and regularization methods. Such as the L1 regularization.

/**
 * The set of parameters (such as weights, training method, optimizer, etc.) in
 * the global scope
 * 
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public class GlobalNetworkParam implements Serializable {

	private static final long serialVersionUID = -1216927656396018976L;

	// these parameters are used for discriminative training using LBFGS.
	/** The L2 regularization parameter weight */
	protected transient double _kappa;
	/** The optimizer */
	protected transient Optimizer _opt;
	/** The optimizer factory */
	protected transient OptimizerFactory _optFactory;
	/** The gradient for each dimension */
	protected transient double[] _counts;
	/** A variable to store previous value of the objective function */
	protected transient double _obj_old;
	/** A variable to store current value of the objective function */
	protected transient double _obj;
	/** A variable for batch SGD optimization, if applicable */
	protected transient int _batchSize;

	protected transient int _version;

	/**
	 * Map from feature type to [a map from output to [a map from input to feature
	 * ID]]
	 */
	protected HashMap<String, HashMap<String, HashMap<String, Integer>>> _featureIntMap;
	/** Map from feature type to input */
	protected HashMap<String, ArrayList<String>> _type2inputMap;
	/**
	 * A feature int map (similar to {@link #_featureIntMap}) for each local thread
	 */
	protected ArrayList<HashMap<String, HashMap<String, HashMap<String, Integer>>>> _subFeatureIntMaps;
	/** The size of each feature int maps for each local thread */
	protected int[] _subSize;

	protected String[][] _feature2rep;// three-dimensional array representation of the feature.
	/** The weights parameter */
	protected double[] _weights;
	/** Store the best weights when using the batch sgd */
	protected double[] _bestWeight;
	/** A flag whether the model is discriminative */
	protected boolean _isDiscriminative;
	/**
	 * The current number of features that will be updated as the process goes.
	 * 
	 * @see #_fixedFeaturesSize
	 */
	protected int _size;
	/**
	 * The final number of features
	 * 
	 * @see #_size
	 */
	protected int _fixedFeaturesSize;
	/** A flag describing whether the set of features is already fixed */
	protected boolean _locked = false;

	/**
	 * A counter for how many consecutive times the decrease in objective value is
	 * less than 0.01%
	 */
	protected int smallChangeCount = 0;
	/**
	 * The total number of instances for the coefficient the batch SGD
	 * regularization term
	 */
	protected int totalNumInsts;

	/** Neural CRF socket server controller */
	protected NNCRFGlobalNetworkParam _nnController;
	/**
	 * The weights that some of them will be replaced by neural net if NNCRF is
	 * enabled.
	 */
	private transient double[] concatWeights, concatCounts;

	/** Initialize weights with an existing model */
	protected GlobalNetworkParam _pretrainG;
	protected boolean _isFixedPretrain;

	/** A set of features only seen in test set */
	protected HashSet<String> unknownSet;

	public HashSet<String> getUnknownFeatures() {
		return unknownSet;
	}

	public GlobalNetworkParam() {
		this(OptimizerFactory.getLBFGSFactory());
	}

	public GlobalNetworkParam(OptimizerFactory optimizerFactory) {
		this._locked = false;
		this._version = -1;
		this._size = 0;
		this._fixedFeaturesSize = 0;
		this._obj_old = Double.NEGATIVE_INFINITY;
		this._obj = Double.NEGATIVE_INFINITY;
		this._isDiscriminative = !NetworkConfig.TRAIN_MODE_IS_GENERATIVE;
		if (this.isDiscriminative()) {
			this._batchSize = NetworkConfig.BATCH_SIZE;
			this._kappa = NetworkConfig.L2_REGULARIZATION_CONSTANT;
		}
		this._featureIntMap = new HashMap<>();
		this._type2inputMap = new HashMap<>();
		this._optFactory = optimizerFactory;
		if (NetworkConfig.PARALLEL_FEATURE_EXTRACTION && NetworkConfig.NUM_THREADS > 1) {
			this._subFeatureIntMaps = new ArrayList<>();
			for (int i = 0; i < NetworkConfig.NUM_THREADS; i++) {
				this._subFeatureIntMaps.add(new HashMap<String, HashMap<String, HashMap<String, Integer>>>());
			}
			this._subSize = new int[NetworkConfig.NUM_THREADS];
		}
	}

	/**
	 * Get the map from feature type to [a map from output to [a map from input to
	 * feature ID]]
	 * 
	 * @return
	 */
	public HashMap<String, HashMap<String, HashMap<String, Integer>>> getFeatureIntMap() {
		return this._featureIntMap;
	}

	public double[] getWeights() {
		return this._weights;
	}

	/**
	 * Return the current number of features
	 * 
	 * @see #countFixedFeatures()
	 * @return
	 */
	public int countFeatures() {
		return this._size;
	}

	/**
	 * Return the final number of features
	 * 
	 * @return
	 * @see #countFeatures()
	 */
	public int countFixedFeatures() {
		return this._fixedFeaturesSize;
	}

	public boolean isFixed(int f_global) {
		return f_global < this._fixedFeaturesSize;
	}

	/**
	 * Return the String[] representation of the feature with the specified index
	 * 
	 * @param f_global
	 * @return
	 */
	public String[] getFeatureRep(int f_global) {
		return this._feature2rep[f_global];
	}

	/**
	 * Add certain value to the specified feature (identified by the id)
	 * 
	 * @param feature
	 * @param count
	 */
	public synchronized void addCount(int feature, double count) {
		if (Double.isNaN(count)) {
			throw new RuntimeException("count is NaN.");
		}

		if (this.isFixed(feature))
			return;
		// if the model is discriminative model, we will flip the sign for
		// the counts because we will need to use LBFGS.
		if (this.isDiscriminative()) {
			this._counts[feature] -= count;
		} else {
			this._counts[feature] += count;
		}

	}

	public synchronized void addObj(double obj) {
		this._obj += obj;
	}

	public double getObj() {
		return this._obj;
	}

	public double getObj_old() {
		return this._obj_old;
	}

	private double getCount(int f) {
		return this._counts[f];
	}

	public double getWeight(int f) {
		// if the feature is just newly created, for example, return the initial weight,
		// which is zero.
		// if(f>=this._weights.length)
		// return NetworkConfig.FEATURE_INIT_WEIGHT;
		return this._weights[f];
	}

	/**
	 * Set a weight at the specified index if it is not fixed yet
	 * 
	 * @param f
	 * @param weight
	 * @see #overRideWeight(int, double)
	 */
	public synchronized void setWeight(int f, double weight) {
		if (this.isFixed(f))
			return;
		this._weights[f] = weight;
	}

	/**
	 * Force set a weight at the specified index
	 * 
	 * @param f
	 * @param weight
	 * @see #setWeight(int, double)
	 */
	public synchronized void overRideWeight(int f, double weight) {
		this._weights[f] = weight;
	}

	public void unlock() {
		if (!this.isLocked())
			throw new RuntimeException("This param is not locked.");
		this._locked = false;
	}

	public void unlockForNewFeaturesAndFixCurrentFeatures() {
		if (!this.isLocked())
			throw new RuntimeException("This param is not locked.");
		this.fixCurrentFeatures();
		this._locked = false;
	}

	public void fixCurrentFeatures() {
		this._fixedFeaturesSize = this._size;
	}

	/**
	 * Expand the feature set to include possible combinations not seen during
	 * training. Only works for non-discriminative model
	 */
	private void expandFeaturesForGenerativeModelDuringTesting() {
		// this.unlockForNewFeaturesAndFixCurrentFeatures();

		// if it is a discriminative model, then do not expand the features.
		if (this.isDiscriminative()) {
			return;
		}

		System.err.println("==EXPANDING THE FEATURES===");
		System.err.println("Before expansion:" + this.size());
		Iterator<String> types = this._featureIntMap.keySet().iterator();
		while (types.hasNext()) {
			String type = types.next();
			HashMap<String, HashMap<String, Integer>> output2input = this._featureIntMap.get(type);
			ArrayList<String> inputs = this._type2inputMap.get(type);
			System.err.println("Feature of type " + type + " has " + inputs.size() + " possible inputs.");
			Iterator<String> outputs = output2input.keySet().iterator();
			while (outputs.hasNext()) {
				String output = outputs.next();
				for (String input : inputs) {
					this.toFeature(type, output, input);
				}
			}
		}
		System.err.println("After expansion:" + this.size());

		// this.lockIt();
	}

	public void lockItAndKeepExistingFeatureWeights() {
		Random r = new Random(NetworkConfig.RANDOM_INIT_FEATURE_SEED);

		if (this.isLocked())
			return;

		if (NetworkConfig.TRAIN_MODE_IS_GENERATIVE) {
			this.expandFeaturesForGenerativeModelDuringTesting();
		}

		double[] weights_new = new double[this._size];
		this._counts = new double[this._size];
		for (int k = 0; k < this._weights.length; k++) {
			weights_new[k] = this._weights[k];
		}
		for (int k = this._weights.length; k < this._size; k++) {
			weights_new[k] = NetworkConfig.RANDOM_INIT_WEIGHT ? (r.nextDouble() - .5) / 10
					: NetworkConfig.FEATURE_INIT_WEIGHT;
		}
		this._weights = weights_new;
		this.resetCountsAndObj();

		this._feature2rep = new String[this._size][];
		Iterator<String> types = this._featureIntMap.keySet().iterator();
		while (types.hasNext()) {
			String type = types.next();
			HashMap<String, HashMap<String, Integer>> output2input = this._featureIntMap.get(type);
			Iterator<String> outputs = output2input.keySet().iterator();
			while (outputs.hasNext()) {
				String output = outputs.next();
				HashMap<String, Integer> input2id = output2input.get(output);
				Iterator<String> inputs = input2id.keySet().iterator();
				while (inputs.hasNext()) {
					String input = inputs.next();
					int id = input2id.get(input);
					this._feature2rep[id] = new String[] { type, output, input };
				}
			}
		}
		this._version = 0;
		if (!NetworkConfig.USE_NEURAL_FEATURES)
			this._opt = this._optFactory.create(this._weights.length, getFeatureIntMap());
		else
			this._opt = this._optFactory.create(_nnController.getNonNeuralAndInternalNeuralSize(), getFeatureIntMap());
		this._locked = true;

		System.err.println(this._size + " features.");

	}

	/**
	 * Lock current features. If this is locked it means no new features will be
	 * allowed.
	 */
	public void lockIt() {
		Random r = new Random(NetworkConfig.RANDOM_INIT_FEATURE_SEED);

		if (this.isLocked())
			return;

		this.expandFeaturesForGenerativeModelDuringTesting();

		double[] weights_new = new double[this._size];
		this._counts = new double[this._size];
		for (int k = 0; k < this._fixedFeaturesSize; k++) {
			weights_new[k] = this._weights[k];
		}
		for (int k = this._fixedFeaturesSize; k < this._size; k++) {
			weights_new[k] = NetworkConfig.RANDOM_INIT_WEIGHT ? (r.nextDouble() - .5) / 10
					: NetworkConfig.FEATURE_INIT_WEIGHT;
		}
		this._weights = weights_new;

		// initialize NN params and gradParams
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			_nnController = new NNCRFGlobalNetworkParam(this);
			_nnController.setRemoteNN(new RemoteNN(NetworkConfig.OPTIMIZE_NEURAL));
			_nnController.initializeInternalNeuralWeights();
			// if(NeuralConfig.NUM_LAYER == 0 && NeuralConfig.EMBEDDING_SIZE.get(0)==0){
			// _nnController.setInternalNeuralWeights(_weights);
			// }

		}

		this._feature2rep = new String[this._size][];
		Iterator<String> types = this._featureIntMap.keySet().iterator();
		while (types.hasNext()) {
			String type = types.next();
			HashMap<String, HashMap<String, Integer>> output2input = this._featureIntMap.get(type);
			Iterator<String> outputs = output2input.keySet().iterator();
			while (outputs.hasNext()) {
				String output = outputs.next();
				HashMap<String, Integer> input2id = output2input.get(output);
				Iterator<String> inputs = input2id.keySet().iterator();
				while (inputs.hasNext()) {
					String input = inputs.next();
					int id = input2id.get(input);
					this._feature2rep[id] = new String[] { type, output, input };
				}
			}
		}

		// resetCountsAndObj() will call copyPretrainWeights only if isFixedPretrain ==
		// true
		// so we still need to call this for the first time
		if (_pretrainG != null) {
			copyPretrainWeights();
		}

		this.resetCountsAndObj();

		this._version = 0;
		if (!NetworkConfig.USE_NEURAL_FEATURES)
			this._opt = this._optFactory.create(this._weights.length, getFeatureIntMap());
		else
			this._opt = this._optFactory.create(_nnController.getNonNeuralAndInternalNeuralSize(), getFeatureIntMap());
		this._locked = true;

		System.err.println(this._size + " features.");

	}

	public int size() {
		return this._size;
	}

	public boolean isLocked() {
		return this._locked;
	}

	public int getVersion() {
		return this._version;
	}

	public int toFeature(String type, String output, String input) {
		return this.toFeature(null, type, output, input);
	}

	/**
	 * Converts a tuple of feature type, input, and output into the feature index.
	 * 
	 * @param type
	 *            The feature type (e.g., "EMISSION", "FEATURE_1", etc.)
	 * @param output
	 *            The string representing output label associated with this feature.
	 *            Note that this does not have to be the surface form of the label,
	 *            as any distinguishing string value will work (so, instead of "NN",
	 *            "DT", you can just as well put the indices, like "0", "1")
	 * @param input
	 *            The input (e.g., for emission feature in HMM this might be the
	 *            word itself)
	 * @return
	 */
	public int toFeature(Network network, String type, String output, String input) { // process later , if threadId =
																						// −1, global mode.
		int threadId = network != null ? network.getThreadId() : -1;
		boolean shouldNotCreateNewFeature = false;
		try {
			shouldNotCreateNewFeature = (NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY
					&& network.getInstance().getInstanceId() < 0);
		} catch (NullPointerException e) {
			throw new NetworkException(
					"Missing network on some toFeature calls while trying to extract only from labeled networks.");
		}
		HashMap<String, HashMap<String, HashMap<String, Integer>>> featureIntMap = null;
		if (!NetworkConfig.PARALLEL_FEATURE_EXTRACTION || NetworkConfig.NUM_THREADS == 1 || this.isLocked()) {
			featureIntMap = this._featureIntMap;
		} else {
			if (threadId == -1) {
				throw new NetworkException("Missing network on some toFeature calls while in parallel touch.");
			}
			featureIntMap = this._subFeatureIntMaps.get(threadId);
		}

		// if it is locked, then we might return a dummy feature
		// if the feature does not appear to be present.
		if (this.isLocked() || shouldNotCreateNewFeature) {
			if (!featureIntMap.containsKey(type)) {
				return -1;
			}
			HashMap<String, HashMap<String, Integer>> output2input = featureIntMap.get(type);
			if (!output2input.containsKey(output)) {
				return -1;
			}
			HashMap<String, Integer> input2id = output2input.get(output);
			if (!input2id.containsKey(input)) {
				if (unknownSet == null)
					unknownSet = new HashSet<>();
				unknownSet.add(type + " " + output + " " + input);
				return -1;
			}
			return input2id.get(input);
		}

		Instance inst = network.getInstance();
		int instId = inst.getInstanceId();
		boolean isTestInst = instId > 0 && !inst.isLabeled() || !inst.getLabeledInstance().isLabeled();
		if (isTestInst && !type.equals("neural"))
			type = "test";

		if (!featureIntMap.containsKey(type)) {
			featureIntMap.put(type, new HashMap<String, HashMap<String, Integer>>());
		}

		HashMap<String, HashMap<String, Integer>> outputToInputToIdx = featureIntMap.get(type);
		if (!outputToInputToIdx.containsKey(output)) {
			outputToInputToIdx.put(output, new HashMap<String, Integer>());
		}

		HashMap<String, Integer> inputToIdx = outputToInputToIdx.get(output);
		if (!inputToIdx.containsKey(input)) {
			if (!NetworkConfig.PARALLEL_FEATURE_EXTRACTION || NetworkConfig.NUM_THREADS == 1) {
				inputToIdx.put(input, this._size++);
			} else {
				inputToIdx.put(input, this._subSize[threadId]++);
			}
		}
		return inputToIdx.get(input);
	}

	/**
	 * Globally update the parameters. This will also set {@link #_obj_old} to the
	 * value of {@link #_obj}.
	 * 
	 * @return true if the optimization is deemed to be finished, false otherwise
	 */
	public synchronized boolean update() {
		boolean done;
		if (this.isDiscriminative()) {
			done = this.updateDiscriminative();
		} else {
			done = this.updateGenerative();
		}

		this._obj_old = this._obj;

		return done;
	}

	public double[] getCounts() {
		return this._counts;
	}

	/**
	 * Update the weights using generative algorithm (e.g., for HMM)
	 * 
	 * @return true if the difference between previous and current objective
	 *         function value is less than {@link NetworkConfig#objtol}, false
	 *         otherwise.
	 */
	private boolean updateGenerative() {
		// HashMap<String, Double> word2count = new HashMap<String, Double>();

		Iterator<String> types = this._featureIntMap.keySet().iterator();
		while (types.hasNext()) {
			String type = types.next();
			HashMap<String, HashMap<String, Integer>> output2input = this._featureIntMap.get(type);

			Iterator<String> outputs = output2input.keySet().iterator();
			while (outputs.hasNext()) {
				String output = outputs.next();

				HashMap<String, Integer> input2feature;
				Iterator<String> inputs;

				double sum = 0;
				input2feature = output2input.get(output);
				inputs = input2feature.keySet().iterator();
				while (inputs.hasNext()) {
					String input = inputs.next();
					int feature = input2feature.get(input);
					sum += this.getCount(feature);
				}

				// if(Math.abs(1-sum)>1E-12){
				// System.err.println("sum="+sum+"\t"+type+"\t"+output);
				// }

				input2feature = output2input.get(output);
				inputs = input2feature.keySet().iterator();
				while (inputs.hasNext()) {
					String input = inputs.next();
					int feature = input2feature.get(input);
					double value = sum != 0 ? this.getCount(feature) / sum : 1.0 / input2feature.size();
					this.setWeight(feature, Math.log(value));

					// if(value>1E-15)
					// {
					// String s = Arrays.toString(this.getFeatureRep(feature));
					// if(s.indexOf("transition")!=-1 && s.indexOf("low_point_1")!=-1){
					// System.err.println(s+"\t"+value);
					// }
					// }

					if (Double.isNaN(Math.log(value))) {
						throw new RuntimeException(
								"x" + value + "\t" + this.getCount(feature) + "/" + sum + "\t" + input2feature.size());
					}
				}
			}
		}
		boolean done = Math.abs(this._obj - this._obj_old) < NetworkConfig.OBJTOL;

		this._version++;

		// System.err.println("Word2count:");
		// Iterator<String> words = word2count.keySet().iterator();
		// while(words.hasNext()){
		// String word = words.next();
		// double count = word2count.get(word);
		// System.err.println(word+"\t"+count);
		// }
		// System.exit(1);

		return done;
	}

	public List<Double> toList(double[] arr) {
		List<Double> result = new ArrayList<>();
		for (double num : arr) {
			result.add(num);
		}
		return result;
	}

	/**
	 * Update the parameters using discriminative algorithm (e.g., CRF). If the
	 * optimization seems to be done, it will return true.
	 * 
	 * @return true if the difference between previous objective value and current
	 *         objective value is less than {@link NetworkConfig#OBJTOL} or the
	 *         optimizer deems the optimization is finished, or the decrease is less
	 *         than 0.01% for three iterations, false otherwise.
	 */
	protected boolean updateDiscriminative() {
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (concatWeights == null) {
				int concatDim = _nnController.getNonNeuralAndInternalNeuralSize();
				concatWeights = new double[concatDim];
				concatCounts = new double[concatDim];
			}
			_nnController.getNonNeuralAndInternalNeuralWeights(concatWeights, concatCounts);

			this._opt.setVariables(concatWeights);
			this._opt.setGradients(concatCounts);
		} else {
			this._opt.setVariables(this._weights);
			this._opt.setGradients(this._counts);
		}

		this._opt.setObjective(-this._obj);

		boolean done = false;

		try {
			// The _weights parameters will be updated inside this optimize method.
			// This is possible since the _weights array is passed to the optimizer above,
			// and the optimizer will set the weights directly, as arrays are passed by
			// reference
			done = this._opt.optimize();
		} catch (ExceptionWithIflag e) {
			throw new NetworkException("Exception with Iflag:" + e.getMessage());
		}

		if (NetworkConfig.RATIO_STOP_CRIT && this._opt.name().contains("LBFGS Optimizer")) {
			double diff = this.getObj() - this.getObj_old();
			if (diff >= 0 && diff < NetworkConfig.OBJTOL) {
				done = true;
			}
			double diffRatio = Math.abs(diff / this.getObj_old());
			if (diff >= 0 && diffRatio < 1e-4) {
				this.smallChangeCount += 1;
			} else {
				this.smallChangeCount = 0;
			}
			if (this.smallChangeCount == 3) {
				done = true;
			}
		} else {
			if (Math.abs(this.getObj() - this.getObj_old()) < NetworkConfig.OBJTOL) {
				done = true;
			}
		}

		if (done && this._opt.name().contains("LBFGS Optimizer") && !NetworkConfig.USE_NEURAL_FEATURES) {
			// If we stop early, we need to copy solution_cache,
			// as noted in the Javadoc for solution_cache in LBFGS class.
			// This is because the _weights will contain the next value to be evaluated,
			// and so does not correspond to the current objective value.
			// In practice, though, the two are usually very close to each other (if we
			// are stopping near the solution), so not copying will also work.
			for (int i = 0; i < this._weights.length; i++) {
				this._weights[i] = LBFGS.solution_cache[i];
			}
		}

		if (NetworkConfig.USE_NEURAL_FEATURES) {
			_nnController.updateNonNeuralAndInternalNeuralWeights(concatWeights);
		}

		this._version++;
		return done;
	}

	public boolean isDiscriminative() {
		return this._isDiscriminative;
	}

	/**
	 * Set {@link #_counts} (the gradient) and {@link #_obj} to the regularization
	 * term, essentially zeroing the values to be updated with the model gradient
	 * and objective value.
	 */
	protected synchronized void resetCountsAndObj() {

		double coef = 1.0;
		if (NetworkConfig.USE_BATCH_TRAINING) {
			coef = this._batchSize * 1.0 / this.totalNumInsts;
			if (coef > 1)
				coef = 1.0;
		}

		for (int k = 0; k < this._size; k++) {
			this._counts[k] = 0.0;
			// for regularization
			if (this.isDiscriminative() && this._kappa > 0 && k >= this._fixedFeaturesSize) {
				if (NetworkConfig.USE_NEURAL_FEATURES && _nnController.isNNFeature(k))
					continue; // this weight is not really a parameter as it is provided from NN
				this._counts[k] += 2 * coef * this._kappa * this._weights[k];
			}
		}
		if (NetworkConfig.OPTIMIZE_NEURAL && NetworkConfig.USE_NEURAL_FEATURES) {
			// reset the internal feature weights here.
			double[] internalNNWeights = this._nnController.getInternalNeuralWeights();
			double[] internalNNCounts = this._nnController.getInternalNeuralGradients();
			for (int k = 0; k < internalNNWeights.length; k++) {
				internalNNCounts[k] = 0.0;
				if (NetworkConfig.REGULARIZE_NEURAL_FEATURES) {
					if (this.isDiscriminative() && this._kappa > 0) {
						internalNNCounts[k] += 2 * coef * this._kappa * internalNNWeights[k];
					}
				}
			}
		}

		for (int k = 0; k < this._size; k++) {
			if (_feature2rep[k][0].equals("test")) {
				this._weights[k] = 0;
				this._counts[k] = 0;
			}
		}

		// set pretrain weights to initial values
		if (_pretrainG != null && _isFixedPretrain) {
			copyPretrainWeights();
		}

		this._obj = 0.0;
		// for regularization
		if (this.isDiscriminative() && this._kappa > 0) {
			if (NetworkConfig.USE_NEURAL_FEATURES) {
				if (NetworkConfig.OPTIMIZE_NEURAL && NetworkConfig.REGULARIZE_NEURAL_FEATURES) {
					this._obj += MathsVector.square(this._nnController.getInternalNeuralWeights());
				}
				for (int k = 0; k < _weights.length; k++) {
					if (!_nnController.isNNFeature(k)) {
						this._obj += this._weights[k] * this._weights[k];
					}
				}
				this._obj *= -coef * this._kappa;
			} else {
				this._obj += -coef * this._kappa * MathsVector.square(this._weights);
			}
		}
		// NOTES:
		// for additional terms such as regularization terms:
		// always add to _obj the term g(x) you would like to maximize.
		// always add to _counts the NEGATION of the term g(x)'s gradient.
	}

	public NNCRFGlobalNetworkParam getNNCRFController() {
		return this._nnController;
	}

	public void setInstsNum(int number) {
		this.totalNumInsts = number;
	}

	public void setPretrainParams(GlobalNetworkParam pretrain_g, boolean isFixed) {
		this._pretrainG = pretrain_g;
		this._isFixedPretrain = isFixed;
	}

	public boolean isFixedPretrain() {
		return this._isFixedPretrain;
	}

	public void copyPretrainWeights() {
		if (_pretrainG != null) {
			double[] pretrainWeights = _pretrainG.getWeights();
			for (int i = 0; i < pretrainWeights.length; i++) {
				String[] featRep = _pretrainG.getFeatureRep(i);
				int featIdx = -1;
				try {
					featIdx = _featureIntMap.get(featRep[0]).get(featRep[1]).get(featRep[2]);
				} catch (NullPointerException ex) {
					continue;
				}
				if (featRep[0].equals("test")) {
					if (pretrainWeights[i] != 0.0) {
						System.err.println("test feature must have zero weights!");
						System.exit(1);
					}
				}
				_weights[featIdx] = pretrainWeights[i];
				_counts[featIdx] = 0;
			}
		}
	}

	public boolean checkEqual(GlobalNetworkParam p) {
		boolean v1 = Arrays.equals(this._weights, p._weights);
		boolean v2 = Arrays.deepEquals(this._feature2rep, p._feature2rep);
		return v1 && v2;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(this._featureIntMap);
		out.writeObject(this._feature2rep);
		out.writeObject(this._weights);
		out.writeInt(this._size);
		out.writeInt(this._fixedFeaturesSize);
		out.writeBoolean(this._locked);
		out.writeObject(this._nnController);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this._featureIntMap = (HashMap<String, HashMap<String, HashMap<String, Integer>>>) in.readObject();
		this._feature2rep = (String[][]) in.readObject();
		this._weights = (double[]) in.readObject();
		this._size = in.readInt();
		this._fixedFeaturesSize = in.readInt();
		this._locked = in.readBoolean();
		if (in.available() > 0) {
			this._nnController = (NNCRFGlobalNetworkParam) in.readObject();
		}
	}

}