package com.example.mathsolver;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.example.sp.HybridGrammar;
import com.statnlp.example.sp.HybridPattern;
import com.statnlp.example.sp.SemTextDataManager;
import com.statnlp.example.sp.SemTextNetwork;
import com.statnlp.example.sp.SemanticForest;
import com.statnlp.example.sp.SemanticForestNode;
import com.statnlp.example.sp.SemanticUnit;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class MathSolverNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5456278133972215766L;
	private HybridGrammar g;
	private SemTextDataManager dm;
	private SemanticForest forest;
	private int _maxSentLen = 45;
	private long[] _nodes;
	private int[][][] _children;
	private int[] _numNodesInSubStructure = new int[this._maxSentLen + 1];

	public MathSolverNetworkCompiler(HybridGrammar g, SemanticForest global_forest, SemTextDataManager dm) {
		this.g = g;
		this.forest = global_forest;
		this.dm = dm;
		this.forest.getAllNodes();
		int[] capacity = new int[] { 100, 100, 1000, 1000, 1000, 2 };
		NetworkIDMapper.setCapacity(capacity);
	}

	@Override
	public SemTextNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {

		MathInstance stInst = (MathInstance) inst;
		if (inst.isLabeled()) {
			return this.compile_labeled(networkId, stInst, param);
		} else {
			return this.compile_unlabeled(networkId, stInst, param);
		}

	}

	private SemTextNetwork compile_labeled(int networkId, MathInstance inst, LocalNetworkParam param) {

		SemTextNetwork network = new SemTextNetwork(networkId, inst, param);
		Sentence sent = inst.getInput();
		SemanticForest tree = inst.getOutput();
		return this.compile(network, sent, tree);

	}

	public SemTextNetwork compile(SemTextNetwork network, Sentence sent, SemanticForest tree) {

		for (int eIndex = 1; eIndex <= sent.length(); eIndex++) {
			for (int L = 1; L <= eIndex; L++) {
				int bIndex = eIndex - L;
				for (SemanticForestNode forestNode : tree.getAllNodes()) {

					if (forestNode.isRoot()) {
						// System.out.println("root node");
						continue;
					}
					// System.out.println("forestNode: " +
					// forestNode.getUnit().toString());
					// System.out.println("compile forestNode
					// "+forestNode.getName());
					// If it is a word, create a node with w pattern
					if (eIndex == bIndex + 1) {
						// System.out.println("create word nodes");

						long node = this.toNode(bIndex, eIndex, forestNode, this.g.getw());
						network.addNode(node);
						// System.out.println(
						// "create word nodes " +
						// Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
					}

					// If node's arity is 1, create a node with pattern X
					// create a child node with root pattern A/B/C
					// add nodes and edge to the network
					if (forestNode.arity() == 1) {
						// System.out.println("create nodes if arity == 1");

						SemanticForestNode[] childTreeNodes0 = forestNode.getChildren()[0];

						long node_X = this.toNode(bIndex, eIndex, forestNode, this.g.getX());
						boolean added = false;
						for (SemanticForestNode childForestNode : childTreeNodes0) {

							long node_child = this.toNode(bIndex, eIndex, childForestNode,
									this.g.getRootPatternByArity(childForestNode.arity()));
							// System.out.println("childForestNode " +
							// childForestNode.getForm() + " " + node_child);
							if (network.contains(node_child)) {
								if (!added) {
									network.addNode(node_X);
									// System.out.println("network.addNode(node_X)
									// ");
									added = true;
								}
								network.addEdge(node_X, new long[] { node_child });
							}
						}
					}
					// If node's arity is 2, create two nodes, one with pattern
					// X and the other with pattern Y
					// create left and right children respectively
					else if (forestNode.arity() == 2) {
						// System.out.println("create nodes if arity == 2");

						SemanticForestNode[] childTreeNodes0 = forestNode.getChildren()[0];
						SemanticForestNode[] childTreeNodes1 = forestNode.getChildren()[1];

						long node_X = this.toNode(bIndex, eIndex, forestNode, this.g.getX());
						boolean addedX = false;
						for (SemanticForestNode childForestNode : childTreeNodes0) {
							long node_child = this.toNode(bIndex, eIndex, childForestNode,
									this.g.getRootPatternByArity(childForestNode.arity()));
							if (network.contains(node_child)) {
								if (!addedX) {
									network.addNode(node_X);
									addedX = true;
								}
								network.addEdge(node_X, new long[] { node_child });
							}
						}

						long node_Y = this.toNode(bIndex, eIndex, forestNode, this.g.getY());
						boolean addedY = false;
						for (SemanticForestNode childForestNode : childTreeNodes1) {
							long node_child = this.toNode(bIndex, eIndex, childForestNode,
									this.g.getRootPatternByArity(childForestNode.arity()));
							if (network.contains(node_child)) {
								if (!addedY) {
									network.addNode(node_Y);
									addedY = true;
								}
								network.addEdge(node_Y, new long[] { node_child });
							}
						}
					}
					// add pattern nodes
					for (HybridPattern lhs : this.getValidHybridPatterns(forestNode)) {
						// System.out.println("<<<<<HybridPattern lhs:>>>>>" +
						// lhs.toString());

						long node = this.toNode(bIndex, eIndex, forestNode, lhs);
						boolean added = false;

						ArrayList<HybridPattern[]> RHS = this.g.getRHS(forestNode.arity(), lhs);
						// no edges to add for this pattern.
						if (lhs.isw()) {
							continue;
						}
						if (lhs.isX()) {
							continue;
						}
						if (lhs.isY()) {
							continue;
						}
						for (HybridPattern[] rhs : RHS) {

							if (rhs.length == 1) {
								// System.out.println("HybridPattern rhs == 1:"
								// + rhs[0].toString());

								long node_c1 = this.toNode(bIndex, eIndex, forestNode, rhs[0]);
								if (network.contains(node_c1)) {
									if (!added) {
										network.addNode(node);
										added = true;
									}
									network.addEdge(node, new long[] { node_c1 });
								}
							} else if (rhs.length == 2) {
								// System.out.println(
								// "HybridPattern rhs == 2: " +
								// rhs[0].toString() + " " + rhs[1].toString());

								for (int cIndex = bIndex + 1; cIndex < eIndex; cIndex++) {
									long node_c1 = this.toNode(bIndex, cIndex, forestNode, rhs[0]);
									long node_c2 = this.toNode(cIndex, eIndex, forestNode, rhs[1]);
									if (network.contains(node_c1) && network.contains(node_c2)) {
										if (!added) {
											network.addNode(node);
											added = true;
										}
										network.addEdge(node, new long[] { node_c1, node_c2 });
									}
								}
							} else {
								throw new RuntimeException("# rhs=" + Arrays.toString(rhs));
							}
						}
					}
				}
			}
		}

		long root = this.toNode_root(sent.length());
		network.addNode(root);

		SemanticForestNode[][] children_of_root = tree.getRoot().getChildren();

		if (children_of_root.length != 1)
			throw new RuntimeException("The root should have arity 1...");

		SemanticForestNode[] child_of_root = children_of_root[0];

		for (int k = 0; k < child_of_root.length; k++) {
			// System.out.println(child_of_root.length); length = 1
			// all root node starting with Query e.g. *n:Query -> ({ answer (
			// *n:State ) }) but with root pattern B
			// System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(root))+child_of_root[k].getName()+"\t"+child_of_root[0].arity()+"\t"+this._g.getRootPatternByArity(child_of_root[0].arity()));

			// those preroot nodes already exist in network
			long preroot = this.toNode(0, sent.length(), child_of_root[k],
					this.g.getRootPatternByArity(child_of_root[0].arity()));
			network.addEdge(root, new long[] { preroot });
		}
		network.finalizeNetwork();
		return network;
	}

	public SemTextNetwork compile_unlabeled(int networkId, Instance inst, LocalNetworkParam param) {
		MathInstance math_inst = (MathInstance) inst;
		SemTextNetwork network = new SemTextNetwork(networkId, math_inst, param);
		Sentence sent = math_inst.getInput();
		SemanticForest tree = math_inst.getOutput();
		// System.out.println(tree);
		for (int eIndex = 1; eIndex <= sent.length(); eIndex++) {
			for (int L = 1; L <= eIndex; L++) {
				int bIndex = eIndex - L;
				for (SemanticForestNode forestNode : this.forest.getAllNodes()) {

					if (forestNode.isRoot()) {
						// System.out.println("root node");
						continue;
					}
					// System.out.println("forestNode: " +
					// forestNode.getUnit().toString());
					// System.out.println("compile forestNode
					// "+forestNode.getName());
					// If it is a word, create a node with w pattern
					if (eIndex == bIndex + 1) {
						// System.out.println("create word nodes");

						long node = this.toNode(bIndex, eIndex, forestNode, this.g.getw());
						network.addNode(node);
						// System.out.println(
						// "create word nodes " +
						// Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
					}

					// If node's arity is 1, create a node with pattern X
					// create a child node with root pattern A/B/C
					// add nodes and edge to the network
					if (forestNode.arity() == 1) {
						// System.out.println("create nodes if arity == 1");

						SemanticForestNode[] childTreeNodes0 = forestNode.getChildren()[0];

						long node_X = this.toNode(bIndex, eIndex, forestNode, this.g.getX());
						boolean added = false;
						for (SemanticForestNode childForestNode : childTreeNodes0) {

							long node_child = this.toNode(bIndex, eIndex, childForestNode,
									this.g.getRootPatternByArity(childForestNode.arity()));
							// System.out.println("childForestNode " +
							// childForestNode.getForm() + " " + node_child);
							if (network.contains(node_child)) {
								if (!added) {
									network.addNode(node_X);
									// System.out.println("network.addNode(node_X)
									// ");
									added = true;
								}
								network.addEdge(node_X, new long[] { node_child });
							}
						}
					}
					// If node's arity is 2, create two nodes, one with pattern
					// X and the other with pattern Y
					// create left and right children respectively
					else if (forestNode.arity() == 2) {
						// System.out.println("create nodes if arity == 2");

						SemanticForestNode[] childTreeNodes0 = forestNode.getChildren()[0];
						SemanticForestNode[] childTreeNodes1 = forestNode.getChildren()[1];

						long node_X = this.toNode(bIndex, eIndex, forestNode, this.g.getX());
						boolean addedX = false;
						for (SemanticForestNode childForestNode : childTreeNodes0) {
							long node_child = this.toNode(bIndex, eIndex, childForestNode,
									this.g.getRootPatternByArity(childForestNode.arity()));
							if (network.contains(node_child)) {
								if (!addedX) {
									network.addNode(node_X);
									addedX = true;
								}
								network.addEdge(node_X, new long[] { node_child });
							}
						}

						long node_Y = this.toNode(bIndex, eIndex, forestNode, this.g.getY());
						boolean addedY = false;
						for (SemanticForestNode childForestNode : childTreeNodes1) {
							long node_child = this.toNode(bIndex, eIndex, childForestNode,
									this.g.getRootPatternByArity(childForestNode.arity()));
							if (network.contains(node_child)) {
								if (!addedY) {
									network.addNode(node_Y);
									addedY = true;
								}
								network.addEdge(node_Y, new long[] { node_child });
							}
						}
					}
					// add pattern nodes
					for (HybridPattern lhs : this.getValidHybridPatterns(forestNode)) {
						// System.out.println("<<<<<HybridPattern lhs:>>>>>" +
						// lhs.toString());

						long node = this.toNode(bIndex, eIndex, forestNode, lhs);
						boolean added = false;

						ArrayList<HybridPattern[]> RHS = this.g.getRHS(forestNode.arity(), lhs);
						// no edges to add for this pattern.
						if (lhs.isw()) {
							continue;
						}
						if (lhs.isX()) {
							continue;
						}
						if (lhs.isY()) {
							continue;
						}
						for (HybridPattern[] rhs : RHS) {

							if (rhs.length == 1) {
								// System.out.println("HybridPattern rhs == 1:"
								// + rhs[0].toString());

								long node_c1 = this.toNode(bIndex, eIndex, forestNode, rhs[0]);
								if (network.contains(node_c1)) {
									if (!added) {
										network.addNode(node);
										added = true;
									}
									network.addEdge(node, new long[] { node_c1 });
								}
							} else if (rhs.length == 2) {
								// System.out.println(
								// "HybridPattern rhs == 2: " +
								// rhs[0].toString() + " " + rhs[1].toString());

								for (int cIndex = bIndex + 1; cIndex < eIndex; cIndex++) {
									long node_c1 = this.toNode(bIndex, cIndex, forestNode, rhs[0]);
									long node_c2 = this.toNode(cIndex, eIndex, forestNode, rhs[1]);
									if (network.contains(node_c1) && network.contains(node_c2)) {
										if (!added) {
											network.addNode(node);
											added = true;
										}
										network.addEdge(node, new long[] { node_c1, node_c2 });
									}
								}
							} else {
								throw new RuntimeException("# rhs=" + Arrays.toString(rhs));
							}
						}
					}
				}
			}
		}

		long root = this.toNode_root(sent.length());
		network.addNode(root);

		SemanticForestNode[][] children_of_root = this.forest.getRoot().getChildren();

		if (children_of_root.length != 1)
			throw new RuntimeException("The root should have arity 1...");

		SemanticForestNode[] child_of_root = children_of_root[0];

		for (int k = 0; k < child_of_root.length; k++) {
			// System.out.println(child_of_root.length + "" + child_of_root[k]); // length =
			// 1
			// all root node starting with Query e.g. *n:Query -> ({ answer (
			// *n:State ) }) but with root pattern B
			// System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(root))+child_of_root[k].getName()+"\t"+child_of_root[0].arity()+"\t"+this._g.getRootPatternByArity(child_of_root[0].arity()));

			// those preroot nodes already exist in network
			long preroot = this.toNode(0, sent.length(), child_of_root[k],
					this.g.getRootPatternByArity(child_of_root[0].arity()));
			if (network.contains(preroot))
				// System.err.println(child_of_root[k].getUnit());
				network.addEdge(root, new long[] { preroot });
			// System.err.println("added");
		}
		network.finalizeNetwork();
		return network;
	}

	@Override
	public MathInstance decompile(Network network) {
		SemTextNetwork stNetwork = (SemTextNetwork) network;

		MathInstance inst = (MathInstance) stNetwork.getInstance();
		inst = inst.duplicate();

		// if the value is -inf, it means there is no prediction.
		if (stNetwork.getMax() == Double.NEGATIVE_INFINITY) {
			return inst;
		}
		// System.out.println("Decode a new instance>>>>>>");
		SemanticForest forest = this.toTree(stNetwork);
		inst.setPrediction(forest);

		return inst;

	}

	private SemanticForest toTree(SemTextNetwork network) {

		SemanticForestNode root = SemanticForestNode.createRootNode(MathSolverConfig._SEMANTIC_FOREST_MAX_DEPTH);
		this.toTree_helper(network, network.countNodes() - 1, root);
		return new SemanticForest(root);

	}

	/**
	 * 
	 * @param network
	 * @param node_k
	 *            the position index of currNode in network {@link #_nodes}
	 * @param currNode
	 *            the SemanticForestNode of node_k
	 */
	private void toTree_helper(SemTextNetwork network, int node_k, SemanticForestNode currNode) {
		// the node id of current SemanticForestNode currNode
		long node = network.getNode(node_k);
		Sentence sent = ((MathInstance) network.getInstance()).getInput();
		int[] ids_node = NetworkIDMapper.toHybridNodeArray(node);

		int[] children_k = network.getMaxPath(node_k); // length 0, 1, 2

		double score = network.getMax(node_k);
		// System.out.println("score" + score);
		if (currNode.getScore() == Double.NEGATIVE_INFINITY) {
			currNode.setScore(score);
			currNode.setInfo("info:" + Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
		}
		int eIndex = ids_node[0];
		int bIndex = ids_node[0] - ids_node[1];
		int wIndex = ids_node[3];
		String align_word = getWord(sent, bIndex, eIndex);
		currNode.setAlignment(align_word);
		if (this.dm.getAllUnits().get(wIndex).getMRL().equals("NUM")) {
			// System.out.println(this.dm.getAllUnits().get(wIndex).getMRL());
			String align_num = getNumberValue(sent, bIndex, eIndex);
			currNode.setValueString(align_num);
			double number = getNumber(sent, bIndex, eIndex);
			currNode.setNumValue(number);
		}
		if (this.dm.getAllUnits().get(wIndex).getMRL().equals("X")) {
			// System.out.println(this.dm.getAllUnits().get(wIndex).getMRL());
			ArrayList<WordToken> tokens = getWordTokens(sent, bIndex, eIndex);
			currNode.setTokens(tokens);
		}

		for (int child_k : children_k) {

			long child = network.getNode(child_k);
			int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
			// System.out.println(child_k+" childPattern
			// "+this._g.getPatternById(ids_child[4])+"
			// "+this._dm.getAllUnits().get(ids_child[3]));

			if (node_k == network.countNodes() - 1) { // root node
				SemanticUnit unit = this.dm.getAllUnits().get(ids_child[3]);
				SemanticForestNode childNode = new SemanticForestNode(unit, currNode.getHIndex() - 1);
				currNode.setChildren(0, new SemanticForestNode[] { childNode });
				this.toTree_helper(network, child_k, childNode);
			} else if (this.g.getX().getId() == ids_node[4]) {

				SemanticUnit unit = this.dm.getAllUnits().get(ids_child[3]);
				SemanticForestNode childNode = new SemanticForestNode(unit, currNode.getHIndex() - 1);
				currNode.setChildren(0, new SemanticForestNode[] { childNode });
				this.toTree_helper(network, child_k, childNode);
			} else if (this.g.getY().getId() == ids_node[4]) {
				SemanticUnit unit = this.dm.getAllUnits().get(ids_child[3]);
				SemanticForestNode childNode = new SemanticForestNode(unit, currNode.getHIndex() - 1);
				// System.out.println("3
				// Child:"+childNode.getUnit()+"\t"+childNode.getUnit().arity());
				// System.out.println("3:"+currNode.getUnit()+"\t"+childNode.getUnit()+"\t"+currNode.arity());
				currNode.setChildren(1, new SemanticForestNode[] { childNode });
				this.toTree_helper(network, child_k, childNode);
			} else {
				this.toTree_helper(network, child_k, currNode);
			}
		}

	}

	/**
	 * Return a array of hybrid pattern according to the arity of the given
	 * forestNode. Arity = 0, 1, 2
	 * 
	 * @param forestNode
	 * @return
	 */
	private HybridPattern[] getValidHybridPatterns(SemanticForestNode forestNode) {

		HybridPattern[] ps = this.g.getPatternsByArity(forestNode.arity());

		// System.err.println(forestNode.arity());
		// System.err.println(Arrays.toString(ps));

		return ps;
	}

	private long toNode_root(int sent_len) {
		return NetworkIDMapper.toHybridNodeID(new int[] { sent_len + 1, 0, 0, 0, 0, Network.NODE_TYPE.max.ordinal() });
	}

	private long toNode(int bIndex, int eIndex, SemanticForestNode node, HybridPattern p) {
		// okay, the weird problem is now fixed. due to the fact that there are
		// some new units which only appear in the test set.
		// if(12==eIndex && 12==eIndex-bIndex && 16==node.getHIndex() &&
		// 0==node.getWIndex() && 2==p.getId()){
		// System.err.println("0-th
		// unit:"+this._dm.getAllUnits().get(0)+"\t"+this._dm.getAllUnits().get(0).getId());
		// System.err.println("1-st
		// unit:"+this._dm.getAllUnits().get(1)+"\t"+this._dm.getAllUnits().get(1).getId());
		// System.err.println("curr unit:"+node.getUnit().getId());
		// throw new
		// RuntimeException("ah??"+"\t"+node.getUnit()+"\t"+node.arity());
		// }
		return NetworkIDMapper.toHybridNodeID(new int[] { eIndex, eIndex - bIndex, node.getHIndex(), node.getWIndex(),
				p.getId(), Network.NODE_TYPE.max.ordinal() });
	}

	private String getWord(Sentence sent, int bIndex, int eIndex) {
		StringBuilder sb = new StringBuilder();
		for (int i = bIndex; i < eIndex; i++) {
			sb.append(sent.get(i).getName() + "_" + i + " ");
		}
		String alignment = sb.toString();
		return alignment;
	}

	private ArrayList<WordToken> getWordTokens(Sentence sent, int bIndex, int eIndex) {
		ArrayList<WordToken> tokens = new ArrayList<>();
		for (int i = bIndex; i < eIndex; i++) {
			tokens.add(sent.get(i));
		}
		return tokens;
	}

	private String getNumberValue(Sentence sent, int bIndex, int eIndex) {
		StringBuilder sb = new StringBuilder();
		for (int i = bIndex; i < eIndex; i++) {
			WordToken wordToken = sent.get(i);
			String word = wordToken.isNumber() ? wordToken.getNumberVal() + "" : wordToken.getName();
			sb.append(word + " ");
		}
		String alignment = sb.toString().trim();
		return alignment;
	}

	private double getNumber(Sentence sent, int bIndex, int eIndex) {
		for (int i = bIndex; i < eIndex; i++) {
			WordToken wordToken = sent.get(i);
			// System.out.println(wordToken + ": " + bIndex);
			double word = wordToken.isGoldNumber() ? wordToken.getNumberVal() : -1.0;
			if (word != -1)
				return word;
		}

		return -1;
	}
}
