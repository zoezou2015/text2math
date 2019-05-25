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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import com.statnlp.commons.types.Instance;

/**
 * The base class for representing networks. This class is equipped with
 * algorithm to calculate the inside-outside score, which is also a
 * generalization to the forward-backward score.<br>
 * You might want to use {@link TableLookupNetwork} for more functions such as
 * adding nodes and edges.
 * 
 * @see TableLookupNetwork
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class Network implements Serializable, HyperGraph {

	public static enum NODE_TYPE {
		sum, max
	};

	private static final long serialVersionUID = -3630379919120581209L;

	/** The working array for each thread for calculating inside scores */
	protected static double[][] insideSharedArray = new double[NetworkConfig.NUM_THREADS][]; // TODO: The value of
																								// NetworkConfig.NUM_THREADS
																								// might change after
																								// first access to
																								// Network class
	/** The working array for each thread for calculating outside scores */
	protected static double[][] outsideSharedArray = new double[NetworkConfig.NUM_THREADS][];
	/** The working array for each thread for calculating max scores */
	protected static double[][] maxSharedArray = new double[NetworkConfig.NUM_THREADS][];
	/** The working array for each thread for calculating cost */
	protected static double[][] costSharedArray = new double[NetworkConfig.NUM_THREADS][];
	/**
	 * The working array for each thread for storing max paths (for backtracking)
	 */
	protected static int[][][] maxPathsSharedArrays = new int[NetworkConfig.NUM_THREADS][][];
	/** The working array for each thread for calculating max k scores */
	protected static double[][][] maxKSharedArray = new double[NetworkConfig.NUM_THREADS][][];
	/**
	 * The working array for each thread for storing max k paths (for backtracking)
	 */
	protected static int[][][][] maxKPathsSharedArrays = new int[NetworkConfig.NUM_THREADS][][][];
	/**
	 * The working array for each thread for storing max k paths (for backtracking)
	 */
	protected static int[][][][] maxKPathsListBestSharedArrays = new int[NetworkConfig.NUM_THREADS][][][];

	/** The IDs associated with the network (within the scope of the thread). */
	protected int _networkId;
	/** The id of the thread */
	protected int _threadId;
	/** The instance */
	protected transient Instance _inst;
	/** The weight */
	protected transient double _weight;
	/** The feature parameters */
	protected transient LocalNetworkParam _param;

	/** At each index, store the node's inside score */
	protected transient double[] _inside;
	/** At each index, store the node's outside score */
	protected transient double[] _outside;
	/** At each index, store the score of the max tree */
	protected transient double[] _max;
	/** Stores the paths associated with the above tree */
	protected transient int[][] _max_paths;
	/** At each index, store the score of the max k tree */
	protected transient double[][] _max_k;
	/** Stores the paths associated with the above tree */
	protected transient int[][][] _max_k_paths;
	/** Stores the paths best list associated with the above tree */
	protected transient int[][][] _max_k_path_listbest;
	/** To mark whether a node has been visited in one iteration */
	protected transient boolean[] _visited;
	/** The marginal score for each node */
	protected transient double[] _marginal;

	/** The compiler that created this network */
	protected NetworkCompiler _compiler;
	/** The labeled version of this network, if exists, null otherwise */
	private Network _labeledNetwork;
	/** The unlabeled version of this network, if exists, null otherwise */
	private Network _unlabeledNetwork;

	/**
	 * Default constructor. Note that the network constructed using this default
	 * constructor is lacking the {@link LocalNetworkParam} object required for
	 * actual use. Use this only for generating generic network, which is later
	 * actualized using another constructor.
	 * 
	 * @see #Network(int, Instance, LocalNetworkParam)
	 */
	public Network() {
	}

	/**
	 * Construct a network
	 * 
	 * @param networkId
	 * @param inst
	 * @param param
	 */
	public Network(int networkId, Instance inst, LocalNetworkParam param) {
		this(networkId, inst, param, null);
	}

	/**
	 * Construct a network, specifying the NetworkCompiler that created this network
	 * 
	 * @param networkId
	 * @param inst
	 * @param param
	 * @param compiler
	 */
	public Network(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler) {
		this._networkId = networkId;
		this._threadId = param.getThreadId();
		this._inst = inst;
		this._weight = this._inst.getWeight();
		this._param = param;
		this._compiler = compiler;
	}

	protected double[] getInsideSharedArray() {
		if (insideSharedArray[this._threadId] == null || this.countNodes() > insideSharedArray[this._threadId].length)
			insideSharedArray[this._threadId] = new double[this.countNodes()];
		return insideSharedArray[this._threadId];
	}

	protected double[] getOutsideSharedArray() {
		if (outsideSharedArray[this._threadId] == null || this.countNodes() > outsideSharedArray[this._threadId].length)
			outsideSharedArray[this._threadId] = new double[this.countNodes()];
		return outsideSharedArray[this._threadId];
	}

	protected double[] getMaxSharedArray() {
		if (maxSharedArray[this._threadId] == null || this.countNodes() > maxSharedArray[this._threadId].length)
			maxSharedArray[this._threadId] = new double[this.countNodes()];
		return maxSharedArray[this._threadId];
	}

	protected double[] getCostSharedArray() {
		if (costSharedArray[this._threadId] == null || this.countNodes() > costSharedArray[this._threadId].length)
			costSharedArray[this._threadId] = new double[this.countNodes()];
		return costSharedArray[this._threadId];
	}

	protected int[][] getMaxPathSharedArray() {
		if (maxPathsSharedArrays[this._threadId] == null
				|| this.countNodes() > maxPathsSharedArrays[this._threadId].length)
			maxPathsSharedArrays[this._threadId] = new int[this.countNodes()][];
		return maxPathsSharedArrays[this._threadId];
	}

	protected int[][][] getMaxKPathSharedArray() {
		if (maxKPathsSharedArrays[this._threadId] == null
				|| this.countNodes() > maxKPathsSharedArrays[this._threadId].length)
			maxKPathsSharedArrays[this._threadId] = new int[this.countNodes()][][];
		return maxKPathsSharedArrays[this._threadId];
	}

	protected double[][] getMaxKSharedArray() {
		if (maxKSharedArray[this._threadId] == null || this.countNodes() > maxKSharedArray[this._threadId].length)
			maxKSharedArray[this._threadId] = new double[this.countNodes()][];
		return maxKSharedArray[this._threadId];
	}

	protected int[][][] getMaxKPathListBestSharedArray() {
		if (maxKPathsListBestSharedArrays[this._threadId] == null
				|| this.countNodes() > maxKPathsListBestSharedArrays[this._threadId].length)
			maxKPathsListBestSharedArrays[this._threadId] = new int[this.countNodes()][][];
		return maxKPathsListBestSharedArrays[this._threadId];
	}

	public int getNetworkId() {
		return this._networkId;
	}

	public int getThreadId() {
		return this._threadId;
	}

	/**
	 * Returns the instance modeled by this network
	 * 
	 * @return
	 */
	public Instance getInstance() {
		return this._inst;
	}

	/**
	 * Returns the compiler that compiled this network
	 * 
	 * @return
	 */
	public NetworkCompiler getCompiler() {
		return this._compiler;
	}

	/**
	 * Sets the compiler that compiled this network
	 * 
	 * @param compiler
	 */
	public void setCompiler(NetworkCompiler compiler) {
		this._compiler = compiler;
	}

	/**
	 * Returns the labeled network related to this network<br>
	 * If this network represents a labeled network, this will return itself
	 * 
	 * @return
	 */
	public Network getLabeledNetwork() {
		if (getInstance().isLabeled()) {
			return this;
		}
		return this._labeledNetwork;
	}

	/**
	 * Sets the labeled network related to this network
	 * 
	 * @param network
	 */
	public void setLabeledNetwork(Network network) {
		this._labeledNetwork = network;
	}

	/**
	 * Returns the unlabeled network related to this network<br>
	 * If this network represents an unlabeled network, this will return itself
	 * 
	 * @return
	 */
	public Network getUnlabeledNetwork() {
		if (!getInstance().isLabeled()) {
			return this;
		}
		return this._unlabeledNetwork;
	}

	/**
	 * Sets the unlabeled network related to this network
	 * 
	 * @param network
	 */
	public void setUnlabeledNetwork(Network network) {
		this._unlabeledNetwork = network;
	}

	/**
	 * Returns the inside score for the root node
	 * 
	 * @return
	 */
	public double getInside() {
		return this._inside[this.countNodes() - 1];
	}

	/**
	 * Return the marginal score for the network at a specific index (Note: do not
	 * support SSVM yet)
	 * 
	 * @param k
	 * @return
	 */
	public double getMarginal(int k) {
		return this._marginal[k];
	}

	/**
	 * Return the maximum score for this network (which is the max score for the
	 * root node)
	 * 
	 * @return
	 */
	public double getMax() {
		int rootIdx = this.countNodes() - 1;
		return this._max[rootIdx];
	}

	/**
	 * Return the maximum score for this network ending in the node with the
	 * specified index
	 * 
	 * @param k
	 * @return
	 */
	public double getMax(int k) {
		return this._max[k];
	}

	/**
	 * Return the children of the hyperedge which is part of the maximum path of
	 * this network
	 * 
	 * @return
	 */
	public int[] getMaxPath() {
		return this._max_paths[this.countNodes() - 1];
	}

	/**
	 * Return the children of the hyperedge which is part of the maximum path of
	 * this network ending at the node at the specified index
	 * 
	 * @return
	 */
	public int[] getMaxPath(int k) {
		return this._max_paths[k];
	}

	public double getMaxTopK(int nodeIdx, int k) {
		return this._max_k[nodeIdx][k];
	}

	public int[] getMaxTopKPath(int nodeIdx, int k) {
		return this._max_k_paths[nodeIdx][k];
	}

	public int[] getMaxTopKBestListPath(int nodeIdx, int k) {
		return this._max_k_path_listbest[nodeIdx][k];
	}

	/**
	 * Calculate the marginal score for all nodes
	 */
	public void marginal() {
		this._marginal = new double[this.countNodes()];
		double sum = this.sum();
		this.outside();
		Arrays.fill(this._marginal, Double.NEGATIVE_INFINITY);
		for (int k = 0; k < this.countNodes(); k++) {
			this.marginal(k, sum);
		}
	}

	/**
	 * Calculate the marginal score at the specific node
	 * 
	 * @param k
	 */
	protected void marginal(int k, double sum) {
		if (this.isRemoved(k)) {
			this._marginal[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		// since inside and outside are in log space
		this._marginal[k] = this._inside[k] + this._outside[k] - sum;
	}

	/**
	 * Get the sum of the network (i.e., the inside score)
	 * 
	 * @return
	 */
	public double sum() {
		this.inside();
		return this.getInside();
	}

	/**
	 * Train the network
	 */
	public void train() {
		if (this._weight == 0)
			return;
		if (NetworkConfig.MODEL_TYPE.USE_SOFTMAX) {
			this.inside();
			this.outside();
		} else { // Use real max
			this.max();
		}
		this.updateGradient();
		this.updateObjective();
	}

	/**
	 * Calculate the inside score of all nodes
	 */
	protected void inside() {
		this._inside = this.getInsideSharedArray();
		Arrays.fill(this._inside, 0.0);
		for (int k = 0; k < this.countNodes(); k++) {
			this.inside(k);
		}

		if (this.getInside() == Double.NEGATIVE_INFINITY) {
			throw new RuntimeException("Error: network (ID=" + _networkId + ") has zero inside score");
		}
	}

	/**
	 * Calculate the outside score of all nodes
	 */
	protected void outside() {
		this._outside = this.getOutsideSharedArray();
		Arrays.fill(this._outside, Double.NEGATIVE_INFINITY);
		for (int k = this.countNodes() - 1; k >= 0; k--) {
			this.outside(k);
		}
	}

	public void updateGradient(double[] gradientArray) {

	}

	/**
	 * Calculate and update the inside-outside score of all nodes
	 */
	protected void updateGradient() {
		if (NetworkConfig.MODEL_TYPE.USE_SOFTMAX) {
			for (int k = 0; k < this.countNodes(); k++) {
				this.updateGradient(k);
			}
		} else { // Use real max
			// Max is already calculated
			int rootIdx = this.countNodes() - 1;
			resetVisitedMark();
			this.updateGradient(rootIdx);
		}
	}

	private void resetVisitedMark() {
		this._visited = new boolean[countNodes()];
		for (int i = 0; i < this._visited.length; i++) {
			this._visited[i] = false;
		}
	}

	protected void updateObjective() {
		double objective = 0.0;
		if (NetworkConfig.MODEL_TYPE.USE_SOFTMAX) {
			objective = this.getInside() * this._weight;
		} else { // Use real max
			objective = this.getMax() * this._weight;
		}
		this._param.addObj(objective);
	}

	/**
	 * Goes through each nodes in the network to gather list of features
	 */
	public synchronized void touch() {
		for (int k = 0; k < this.countNodes(); k++)
			this.touch(k);
	}

	/**
	 * Calculate the maximum score for all nodes
	 */
	public void max() {
		this._max = this.getMaxSharedArray();
		this._max_paths = this.getMaxPathSharedArray();
		for (int k = 0; k < this.countNodes(); k++) {
			this.max(k);
		}
	}

	/**
	 * Calculate the inside score for the specified node
	 * 
	 * @param k
	 */
	protected void inside(int k) {
		if (this.isRemoved(k)) {
			this._inside[k] = Double.NEGATIVE_INFINITY;
			return;
		}

		double inside = 0.0;
		int[][] childrenList_k = this.getChildren(k);

		// If this node has no child edge, assume there is one edge with no child node
		// This is done so that every node is visited in the feature extraction step
		// below
		if (childrenList_k.length == 0) {
			childrenList_k = new int[1][0];
		}

		{
			int children_k_index = 0;
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for (int child_k : children_k) {
				if (this.isRemoved(child_k)) {
					ignoreflag = true;
				}
			}
			if (ignoreflag) {
				inside = Double.NEGATIVE_INFINITY;
			} else {
				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				double score = fa.getScore(this._param);
				if (NetworkConfig.MODEL_TYPE.USE_COST) {
					score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
				}
				for (int child_k : children_k) {
					score += this._inside[child_k];
				}
				inside = score;
			}
		}

		for (int children_k_index = 1; children_k_index < childrenList_k.length; children_k_index++) {
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for (int child_k : children_k) {
				if (this.isRemoved(child_k)) {
					ignoreflag = true;
				}
			}
			if (ignoreflag)
				continue;

			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			double score = fa.getScore(this._param);
			if (NetworkConfig.MODEL_TYPE.USE_COST) {
				score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
			}
			for (int child_k : children_k) {
				score += this._inside[child_k];
			}

			inside = sumLog(inside, score);
		}

		this._inside[k] = inside;

		if (this._inside[k] == Double.NEGATIVE_INFINITY)
			this.remove(k);
	}

	/**
	 * Calculate the outside score for the specified node
	 * 
	 * @param k
	 */
	protected void outside(int k) {
		if (this.isRemoved(k)) {
			this._outside[k] = Double.NEGATIVE_INFINITY;
			return;
		} else
			this._outside[k] = this.isRoot(k) ? 0.0 : this._outside[k];

		if (this._inside[k] == Double.NEGATIVE_INFINITY)
			this._outside[k] = Double.NEGATIVE_INFINITY;

		int[][] childrenList_k = this.getChildren(k);
		for (int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++) {
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for (int child_k : children_k)
				if (this.isRemoved(child_k)) {
					ignoreflag = true;
					break;
				}
			if (ignoreflag)
				continue;

			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			double score = fa.getScore(this._param);
			if (NetworkConfig.MODEL_TYPE.USE_COST) {
				score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
			}
			score += this._outside[k];
			for (int child_k : children_k) {
				score += this._inside[child_k];
			}

			if (score == Double.NEGATIVE_INFINITY)
				continue;

			for (int child_k : children_k) {
				double v1 = this._outside[child_k];
				double v2 = score - this._inside[child_k];
				this._outside[child_k] = sumLog(v1, v2);
			}
		}

		if (this._outside[k] == Double.NEGATIVE_INFINITY) {
			this.remove(k);
		}
	}

	/**
	 * Calculate and update the gradient for features present at the specified node
	 * 
	 * @param k
	 */
	protected void updateGradient(int k) {
		if (this.isRemoved(k))
			return;

		int[][] childrenList_k = this.getChildren(k);
		int[] maxChildren = null;
		if (!NetworkConfig.MODEL_TYPE.USE_SOFTMAX) {
			if (this._visited[k])
				return;
			this._visited[k] = true;
			maxChildren = this.getMaxPath(k); // For Structured SVM
		}

		for (int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++) {
			double count = 0.0;
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for (int child_k : children_k) {
				if (this.isRemoved(child_k)) {
					ignoreflag = true;
					break;
				}
			}
			if (!NetworkConfig.MODEL_TYPE.USE_SOFTMAX) { // Consider only max path
				if (!Arrays.equals(children_k, maxChildren)) {
					continue;
				}
			}
			if (ignoreflag) {
				continue;
			}

			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			if (NetworkConfig.MODEL_TYPE.USE_SOFTMAX) {
				double score = fa.getScore(this._param); // w*f
				if (NetworkConfig.MODEL_TYPE.USE_COST) {
					score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
				}
				score += this._outside[k]; // beta(s')
				for (int child_k : children_k) {
					score += this._inside[child_k]; // alpha(s)
				}
				double normalization = this.getInside();
				count = Math.exp(score - normalization); // Divide by normalization term Z
			} else { // Use real max
				count = 1;
			}
			count *= this._weight;

			fa.update(this._param, count);
			if (!NetworkConfig.MODEL_TYPE.USE_SOFTMAX) {
				for (int child_k : children_k) {
					this.updateGradient(child_k);
				}
			}
		}
	}

	/**
	 * Gather features from the specified node
	 * 
	 * @param k
	 */
	protected void touch(int k) {
		if (this.isRemoved(k))
			return;

		int[][] childrenList_k = this.getChildren(k);
		for (int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++) {
			int[] children_k = childrenList_k[children_k_index];
			this._param.extract(this, k, children_k, children_k_index);
		}
	}

	/**
	 * Calculate the maximum score at the specified node
	 * 
	 * @param k
	 */
	protected void max(int k) {
		if (this.isRemoved(k)) {
			this._max[k] = Double.NEGATIVE_INFINITY;
			return;
		}

		if (this.isSumNode(k)) {

			double inside = 0.0;
			int[][] childrenList_k = this.getChildren(k);

			if (childrenList_k.length == 0) {
				childrenList_k = new int[1][0];
			}

			{
				int children_k_index = 0;
				int[] children_k = childrenList_k[children_k_index];

				boolean ignoreflag = false;
				for (int child_k : children_k)
					if (this.isRemoved(child_k))
						ignoreflag = true;
				if (ignoreflag) {
					inside = Double.NEGATIVE_INFINITY;
				} else {
					FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
					double score = fa.getScore(this._param);
					if (NetworkConfig.MODEL_TYPE.USE_COST) {
						try {
							score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
						} catch (NullPointerException e) {
							System.err.println(
									"WARNING: Compiler was not specified during network creation, setting cost to 0.0");
						}
					}
					for (int child_k : children_k) {
						score += this._max[child_k];
					}
					inside = score;
				}

				// if it is a sum node, then any path is the same for such a node.
				// this is something you need to make sure when constructing such a network.
				this._max_paths[k] = children_k;
			}

			for (int children_k_index = 1; children_k_index < childrenList_k.length; children_k_index++) {
				int[] children_k = childrenList_k[children_k_index];

				boolean ignoreflag = false;
				for (int child_k : children_k)
					if (this.isRemoved(child_k))
						ignoreflag = true;
				if (ignoreflag)
					continue;

				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				double score = fa.getScore(this._param);
				if (NetworkConfig.MODEL_TYPE.USE_COST) {
					try {
						score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
					} catch (NullPointerException e) {
						System.err.println(
								"WARNING: Compiler was not specified during network creation, setting cost to 0.0");
					}
				}
				for (int child_k : children_k) {
					score += this._max[child_k];
				}

				inside = sumLog(inside, score);

			}

			this._max[k] = inside;
		} else {
			int[][] childrenList_k = this.getChildren(k);
			this._max[k] = Double.NEGATIVE_INFINITY;

			for (int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++) {
				int[] children_k = childrenList_k[children_k_index];
				boolean ignoreflag = false;
				for (int child_k : children_k)
					if (this.isRemoved(child_k)) {
						ignoreflag = true;
						break;
					}
				if (ignoreflag)
					continue;

				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				double score = fa.getScore(this._param);
				if (NetworkConfig.MODEL_TYPE.USE_COST) {
					try {
						score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
					} catch (NullPointerException e) {
						System.err.println(
								"WARNING: Compiler was not specified during network creation, setting cost to 0.0");
					}
				}
				for (int child_k : children_k) {
					score += this._max[child_k];
				}
				if (score >= this._max[k]) {
					this._max[k] = score;
					this._max_paths[k] = children_k;
				}
			}
		}
	}

	/**
	 * Using the normal approach, each node we maintain a top-k list. top-k viterbi
	 * decoding.
	 */
	protected void topK() {
		int topK = NetworkConfig._topKValue;
		this._max_k = getMaxKSharedArray();
		this._max_k_paths = getMaxKPathSharedArray();
		this._max_k_path_listbest = getMaxKPathListBestSharedArray();
		for (int nodeIdx = 0; nodeIdx < this.countNodes(); nodeIdx++) {
			this._max_k[nodeIdx] = new double[NetworkConfig._topKValue];
			this._max_k_paths[nodeIdx] = new int[NetworkConfig._topKValue][];
			this._max_k_path_listbest[nodeIdx] = new int[NetworkConfig._topKValue][];
			this.askKBest(nodeIdx, topK);
		}
	}

	/**
	 * Ask the k^{th} best of nodeIdx, currently specific for CKY-styly parsing
	 * 
	 * @param nodeIdx
	 * @param q
	 */
	protected void askKBest(int nodeIdx, int TOPK) {
		int[][] childrenList_k = this.getChildren(nodeIdx);
		for (int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++) {
			int[] children_k = childrenList_k[children_k_index];
			boolean ignoreflag = false;
			for (int child_k : children_k)
				if (this.isRemoved(child_k)) {
					ignoreflag = true;
					break;
				}
			if (ignoreflag)
				continue;

			BinaryHeap heap = new BinaryHeap(NetworkConfig._topKValue + 1);
			int n = 0;

			int currMaxPath[][] = new int[TOPK][children_k.length]; // topk and (l-best, r-best)

			FeatureArray fa = this._param.extract(this, nodeIdx, children_k, children_k_index);
			double score = fa.getScore(this._param);
			double firstBest = score;
			for (int child_k : children_k) {
				firstBest += this._max_k[child_k][0];
			}
			int[] firstBestListIdx = new int[children_k.length];
			Arrays.fill(firstBestListIdx, 0);
			ValueIndexPair vip = new ValueIndexPair(firstBest, firstBestListIdx); // first best of left, first best of
																					// right
			heap.add(vip);
			HashSet<IndexPair> beenPushedSet = new HashSet<>();
			beenPushedSet.add(new IndexPair(firstBestListIdx));
			if (children_k.length == 0) {
				vip = heap.removeMax();
				currMaxPath[0] = vip.bestListIdx;
				for (int k = 1; k < TOPK; k++)
					currMaxPath[k] = null;
			} else {
				while (n < TOPK) {
					vip = heap.removeMax();
					if (vip.val == Double.NEGATIVE_INFINITY)
						break;
					currMaxPath[n] = vip.bestListIdx;
					n++;
					if (n >= TOPK)
						break;

					for (int ith = 0; ith < vip.bestListIdx.length; ith++) {
						int[] listIdx = vip.bestListIdx.clone();
						IndexPair ip = new IndexPair(listIdx);
						ip.set(ith, listIdx[ith] + 1);
						if (!beenPushedSet.contains(ip)) {
							double kbestScore = 0;
							for (int ck = 0; ck < children_k.length; ck++) {
								kbestScore += ith == ck ? this._max_k[children_k[ck]][vip.bestListIdx[ck] + 1]
										: this._max_k[children_k[ck]][vip.bestListIdx[ck]];
							}
							kbestScore += score;
							heap.add(new ValueIndexPair(kbestScore, ip.indices));
							beenPushedSet.add(ip);
						}
					}
				}
				for (int x = n; x < TOPK; x++)
					currMaxPath[x] = null;
			}
			// merge two topK vectors into one
			if (children_k_index == 0) {
				this._max_k_path_listbest[nodeIdx] = currMaxPath;
				for (int tk = 0; tk < this._max_k_path_listbest[nodeIdx].length; tk++) {
					if (this._max_k_path_listbest[nodeIdx][tk] == null) {
						this._max_k_paths[nodeIdx][tk] = null;
						this._max_k[nodeIdx][tk] = Double.NEGATIVE_INFINITY;
						continue;
					}
					this._max_k_paths[nodeIdx][tk] = children_k;
					double tkbest = 0;
					int c = 0;
					for (int child_k : children_k) {
						tkbest += this._max_k[child_k][this._max_k_path_listbest[nodeIdx][tk][c++]];
					}
					this._max_k[nodeIdx][tk] = tkbest + score;
				}
			} else {
				this._max_k_path_listbest[nodeIdx] = this.merge(currMaxPath, children_k, nodeIdx, score);
			}

		}
	}

	/**
	 * 
	 * @param currMaxPath:
	 *            nth best, and the best pair from child
	 * @param globalMaxKPath
	 * @param maxKScore[nodeIdx][kthbest]:
	 *            only know the children
	 * @return
	 */
	private int[][] merge(int currMaxPath[][], int[] children, int nodeIdx, double score) {
		int[][] answer = new int[NetworkConfig._topKValue][children.length];// pair is two
		int[][] answerPath = new int[NetworkConfig._topKValue][children.length];
		int i = 0, j = 0, k = 0;
		while (k < answer.length) {
			// System.err.println("node idx:"+nodeIdx+" i:"+i+" j:"+j);
			double left = Double.NEGATIVE_INFINITY;
			if (currMaxPath[i] != null) {
				left = 0;
				for (int ith = 0; ith < children.length; ith++) {
					left += this._max_k[children[ith]][currMaxPath[i][ith]];
				}
			}

			int[] pathChildren = this._max_k_paths[nodeIdx][j] == null ? null : this._max_k_paths[nodeIdx][j];

			double right = Double.NEGATIVE_INFINITY;
			if (!(pathChildren == null || this._max_k_path_listbest[nodeIdx][j] == null)) {
				right = 0;
				for (int pth = 0; pth < pathChildren.length; pth++)
					right += this._max_k[pathChildren[pth]][this._max_k_path_listbest[nodeIdx][j][pth]];
			}

			if (left == Double.NEGATIVE_INFINITY && right == Double.NEGATIVE_INFINITY) {
				break;
			}

			if (left > right) {
				answer[k] = currMaxPath[i];
				this._max_k[nodeIdx][k] = left + score;
				answerPath[k] = children;
				i++;
			} else {
				answer[k] = this._max_k_path_listbest[nodeIdx][j];
				this._max_k[nodeIdx][k] = right + score;
				System.arraycopy(this._max_k_paths[nodeIdx][j], 0, answerPath[k], 0,
						this._max_k_paths[nodeIdx][j].length); // a faster way to copy array
				// answerPath[k] = this._max_k_paths[nodeIdx][j].clone();
				j++;
			}
			k++;

		}
		for (int x = k; x < answer.length; x++) {
			answer[x] = null;
			answerPath[x] = null;
		}
		this._max_k_paths[nodeIdx] = answerPath;
		return answer;
	}

	private double sumLog(double inside, double score) {
		double v1 = inside;
		double v2 = score;
		if (v1 == v2 && v2 == Double.NEGATIVE_INFINITY) {
			return Double.NEGATIVE_INFINITY;
		} else if (v1 == v2 && v2 == Double.POSITIVE_INFINITY) {
			return Double.POSITIVE_INFINITY;
		} else if (v1 > v2) {
			return Math.log1p(Math.exp(v2 - v1)) + v1;
		} else {
			return Math.log1p(Math.exp(v1 - v2)) + v2;
		}
	}

	/**
	 * Count the number of removed nodes
	 */
	public int countRemovedNodes() {
		int count = 0;
		for (int k = 0; k < this.countNodes(); k++)
			if (this.isRemoved(k))
				count++;
		return count;
	}

	/**
	 * Get the root node of the network.
	 * 
	 * @return
	 */
	public long getRoot() {
		return this.getNode(this.countNodes() - 1);
	}

	/**
	 * Get the array form of the node at the specified index in the node array
	 */
	@Override
	public int[] getNodeArray(int k) {
		long node = this.getNode(k);
		return NetworkIDMapper.toHybridNodeArray(node);
	}

	// this ad-hoc method is useful when performing
	// some special sum operations (in conjunction with max operations)
	// in the decoding phase.
	protected boolean isSumNode(int k) {
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.countNodes(); i++)
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this.getNode(i))));
		return sb.toString();
	}

}
