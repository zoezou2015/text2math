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
/**
 * 
 */
package com.statnlp.hybridnetworks;

import java.io.Serializable;

/**
 * An interface defining a hypergraph
 * @author Wei Lu <luwei@statnlp.com>
 */
public interface HyperGraph extends Serializable{
	
	/**
	 * Count the total number of nodes.
	 * @return
	 */
	public int countNodes();
	
	/**
	 * Get the node with index k.
	 * @param k
	 * @return
	 */
	public long getNode(int k);
	
	/**
	 * Get the array of nodes in this network
	 * @param k
	 * @return
	 */
	public int[] getNodeArray(int k);
	
	/**
	 * Get the children for node with index k.
	 * @param k
	 * @return
	 */
	public int[][] getChildren(int k);
	
	//check whether the node with index k is removed.
	public boolean isRemoved(int k);
	
	/**
	 * Remove the node with index k.
	 * @param k
	 */
	public void remove(int k);
	
	/**
	 * Check whether the node with index k is the root of the network.
	 * @param k
	 * @return
	 */
	public boolean isRoot(int k);
	
	/**
	 * Check whether the node with index k is a leaf of the network.
	 * @param k
	 * @return
	 */
	public boolean isLeaf(int k);
	
	/**
	 * Check if the network contains a particular node.
	 * @param node
	 * @return
	 */
	public boolean contains(long node);
	
	/**
	 * Return the node index in this network of the given node
	 * @param node
	 * @return
	 */
	public int getNodeIndex(long node);
	
}
