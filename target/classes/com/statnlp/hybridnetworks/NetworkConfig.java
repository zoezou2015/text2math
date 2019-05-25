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

public class NetworkConfig {

	/**
	 * The enumeration of available model type<br>
	 * <ul>
	 * <li>{@link #STRUCTURED_PERCEPTRON} (<tt>USE_COST=false</tt>,
	 * <tt>USE_SOFTMAX=false</tt>)</li>
	 * <li>{@link #CRF} (<tt>USE_COST=false</tt>, <tt>USE_SOFTMAX=true</tt>)</li>
	 * <li>{@link #SSVM} (<tt>USE_COST=true</tt>, <tt>USE_SOFTMAX=false</tt>)</li>
	 * <li>{@link #SOFTMAX_MARGIN} (<tt>USE_COST=true</tt>,
	 * <tt>USE_SOFTMAX=true</tt>)</li>
	 * </ul>
	 * 
	 * Each model has two boolean parameters: {@link #USE_COST} and
	 * {@link #USE_SOFTMAX}.<br>
	 * <tt>USE_COST</tt> determines whether the cost function is used, while
	 * <tt>USE_SOFTMAX</tt> determines whether the softmax function is used instead
	 * of max.
	 */
	public static enum ModelType {
		STRUCTURED_PERCEPTRON(false, false), CRF(false, true), SSVM(true, false), SOFTMAX_MARGIN(true, true),;

		public final boolean USE_COST;
		public final boolean USE_SOFTMAX;

		private ModelType(boolean useCost, boolean useSoftmax) {
			USE_COST = useCost;
			USE_SOFTMAX = useSoftmax;
		}
	}

	/**
	 * The value to initialize the weights to if {@link #RANDOM_INIT_WEIGHT} is
	 * <tt>false</tt>.
	 */
	public static double FEATURE_INIT_WEIGHT = 0;
	/**
	 * Whether to initialize the weight vector randomly or fixed to
	 * {@link #FEATURE_INIT_WEIGHT}.
	 */
	public static boolean RANDOM_INIT_WEIGHT = true;
	/**
	 * The seed for random weight vector initialization (for reproducibility)
	 */
	public static int RANDOM_INIT_FEATURE_SEED = 1234;

	/**
	 * Whether generative training is used instead of discriminative
	 */
	public static boolean TRAIN_MODE_IS_GENERATIVE = false;
	/**
	 * The L2 regularization parameter
	 */
	public static double L2_REGULARIZATION_CONSTANT = 0.01;

	/**
	 * Network is the core of StatNLP framework.<br>
	 * This defines the default capacity for defining the nodes of the network<br>
	 * For more information, see {@link Network}
	 * 
	 * @see Network
	 */
	public static final int[] DEFAULT_CAPACITY_NETWORK = new int[] { 4096, 4096, 4096, 4096, 4096 };

	/**
	 * The value used for stopping criterion of change in objective value in
	 * generative models
	 */
	public static double OBJTOL = 10e-15;
	/** @deprecated Use {@link #OBJTOL} instead */
	@Deprecated
	public static final double objtol = OBJTOL;

	public static boolean DEBUG_MODE = false;

	/**
	 * The model type used for learning.<br>
	 * The options are in {@link ModelType}
	 */
	public static ModelType MODEL_TYPE = ModelType.CRF;
	/** Whether to use batch */
	public static boolean USE_BATCH_TRAINING = false;
	/**
	 * Batch size for batch training (if {@link #USE_BATCH_TRAINING} is
	 * <tt>true</tt>) for each thread
	 */
	public static int BATCH_SIZE = 20;
	/** @deprecated Use {@link #BATCH_SIZE} instead */
	@Deprecated
	public static final int batchSize = 20;
	public static int RANDOM_BATCH_SEED = 2345;

	/** @deprecated Use {@link #USE_BATCH_TRAINING} instead */
	@Deprecated
	public static final boolean USE_BATCH_SGD = false;

