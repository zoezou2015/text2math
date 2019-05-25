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

import com.statnlp.commons.types.Instance;

public class LocalNetworkDecoderThread extends Thread{
	
	//the id of the thread.
	private int _threadId = -1;
	//the local feature map.
	private LocalNetworkParam _param;
	//the instances assigned to this thread.
	private Instance[] _instances_input;
	//the instances assigned to this thread.
	private Instance[] _instances_output;
	//the builder.
	private NetworkCompiler _compiler;
	private boolean _cacheParam = true;
	
	//please make sure the threadId is 0-indexed.
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler){
		this(threadId, fm, instances, compiler, false);
	}
	
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, boolean cacheParam){
		this(threadId, fm, instances, compiler, new LocalNetworkParam(threadId, fm, instances.length), cacheParam);
	}
	
	//please make sure the threadId is 0-indexed.
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, LocalNetworkParam param, boolean cacheParam){
		this._threadId = threadId;
		this._param = param;
		fm.setLocalNetworkParams(this._threadId, this._param);
//		if(NetworkConfig._numThreads==1){
//			System.err.println("Set to global mode??");
////			System.exit(1);
//			this._param.setGlobalMode();//set it to global mode
//		}
		this._param.setGlobalMode();//set it to global mode
		this._instances_input = instances;
		this._compiler = compiler;
		this._cacheParam = cacheParam;
	}
	
	public LocalNetworkParam getParam(){
		return this._param;
	}
	
	@Override
	public void run(){
		this.max();
	}
	
	public void max(){
		long time = System.currentTimeMillis();
		this._instances_output = new Instance[this._instances_input.length];
		for(int k = 0; k<this._instances_input.length; k++){
//			System.err.println("Thread "+this._threadId+"\t"+k);
			this._instances_output[k] = this.max(this._instances_input[k], k);
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Decoding time for thread "+this._threadId+" = "+ time/1000.0 +" secs.");
	}
	
	public Instance max(Instance instance, int networkId){
		Network network = this._compiler.compileAndStore(networkId, instance, this._param);
		if(!_cacheParam){
			this._param.disableCache();
		}
		network.max();
		if(NetworkConfig.MAX_MARGINAL_DECODING){
			network.marginal();
		}
//		System.err.println("max="+network.getMax());
		return this._compiler.decompile(network);
	}
	
	public Instance[] getOutputs(){
		return this._instances_output;
	}
	
}
