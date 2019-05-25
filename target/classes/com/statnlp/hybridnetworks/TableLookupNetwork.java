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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;

/**
 * An extension of {@link Network} which defines more functions related to managing nodes and edges.<br>
 * Subclasses might want to override {@link #isRemoved(int)} and {@link #remove(int)} to disable the auto-removal 
 * of nodes performed in this class.<br>
 * The main functions of this class are {@link #addNode(long)}, {@link #addEdge(long, long[])}, and {@link #finalizeNetwork()}.<br>
 * Always call {@link #finalizeNetwork()} after no more nodes and edges are going to be added 
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class TableLookupNetwork extends Network{
	
	private static final long serialVersionUID = -7250820762892368213L;
	
	//temporary data structures used when constructing the network
//	private transient HashSet<Long> _nodes_tmp;
	private transient HashMap<Long, ArrayList<long[]>> _children_tmp;
	
	//at each index, store the node's ID
	protected long[] _nodes;
	//at each index, store the node's list of children's indices (with respect to _nodes)
	protected int[][][] _children;
	//will be useful when doing decoding.
	protected boolean[] _isSumNode;
	
	public void setSumNode(long node){
		int node_k = Arrays.binarySearch(this._nodes, node);
		if(node_k<0){
			throw new RuntimeException("This node does not exist:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
		}
		this._isSumNode[node_k] = true;
	}
	
	private long[] toNodes(int[] ks){
		long[] nodes = new long[ks.length];
		for(int i = 0; i<nodes.length; i++){
			nodes[i] = this.getNode(ks[i]);
		}
		return nodes;
	}
	
	public ArrayList<long[]> getChildren_tmp(long node){
		return this._children_tmp.get(node);
	}
	
	public long[] getNodes_tmp(){
		Iterator<Long> nodes_key = this._children_tmp.keySet().iterator();
		long[] nodes = new long[this._children_tmp.size()];
		for(int k = 0; k<nodes.length; k++)
			nodes[k] = nodes_key.next();
		return nodes;
	}
	
	public boolean remove_tmp(long node){
		if(!this._children_tmp.containsKey(node))
			return false;
		this._children_tmp.remove(node);
		return true;
	}
	
	/**
	 * A convenience method to check whether a network is contained in (is a subgraph of) another network 
	 * @param network
	 * @return
	 */
	public boolean contains(TableLookupNetwork network){
//		if (true)
//		return true;
		
		if(this.countNodes() < network.countNodes()){
			System.err.println("size of this is less than the size of network."+this.countNodes()+"\t"+network.countNodes());
			return false;
		}
		int start = 0;
		for(int j = 0;j<network.countNodes(); j++){
			long node1 = network.getNode(j);
			int[][] children1 = network.getChildren(j);
			boolean found = false;
			for(int k = start; k<this.countNodes() ; k++){
				long node2 = this.getNode(k);
				int[][] children2 = this.getChildren(k);
				if(node1==node2){
					
					for(int[] child1 : children1){
						long[] child1_nodes = network.toNodes(child1);
						boolean child_found = false;
						for(int[] child2 : children2){
							long[] child2_nodes = this.toNodes(child2);
							if(Arrays.equals(child1_nodes, child2_nodes)){
								child_found = true;
							}
						}
						if(!child_found){
							System.err.println("supposingly smaller:"+Arrays.toString(child1_nodes)+"\t"+children1.length);
							for(int t = 0; t<children2.length; t++){
								System.err.println("supposingly larger :"+Arrays.toString(this.toNodes(children2[t]))+"\t"+children2.length);
							}
							System.err.println(node1+"\t"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node1)));
							System.err.println(node2+"\t"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node2)));
							throw new RuntimeException("does not contain!");
//							return false;
						}
					}
					
					found = true;
					start = k;
					break;
				}
			}
			if(!found){
				System.err.println("NOT FOUND:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node1)));
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Default constructor. Note that the network constructed using this default constructor is lacking 
	 * the {@link LocalNetworkParam} object required for actual use.
	 * Use this only for generating generic network, which is later actualized using another constructor.
	 * @see #TableLookupNetwork(int, Instance, LocalNetworkParam)
	 */
	public TableLookupNetwork(){
//		this._nodes_tmp = new HashSet<Long>();
		this._children_tmp = new HashMap<Long, ArrayList<long[]>>();
	}

	/**
	 * Construct a network with the specified instance and parameter
	 * @param networkId
	 * @param inst
	 * @param param
	 */
	public TableLookupNetwork(int networkId, Instance inst, LocalNetworkParam param){
		this(networkId, inst, param, null);
	}
	
	/**
	 * Construct a network with the specified instance and parameter, and with the compiler that created this network
	 * @param networkId
	 * @param inst
	 * @param param
	 * @param compiler
	 */
	public TableLookupNetwork(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler){
		super(networkId, inst, param, compiler);
//		this._nodes_tmp = new HashSet<Long>();
		this._children_tmp = new HashMap<Long, ArrayList<long[]>>();
	}

	/**
	 * Construct a network with the specified nodes and edges<br>
	 * This is mainly used to create a subgraph of a larger graph by modifying the number of nodes
	 * by overriding {@link #countNodes()}
	 * @param networkId
	 * @param inst
	 * @param nodes
	 * @param children
	 * @param param
	 */
	public TableLookupNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param){
		this(networkId, inst, nodes, children, param, null);
	}
	
	/**
	 * Construct a network with the specified nodes and edges, and with the compiler that created this network<br>
	 * This is mainly used to create a subgraph of a larger graph by modifying the number of nodes
	 * by overriding {@link #countNodes()}
	 * @param networkId
	 * @param inst
	 * @param nodes
	 * @param children
	 * @param param
	 * @param compiler
	 */
	public TableLookupNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param, NetworkCompiler compiler){
		super(networkId, inst, param, compiler);
		this._nodes = nodes;
		this._children = children;
	}
	
	@Override
	public long getNode(int k){
		return this._nodes[k];
	}
	
	@Override
	public int[][] getChildren(int k){
		return this._children[k];
	}
	
	public int countTmpNodes_tmp(){
		return this._children_tmp.size();
	}
	
	public long[] getAllNodes(){
		return this._nodes;
	}
	
	public int[][][] getAllChildren(){
		return this._children;
	}
	
	@Override
	public int countNodes() {
		return this._nodes.length;
	}
	
	/**
	 * Remove the node k from the network.
	 */
	public void remove(int k){
		this._nodes[k] = -1;
		if (this._inside!=null){
			this._inside[k] = Double.NEGATIVE_INFINITY;
		}
		if (this._outside!=null){
			this._outside[k] = Double.NEGATIVE_INFINITY;
		}
	}
	
	/**
	 * Check if the node k is removed from the network.
	 */
	public boolean isRemoved(int k){
		return this._nodes[k] == -1;
	}
	
	/**
	 * Check if the node is present in this network.
	 */
	public boolean contains(long node){
		return this._children_tmp.containsKey(node);
	}
	
	public int getNodeIndex(long node){
		return Arrays.binarySearch(this._nodes, node);
	}
	
	/**
	 * Add one node to the network.
	 * @param node The node to be added
	 * @return
	 */
	public boolean addNode(long node){
//		if(node==901360616258948L){
//			throw new RuntimeException("s");
//		}
		if(this._children_tmp.containsKey(node))
			return false;
//			throw new NetworkException("The node is already added:"+node);
		this._children_tmp.put(node, null);
		return true;
	}
	
	public int numNodes_tmp(){
		return this._children_tmp.size();
	}
	
