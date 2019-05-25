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

import java.util.HashSet;
import java.util.concurrent.Callable;

import com.statnlp.commons.types.Instance;

public class LocalNetworkLearnerThread extends Thread implements Callable<Void> {
	
	/** The ID of the thread the lowest ID should be 0. */
	private int _threadId = -1;
	
	/** Whether this thread is performing touch */
	private boolean isTouching ;
	
	/** The maximum number of nodes in the network. */
	private int _networkCapacity = 1000000;
	/** The local feature map. */
	private LocalNetworkParam _param;
	
	/** A flag indicating whether we cache the networks. */
	private boolean _cacheNetworks = true;
	/** The networks. */
	private Network[] _networks;
	
	private Instance[] _instances;
	private NetworkCompiler _builder;
	/** The current iteration number */
	private int _it;
	
	/** Prepare the list of instance ids for the batch selection */
	private HashSet<Integer> chargeInstsIds = null;
	
	private HashSet<Integer> trainInstsIds = null;
	
	/**
	 * Construct a new learner thread using current networks (if cached) or builder (if not cached),
	 * also advancing the iteration number by 1.
	 * @return
	 */
	public LocalNetworkLearnerThread copyThread(){
		if(this._cacheNetworks){
			return new LocalNetworkLearnerThread(this._threadId, this._param, this._instances, this._networks, this._it+1);
		} else {
			return new LocalNetworkLearnerThread(this._threadId, this._param, this._instances, this._builder, this._it+1);
		}
	}
	
	private LocalNetworkLearnerThread(int threadId, LocalNetworkParam param, Instance[] instances, NetworkCompiler builder, int it){
		this._threadId = threadId;
		this._param = param;
		this._instances = instances;
		this._builder = builder;
		this._it = it;
	}
	
	private LocalNetworkLearnerThread(int threadId, LocalNetworkParam param, Instance[] instances, Network[] networks, int it){
		this._threadId = threadId;
		this._param = param;
		this._instances = instances;
		this._networks = networks;
		this._it = it;
	}
	
	/**
	 * Construct a learner thread
	 * Please make sure the threadId is 0-indexed.
	 * @param threadId Should start from 0
	 * @param fm The feature manager
	 * @param instances The instances
	 * @param builder The network compiler
	 * @param it Starting iteration numbe
	 */
	public LocalNetworkLearnerThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler builder, int it){
		this._threadId = threadId;
		this._param = new LocalNetworkParam(this._threadId, fm, instances.length);
		fm.setLocalNetworkParams(this._threadId, this._param);
		
		this._builder = builder;
		this._instances = instances;
		
		if(this._cacheNetworks)
			this._networks = new Network[this._instances.length];
		
		this._it = it;
	}
	
	public int getThreadId(){
		return this._threadId;
	}
	
    @Override
    public void run () {
    	if(!isTouching){
    		this.train(this._it);
    	} else {
    		this.touch();
    	}
    }
    
    public Void call(){
    	this.train(this._it);
    	return null;
    }
    
    /**
     * Go through all networks to know the possible features, 
     * and caching the networks if {@link #_cacheNetworks} is true.
     */
	public void touch(){
		long time = System.currentTimeMillis();
		//extract the features..
		for(int networkId = 0; networkId< this._instances.length; networkId++){
			if(networkId%100==0)
				System.err.print('.');
			if(NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY
					&& ((!this._param._isFinalized && this.getNetwork(networkId).getInstance().getInstanceId() < 0))){
				// When extracting features only for labeled, the first touch is only to extract features from labeled instances
				// The second touch, enabled only when caching is enabled, which is after the LocalNetworkParam being finalized,
				// is only for feature caching
				continue;
			}
			this.getNetwork(networkId).touch();
		}
		System.err.println();
		time = System.currentTimeMillis() - time;
		System.out.println("Thread "+this._threadId + " touch time: "+ time/1000.0+" secs.");
	}

	public void setTouch(){
		this.isTouching = true;
	}

	public void setUnTouch(){
		this.isTouching = false;
	}
	
	/**
	 * Do one iteration of training
	 * add the batch size here is we are using the batch gradient descent, 
	 * every time we shuffle the list.
	 * @param it
	 */
	private void train(int it){
		for(int i = 0; i< this._instances.length; i++){
			if(NetworkConfig.USE_BATCH_TRAINING && !this.chargeInstsIds.contains(this._instances[i].getInstanceId()) && !this.chargeInstsIds.contains(-this._instances[i].getInstanceId()) )
				continue;
			if(this.trainInstsIds != null && !this.trainInstsIds.contains(this._instances[i].getInstanceId()) && !this.trainInstsIds.contains(-this._instances[i].getInstanceId()))
				continue;
			Network network = this.getNetwork(i);
			network.train();
		}
	}
	
	public Network getNetwork(int networkId){
		if(this._cacheNetworks && this._networks[networkId]!=null)
			return this._networks[networkId];
		Network network = this._builder.compileAndStore(networkId, this._instances[networkId], this._param);
		if(this._cacheNetworks)
			this._networks[networkId] = network;
		if(network.countNodes() > this._networkCapacity) this._networkCapacity = network.countNodes();
		return network;
	}
	
	public int getNetworkCapacity(){
		return this._networkCapacity;
	}
	
	public LocalNetworkParam getLocalNetworkParam(){
		return this._param;
	}
	
	public int getIterationNumber(){
		return this._it;
	}

	public void setIterationNumber(int it) {
		this._it = it;
	}
	
	public void setInstanceIdSet(HashSet<Integer> set){
		this.chargeInstsIds = set;
	}
	
	public void setTrainInstanceIdSet(HashSet<Integer> set){
		this.trainInstsIds = set;
	}
	
}