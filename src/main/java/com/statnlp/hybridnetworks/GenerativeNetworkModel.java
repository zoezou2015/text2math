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

import com.statnlp.commons.types.Instance;

public class GenerativeNetworkModel extends NetworkModel {
	
	private static final long serialVersionUID = -3918700498473432574L;

	public static GenerativeNetworkModel create(FeatureManager fm, NetworkCompiler compiler){
		return new GenerativeNetworkModel(fm, compiler);
	}
	
	public GenerativeNetworkModel(FeatureManager fm, NetworkCompiler compiler){
		super(fm, compiler);
	}
	
	@Override
	protected Instance[][] splitInstancesForTrain() {
		
		System.err.println("#instances="+this._allInstances.length);
		
		Instance[][] insts = new Instance[this._numThreads][];

		ArrayList<ArrayList<Instance>> insts_list = new ArrayList<ArrayList<Instance>>();
		int threadId;
		for(threadId = 0; threadId<this._numThreads; threadId++){
			insts_list.add(new ArrayList<Instance>());
		}
		
		threadId = 0;
		for(int k = 0; k<this._allInstances.length; k++){
			Instance inst = this._allInstances[k];
			insts_list.get(threadId).add(inst);
			threadId = (threadId+1)%this._numThreads;
		}
		
		for(threadId = 0; threadId<this._numThreads; threadId++){
			int size = insts_list.get(threadId).size();
			insts[threadId] = new Instance[size];
			for(int i = 0; i < size; i++){
				Instance inst = insts_list.get(threadId).get(i);
				insts[threadId][i] = inst;
			}
			System.out.println("Thread "+threadId+" has "+insts[threadId].length+" instances.");
		}
		
		return insts;
	}
	
}