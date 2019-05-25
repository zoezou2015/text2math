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
import java.util.concurrent.ConcurrentHashMap;

import com.statnlp.commons.types.Instance;

/**
 * The base class for network compiler, a class to convert a problem representation between 
 * {@link Instance} (the surface form) and {@link Network} (the modeled form)<br>
 * When implementing the {@link #compile(int, Instance, LocalNetworkParam)} method, you might 
 * want to split the case into two cases: labeled and unlabeled, where the labeled network contains
 * only the existing nodes and edges in the instance, and the unlabeled network contains all
 * possible nodes and edges in the instance.
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class NetworkCompiler implements Serializable{
	
	/**
	 * A class to store information about a single instance (both labeled and unlabeled versions)
	 * @author Aldrian Obaja <aldrianobaja.m@gmail.com>
	 *
	 */
	public static class InstanceInfo implements Serializable {
		private static final long serialVersionUID = 8576388720516676443L;
		public int instanceId;
		public Network labeledNetwork;
		public Network unlabeledNetwork;
		
		public double score;
		
		public InstanceInfo(int instanceID){
			if(instanceID <= 0){
				throw new RuntimeException("InstanceInfo objects should have positive ID, received ID: "+instanceID);
			}
			this.instanceId = instanceID;
		}
	}
	
	private static final long serialVersionUID = 1052885626598299680L;
	public final ConcurrentHashMap<Integer, InstanceInfo> instanceInfos = new ConcurrentHashMap<Integer, InstanceInfo>();
	
	/**
	 * Compile and store the networks per instance basis (each instance has two networks: labeled and unlabeled)
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 */
	public Network compileAndStore(int networkId, Instance inst, LocalNetworkParam param){
		Network network = compile(networkId, inst, param);
		int absInstID = Math.abs(inst.getInstanceId());
		InstanceInfo info = instanceInfos.putIfAbsent(absInstID, new InstanceInfo(absInstID));
		if(info == null){ // This means previously there is no InstanceInfo
			info = instanceInfos.get(absInstID);
		}
		if(inst.isLabeled()){
			info.labeledNetwork = network;
			if(info.unlabeledNetwork != null){
				info.unlabeledNetwork.setLabeledNetwork(network);
				network.setUnlabeledNetwork(info.unlabeledNetwork);
			}
		} else {
			info.unlabeledNetwork = network;
			if(info.labeledNetwork != null){
				info.labeledNetwork.setUnlabeledNetwork(network);
				network.setLabeledNetwork(info.labeledNetwork);
			}
		}
		return network;
	}
	
	/**
	 * Convert an instance into the network representation.<br>
	 * This process is also called the encoding part (e.g., to create the trellis network 
	 * of POS tags for a given sentence)<br>
	 * Subclasses might want to split this method into two, one for labeled instance, and 
	 * another for unlabeled instance.
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 */
	public abstract Network compile(int networkId, Instance inst, LocalNetworkParam param);
	
	/**
	 * Convert a network into an instance, the surface form.<br>
	 * This process is also called the decoding part (e.g., to get the sequence with maximum 
	 * probability in an HMM)
	 * @param network
	 * @return
	 */
	public abstract Instance decompile(Network network);

	
	/**
	 * The cost of the structure from leaf nodes up to node <code>k</code>.<br>
	 * This is used for structured SVM, and generally the implementation requires the labeled Instance.<br>
	 * Cost is not calculated during test, since there is no labeled instance.<br>
	 * This will call {@link #costAt(int, int[])}, where the actual implementation resides.
	 * @param k
	 * @param child_k
	 * @return
	 */
	public double cost(Network network, int k, int[] child_k){
		if(network.getInstance().getInstanceId() > 0){
			return 0.0;
		}
		return costAt(network, k, child_k);
	}

	/**
	 * The cost of the structure at the edge connecting node <code>k</code> with its specific
	 * child node <code>child_k</code>.<br>
	 * This is used for structured SVM, and generally the implementation requires the labeled Instance, which
	 * can be accessed through {@link Network#getLabeledNetwork}.<br>
	 * @param network
	 * @param parent_k
	 * @param child_k
	 * @return
	 */	
	public double costAt(Network network, int parent_k, int[] child_k){
		int size = network.getInstance().size();
		Network labeledNet = network.getLabeledNetwork();
		long node = network.getNode(parent_k);
		int node_k = labeledNet.getNodeIndex(node);
		if(node_k < 0){
			double nodeCost = NetworkConfig.NODE_COST;
			if(NetworkConfig.NORMALIZE_COST){
				nodeCost /= size;
			}
			nodeCost *= NetworkConfig.MARGIN;
			double edgeCost = NetworkConfig.EDGE_COST;
			if(NetworkConfig.NORMALIZE_COST){
				edgeCost /= size;
			}
			edgeCost *= NetworkConfig.MARGIN;
			return nodeCost+edgeCost;
		}
		long[] childNodes = new long[child_k.length];
		for(int i=0; i<child_k.length; i++){
			childNodes[i] = network.getNode(child_k[i]);
		}
		int[][] children_k = labeledNet.getChildren(node_k);
		boolean edgePresentInLabeled = false;
		for(int[] children: children_k){
			long[] childrenNodes = new long[children.length];
			for(int i=0; i<children.length; i++){
				childrenNodes[i] = labeledNet.getNode(children[i]);
			}
			if(Arrays.equals(childrenNodes, childNodes)){
				edgePresentInLabeled = true;
				break;
			}
		}
		if(edgePresentInLabeled || network.isRoot(parent_k)){
			return 0.0;
		} else {
			double edgeCost = NetworkConfig.EDGE_COST;
			if(NetworkConfig.NORMALIZE_COST){
				edgeCost /= size;
			}
			edgeCost *= NetworkConfig.MARGIN;
			return edgeCost;
		}
	}
	
}