	/** The weight of the cost function for SSVM and Softmax-Margin */
	public static double MARGIN = 0.5;
	/**
	 * A flag whether to normalize the default cost function in cost-based models
	 * like {@link ModelType#SSVM} and {@link ModelType#SOFTMAX_MARGIN}<br>
	 * 
	 * This is one of the three flags for controlling default cost function:
	 * <ul>
	 * <li>{@link #NORMALIZE_COST}</li>
	 * <li>{@link #EDGE_COST}</li>
	 * <li>{@link #NODE_COST}</li>
	 * </ul>
	 */
	public static boolean NORMALIZE_COST = false;
	/**
	 * The cost for having node mismatch in cost-based models like
	 * {@link ModelType#SSVM} and {@link ModelType#SOFTMAX_MARGIN}<br>
	 * 
	 * This is one of the three flags for controlling default cost function:
	 * <ul>
	 * <li>{@link #NORMALIZE_COST}</li>
	 * <li>{@link #EDGE_COST}</li>
	 * <li>{@link #NODE_COST}</li>
	 * </ul>
	 */
	public static double NODE_COST = 1.0;
	/**
	 * The cost for having edge mismatch in cost-based models like
	 * {@link ModelType#SSVM} and {@link ModelType#SOFTMAX_MARGIN}<br>
	 * 
	 * This is one of the three flags for controlling default cost function:
	 * <ul>
	 * <li>{@link #NORMALIZE_COST}</li>
	 * <li>{@link #EDGE_COST}</li>
	 * <li>{@link #NODE_COST}</li>
	 * </ul>
	 */
	public static double EDGE_COST = 0.0;

	/**
	 * Whether features are cached during training.<br>
	 * Without caching training might be very slow.
	 */
	public static boolean CACHE_FEATURES_DURING_TRAINING = true;
	/**
	 * Build features in parallel during the touch process
	 */
	public static boolean PARALLEL_FEATURE_EXTRACTION = false;
	/**
	 * Build features only from labeled instances
	 */
	public static boolean BUILD_FEATURES_FROM_LABELED_ONLY = false;

	/** @deprecated Use {@link #CACHE_FEATURES_DURING_TRAINING} instead */
	@Deprecated
	public static final boolean _CACHE_FEATURES_DURING_TRAINING = true;
	/** @deprecated Use {@link #PARALLEL_FEATURE_EXTRACTION} instead */
	@Deprecated
	public static final boolean _SEQUENTIAL_FEATURE_EXTRACTION = true;
	/** @deprecated Use {@link #BUILD_FEATURES_FROM_LABELED_ONLY} instead */
	@Deprecated
	public static final boolean _BUILD_FEATURES_FROM_LABELED_ONLY = false;

	/**
	 * The number of threads to be used for parallel execution
	 */
	public static int NUM_THREADS = 4;
	/** @deprecated Use {@link #NUM_THREADS} instead */
	@Deprecated
	public static final int _numThreads = NUM_THREADS;

	/** Decoding the max-marginal for each node as well. if set to true */
	public static boolean MAX_MARGINAL_DECODING = false;

	public static int _topKValue = 1;

	public static boolean RANDOM_BATCH = true;

	/***
	 * Please read carefully about the README.txt to install the NN server and also
	 * the communication package for Neural CRF
	 */
	/** If enable the neural CRF model, set it true. */
	public static boolean USE_NEURAL_FEATURES = false;
	/**
	 * Regularized the neural features in CRF or not. set to false then can be done
	 * by dropout
	 ***/
	public static boolean REGULARIZE_NEURAL_FEATURES = false;
	/** If true: Optimized the neural net in CRF **/
	public static boolean OPTIMIZE_NEURAL = false; // false means not update the neural network parameters in CRF.
	/** false: the feature is the word itself. true: word is the indexed word **/
	public static boolean IS_INDEXED_NEURAL_FEATURES = false;

	// semantic settings
	public static int _FOREST_MAX_HEIGHT = 10000;
	public static int _FOREST_MAX_WIDTH = 10000;
	public static int _NETWORK_MAX_DEPTH = 901;
	public static int _nGRAM = 1;// 2;//1;

	public static int _SEMANTIC_FOREST_MAX_DEPTH = 20;// the max depth of the forest when creating the semantic forest.
	public static int _SEMANTIC_PARSING_NGRAM = 1;// 2;
	public static int NEURAL_WINDOW_SIZE = 0;

	public static boolean REBUILD_FOREST_EVERY_TIME = false;

	public static int SAVE_MODEL_AFTER_ITER = 10;

	public static int _maxSpanLen = 2;// the upper-bound of the length of a span.

	public static boolean REPLACE_ORIGINAL_EMISSION = false;

	public static boolean RATIO_STOP_CRIT = false;

	public static boolean EquationParser = false;
	public static boolean MathSolver = false;
}