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
package com.statnlp.example.sp.simple_linear;

import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.example.sp.HybridGrammar;
import com.statnlp.example.sp.SemTextDataManager;
import com.statnlp.example.sp.SemTextInstance;
import com.statnlp.example.sp.SemTextNetwork;
import com.statnlp.example.sp.SemanticForest;
import com.statnlp.example.sp.SemanticForestNode;
import com.statnlp.example.sp.SemanticUnit;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;

/**
 * @author wei_lu
 *
 */
public class SimpleLinearSPNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -2627518568805384558L;

	private HybridGrammar _g;
	private SemTextDataManager _dm;
	private SemanticForest _forest;// the global forest.

	protected static enum NODE_TYPE {
		UNIT_NODE, ROOT
	}

	public static enum TYPE {
		PATTERN_X, PATTERN_Y, WITH_ALIGN, WITHOUT_ALIGN
	};

	public SimpleLinearSPNetworkCompiler(HybridGrammar g, SemanticForest forest, SemTextDataManager dm) {

		this._g = g;
		this._dm = dm;
		this._forest = forest;

		int[] capacity = new int[] { 1000, 1000, 4, 300, 10 };
		NetworkIDMapper.setCapacity(capacity);

	}

	@Override
	public SemTextNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {

		SemTextInstance stInst = (SemTextInstance) inst;
		if (inst.isLabeled()) {
			return this.compile_labeled(networkId, stInst, param);
		} else {
			return this.compile_unlabeled(networkId, stInst, param);
		}

	}

	private SemTextNetwork compile_labeled(int networkId, SemTextInstance inst, LocalNetworkParam param) {

		SemTextNetwork network = new SemTextNetwork(networkId, inst, param);
		Sentence sent = inst.getInput();
		SemanticForest tree = inst.getOutput();
		return this.compile(network, sent, tree);

	}

	private SemTextNetwork compile(SemTextNetwork network, Sentence sent, SemanticForest tree) {

		System.err.print('+');
		for (int eIndex = 1; eIndex <= sent.length(); eIndex++) {
			for (SemanticForestNode forestNode : tree.getAllNodes()) {

				if (forestNode.isRoot())
					continue;
				long node_withoutAlign = this.toNode_withoutAlign(forestNode);
				if (!network.contains(node_withoutAlign)) {
					network.addNode(node_withoutAlign);
				}

				if (forestNode.arity() == 0) {
					long node_withAlign = this.toNode_withAlign(forestNode, eIndex);
					network.addNode(node_withAlign);
					network.addEdge(node_withoutAlign, new long[] { node_withAlign });
				} else if (forestNode.arity() == 1) {
					long node_withAlign_A = this.toNode_withAlign_A(forestNode, eIndex);
					network.addNode(node_withAlign_A);
					network.addEdge(node_withoutAlign, new long[] { node_withAlign_A });

					SemanticForestNode[] children0 = forestNode.getChildren()[0];
					for (SemanticForestNode child0 : children0) {
						long node_child0 = this.toNode_withoutAlign(child0);
						if (network.contains(node_child0)) {
							network.addEdge(node_withAlign_A, new long[] { node_child0 });
						} else {
							throw new RuntimeException("node should be included!");
						}
					}
				} else if (forestNode.arity() == 2) {
					long node_withAlign_A = this.toNode_withAlign_A(forestNode, eIndex);
					network.addNode(node_withAlign_A);
					long node_withAlign_B = this.toNode_withAlign_B(forestNode, eIndex);
					network.addNode(node_withAlign_B);
					network.addEdge(node_withoutAlign, new long[] { node_withAlign_A, node_withAlign_B });

					SemanticForestNode[] children0 = forestNode.getChildren()[0];
					for (SemanticForestNode child0 : children0) {
						long node_child0 = this.toNode_withoutAlign(child0);
						if (network.contains(node_child0)) {
							network.addEdge(node_withAlign_A, new long[] { node_child0 });
						} else {
							throw new RuntimeException("node should be included!");
						}
					}

					SemanticForestNode[] children1 = forestNode.getChildren()[1];
					for (SemanticForestNode child1 : children1) {
						long node_child1 = this.toNode_withoutAlign(child1);
						if (network.contains(node_child1)) {
							network.addEdge(node_withAlign_B, new long[] { node_child1 });
						} else {
							throw new RuntimeException("node should be included!");
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
			long preroot = this.toNode_withoutAlign(child_of_root[k]);
			network.addEdge(root, new long[] { preroot });
		}

		network.finalizeNetwork();

		return network;
	}

	private SemTextNetwork compile_unlabeled(int networkId, SemTextInstance inst, LocalNetworkParam param) {
		SemTextNetwork network = new SemTextNetwork(networkId, inst, param);
		Sentence sent = inst.getInput();
		SemanticForest tree = inst.getOutput();

		return this.compile_unlabeled_helper(network, sent, tree);
	}

	private synchronized SemTextNetwork compile_unlabeled_helper(SemTextNetwork network, Sentence sent,
			SemanticForest tree) {
		System.err.print('+');
		for (int eIndex = 1; eIndex <= sent.length(); eIndex++) {
			for (SemanticForestNode forestNode : this._forest.getAllNodes()) {

				if (forestNode.isRoot())
					continue;
				long node_withoutAlign = this.toNode_withoutAlign(forestNode);
				if (!network.contains(node_withoutAlign)) {
					network.addNode(node_withoutAlign);
				}

				if (forestNode.arity() == 0) {
					long node_withAlign = this.toNode_withAlign(forestNode, eIndex);
					network.addNode(node_withAlign);
					network.addEdge(node_withoutAlign, new long[] { node_withAlign });
				} else if (forestNode.arity() == 1) {
					long node_withAlign_A = this.toNode_withAlign_A(forestNode, eIndex);
					network.addNode(node_withAlign_A);
					network.addEdge(node_withoutAlign, new long[] { node_withAlign_A });

					SemanticForestNode[] children0 = forestNode.getChildren()[0];
					for (SemanticForestNode child0 : children0) {
						long node_child0 = this.toNode_withoutAlign(child0);
						if (network.contains(node_child0)) {
							network.addEdge(node_withAlign_A, new long[] { node_child0 });
						} else {
							throw new RuntimeException("node should be included!");
						}
					}
				} else if (forestNode.arity() == 2) {
					long node_withAlign_A = this.toNode_withAlign_A(forestNode, eIndex);
					network.addNode(node_withAlign_A);
					long node_withAlign_B = this.toNode_withAlign_B(forestNode, eIndex);
					network.addNode(node_withAlign_B);
					network.addEdge(node_withoutAlign, new long[] { node_withAlign_A, node_withAlign_B });

					SemanticForestNode[] children0 = forestNode.getChildren()[0];
					for (SemanticForestNode child0 : children0) {
						long node_child0 = this.toNode_withoutAlign(child0);
						if (network.contains(node_child0)) {
							network.addEdge(node_withAlign_A, new long[] { node_child0 });
						} else {
							throw new RuntimeException("node should be included!");
						}
					}

					SemanticForestNode[] children1 = forestNode.getChildren()[1];
					for (SemanticForestNode child1 : children1) {
						long node_child1 = this.toNode_withoutAlign(child1);
						if (network.contains(node_child1)) {
							network.addEdge(node_withAlign_B, new long[] { node_child1 });
						} else {
							throw new RuntimeException("node should be included!");
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
			long preroot = this.toNode_withoutAlign(child_of_root[k]);
			network.addEdge(root, new long[] { preroot });
		}

		network.finalizeNetwork();

		return network;
	}

	@Override
	public SemTextInstance decompile(Network network) {

		SemTextNetwork stNetwork = (SemTextNetwork) network;

		SemTextInstance inst = (SemTextInstance) stNetwork.getInstance();
		inst = inst.duplicate();

		// if the value is -inf, it means there is no prediction.
		if (stNetwork.getMax() == Double.NEGATIVE_INFINITY) {
			return inst;
		}

		SemanticForest forest = this.toTree(stNetwork);
		inst.setPrediction(forest);

		return inst;

	}

	private SemanticForest toTree(SemTextNetwork network) {

		SemanticForestNode root = SemanticForestNode.createRootNode(NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH);
		this.toTree_helper(network, network.countNodes() - 1, root);
		return new SemanticForest(root);

	}

	private void toTree_helper(SemTextNetwork network, int node_k, SemanticForestNode currNode) {

		long node = network.getNode(node_k);
		int[] ids_node = NetworkIDMapper.toHybridNodeArray(node);
		// System.err.println(">>>"+Arrays.toString(ids_node)+"<<<"+"\t"+node+"\t"+network.getRoot());
		// System.exit(1);
		int[] children_k = network.getMaxPath(node_k);
		double score = network.getMax(node_k);
		if (currNode.getScore() == Double.NEGATIVE_INFINITY) {
			currNode.setScore(score);
			currNode.setInfo("info:" + Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
		}

		for (int child_k : children_k) {
			long child = network.getNode(child_k);
			int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);

			if (node_k == network.countNodes() - 1) {
				SemanticUnit unit = this._dm.getAllUnits().get(ids_child[1]);
				SemanticForestNode childNode = new SemanticForestNode(unit, currNode.getHIndex() - 1);

				// System.err.println("1:"+currNode.getUnit()+"\t"+currNode.getUnit().arity());
				currNode.setChildren(0, new SemanticForestNode[] { childNode });

				this.toTree_helper(network, child_k, childNode);
			} else if (TYPE.PATTERN_X.ordinal() == ids_node[2]) {

				// if(currNode.arity()==0){
				// throw new
				// RuntimeException("joke?"+"\t"+currNode.getUnit()+"\t"+Arrays.toString(ids_node));
				// }

				SemanticUnit unit = this._dm.getAllUnits().get(ids_child[1]);
				SemanticForestNode childNode = new SemanticForestNode(unit, currNode.getHIndex() - 1);

				// System.err.println("yy:"+currNode.getUnit()+"\t"+childNode.getUnit()+"\t"+currNode.arity());
				// System.err.println("2:"+currNode.getUnit()+"\t"+currNode.getUnit().arity());
				currNode.setChildren(0, new SemanticForestNode[] { childNode });

				this.toTree_helper(network, child_k, childNode);
			} else if (TYPE.PATTERN_Y.ordinal() == ids_node[2]) {
				SemanticUnit unit = this._dm.getAllUnits().get(ids_child[1]);
				SemanticForestNode childNode = new SemanticForestNode(unit, currNode.getHIndex() - 1);

				// System.err.println("3:"+currNode.getUnit()+"\t"+currNode.getUnit().arity());
				currNode.setChildren(1, new SemanticForestNode[] { childNode });

				this.toTree_helper(network, child_k, childNode);
			} else {
				this.toTree_helper(network, child_k, currNode);
			}
		}

	}

	private long toNode_root(int sent_len) {

		return NetworkIDMapper.toHybridNodeID(
				new int[] { NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH + 1, 0, 0, 0, NODE_TYPE.ROOT.ordinal() });
	}

	private long toNode_withoutAlign(SemanticForestNode node) {
		return NetworkIDMapper.toHybridNodeID(new int[] { node.getHIndex(), node.getWIndex(),
				TYPE.WITHOUT_ALIGN.ordinal(), 0, NODE_TYPE.UNIT_NODE.ordinal() });

	}

	private long toNode_withAlign(SemanticForestNode node, int bIndex) {
		return NetworkIDMapper.toHybridNodeID(new int[] { node.getHIndex(), node.getWIndex(), TYPE.WITH_ALIGN.ordinal(),
				bIndex, NODE_TYPE.UNIT_NODE.ordinal() });

	}

	private long toNode_withAlign_A(SemanticForestNode node, int bIndex) {
		return NetworkIDMapper.toHybridNodeID(new int[] { node.getHIndex(), node.getWIndex(), TYPE.PATTERN_X.ordinal(),
				bIndex, NODE_TYPE.UNIT_NODE.ordinal() });
	}

	private long toNode_withAlign_B(SemanticForestNode node, int bIndex) {
		return NetworkIDMapper.toHybridNodeID(new int[] { node.getHIndex(), node.getWIndex(), TYPE.PATTERN_Y.ordinal(),
				bIndex, NODE_TYPE.UNIT_NODE.ordinal() });
	}

}