//	public ArrayList<Long> getAllChildren(long node){
//		ArrayList<Long> nodes = new ArrayList<Long>();
//		HashMap<Long, ArrayList<Long>> node2allchildren = new HashMap<Long, ArrayList<Long>>();
//		this.getAllChildrenHelper(node, nodes, node2allchildren);
//		return nodes;
//	}
//	
//	private ArrayList<Long> getAllChildrenHelper(long node, HashMap<Long, ArrayList<Long>> node2allchildren){
//		if(node2allchildren.containsKey(node)){
//			return node2allchildren.get(node);
//		}
//		ArrayList<Long> allchildren = new ArrayList<Long>();
//		ArrayList<long[]> children = this.getChildren_tmp(node);
//		for(long[] child : children){
//			for(long c : child){
//				
//			}
//		}
//	}
	
	/**
	 * Remove all such nodes that is not a descendent of the root<br>
	 * This is a useful method to reduce the number of nodes and edges during network creation.
	 */
	public void checkValidNodesAndRemoveUnused(){
		long[] nodes = new long[this.countTmpNodes_tmp()];
		double[] validity = new double[this.countTmpNodes_tmp()];
		int v = 0;
		Iterator<Long> nodes_it = this._children_tmp.keySet().iterator();
		while(nodes_it.hasNext()){
			nodes[v++] = nodes_it.next();
		}
		Arrays.sort(nodes);
		this.checkValidityHelper(validity, nodes, this.countTmpNodes_tmp()-1);
		for(int k = 0; k<validity.length; k++){
			if(validity[k]==0){
				this.remove_tmp(nodes[k]);
			}
		}
	}
	
	private void checkValidityHelper(double[] validity, long[] nodes, int node_k){
		if(validity[node_k]==1){
			return;
		}
		
		validity[node_k] = 1;
		ArrayList<long[]> children = this.getChildren_tmp(nodes[node_k]);
		if(children==null){
			return;
		}
		for(long[] child : children){
			for(long c : child){
				int c_k = Arrays.binarySearch(nodes, c);
				if(c_k<0)
					throw new RuntimeException("Can not find this node? Position:"+c_k+",value:"+c);
				this.checkValidityHelper(validity, nodes, c_k);
			}
		}
	}
	
	/**
	 * Finalize this network, by converting the temporary arrays for nodes and edges into the finalized one.<br>
	 * This method must be called before this network can be used.
	 */
	public void finalizeNetwork(){
//		System.err.println(this._nodes_tmp.size()+"<<<");
		Iterator<Long> node_ids = this._children_tmp.keySet().iterator();
		ArrayList<Long> values = new ArrayList<Long>();
		while(node_ids.hasNext()){
			values.add(node_ids.next());
		}
		this._nodes = new long[this._children_tmp.keySet().size()];
		HashMap<Long, Integer> nodesValue2IdMap = new HashMap<Long, Integer>();
		Collections.sort(values);
		for(int k = 0 ; k<values.size(); k++){
			this._nodes[k] = values.get(k);
			nodesValue2IdMap.put(this._nodes[k], k);
		}
		
//		this._nodes_tmp = null;
		this._children = new int[this._nodes.length][][];
		
		Iterator<Long> parents = this._children_tmp.keySet().iterator();
		while(parents.hasNext()){
			long parent = parents.next();
			int parent_index = nodesValue2IdMap.get(parent);
			ArrayList<long[]> childrens = this._children_tmp.get(parent);
			if(childrens==null){
				this._children[parent_index] = new int[1][0];
			} else {
				this._children[parent_index] = new int[childrens.size()][];
				for(int k = 0 ; k <this._children[parent_index].length; k++){
					long[] children = childrens.get(k);
					int[] children_index = new int[children.length];
					for(int m = 0; m<children.length; m++){
						children_index[m] = nodesValue2IdMap.get(children[m]);
					}
					this._children[parent_index][k] = children_index;
				}
			}
		}
		for(int k = 0 ; k<this._children.length; k++){
			if(this._children[k]==null){
				this._children[k] = new int[1][0];
			}
		}
		this._children_tmp = null;
	}
	
	private void checkLinkValidity(long parent, long[] children){
		/**/
		for(long child : children){
			if(child >= parent){
				System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(parent)));
				System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(children[0])));
				System.err.println();
				throw new NetworkException("This link seems to be invalid:"+parent+"\t"+Arrays.toString(children));
			}
		}
		/**/
		
		this.checkNodeValidity(parent);
		for(long child : children){
			this.checkNodeValidity(child);
		}
	}
	
	private void checkNodeValidity(long node){
		if(!this._children_tmp.containsKey(node)){
			throw new NetworkException("This node seems to be invalid:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
		}
	}
	
	/**
	 * Add an edge to this network. Only do this after the respective nodes are added.
	 * @param parent The parent node
	 * @param children The child nodes of a SINGLE hyperedge. Note that this only add one edge, 
	 * 				   with the parent as the root and the children as the leaves in the hyperedge.
	 * 				   To add multiple edges, multiple calls to this method is necessary.
	 * @throws NetworkException If the edge is already added
	 */
	public void addEdge(long parent, long[] children){
		this.checkLinkValidity(parent, children);
		if(!this._children_tmp.containsKey(parent) || this._children_tmp.get(parent)==null){
			this._children_tmp.put(parent, new ArrayList<long[]>());
		}
		ArrayList<long[]> existing_children = this._children_tmp.get(parent);
		for(int k = 0; k<existing_children.size(); k++){
			if(Arrays.equals(existing_children.get(k), children)){
				throw new NetworkException("This children is already added. Add again???");
			}
		}
		existing_children.add(children);
	}
	
	@Override
	public boolean isRoot(int k){
		return this.countNodes()-1 == k;
	}
	
	@Override
	public boolean isLeaf(int k){
		int[][] v= this._children[k];
		if(v.length==0) return false;
		if(v[0].length==0) return true;
		return false;
	}
	
	/**
	 * Count the number of invalid nodes
	 * @return
	 */
	public int countInValidNodes(){
		int count = 0;
		for(int k = 0; k<this._nodes.length; k++){
			if(this._inside[k]==Double.NEGATIVE_INFINITY || this._outside[k]==Double.NEGATIVE_INFINITY){
				count++;
			}
		}
		return count;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("nodes:");
		sb.append('[');
		sb.append('\n');
		for(int k = 0; k<this.countNodes(); k++){
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[k])));
			sb.append('\n');
		}
		sb.append(']');
		sb.append('\n');
		sb.append("links:");
		sb.append('[');
		sb.append('\n');
		for(int k = 0; k<this.countNodes(); k++){
			sb.append('<');
			long parent = this._nodes[k];
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(parent)));
			int[][] childrenList = this._children[k];
			for(int i = 0; i<childrenList.length; i++){
				sb.append('\n');
				sb.append('\t');
				sb.append('(');
				int[] children = childrenList[i];
				for(int j = 0; j<children.length; j++){
					sb.append('\n');
					sb.append('\t'+Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[children[j]])));
				}
				sb.append('\n');
				sb.append('\t');
				sb.append(')');
			}
			sb.append('>');
			sb.append('\n');
		}
		sb.append(']');
		sb.append('\n');
		
		return sb.toString();
	}
	
}
