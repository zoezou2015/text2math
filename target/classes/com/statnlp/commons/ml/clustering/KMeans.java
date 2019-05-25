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
package com.statnlp.commons.ml.clustering;

import java.util.Arrays;
import java.util.Random;

import com.statnlp.commons.types.DataPoint;

/**
 * @author wei_lu
 *
 */
public class KMeans {
	
	private DataPoint[] _data;
	private int[] _memberships;
	private DataPoint[] _means;
	private int[] _numElems;
	private int _K;
	
//	public KMeans(DataPoint[] data, DataPoint[] means){
//		this._data = data;
//		this._means = means;
//		this._memberships = new int[this._data.length];
//		this._numElems = new int[means.length];
//	}
	
	public KMeans(DataPoint[] data, int K){
		this._data = data;
		this._memberships = new int[this._data.length];
		this._K = K;
		this._means = new DataPoint[this._K];
		this._numElems = new int[this._K];
	}
	
	public void viewCluster(int c){
		for(int i = 0; i<this._memberships.length; i++){
			if(this._memberships[i]==c){
				System.err.println(i+"\t"+this._data[i]);
			}
		}
	}
	
	public DataPoint[] getData(){
		return this._data;
	}
	
	public DataPoint[] getMeans(){
		return this._means;
	}
	
	public void run(int numIts){
		this.init_rand();
		for(int k = 0; k<numIts; k++){
			System.err.println("Iteration "+k);
			this.E();
			this.M();
		}
	}
	
	public void init_rand(){
		Random rand = new Random(1234);
		for(int k = 0; k<this._K; k++){
			int v = rand.nextInt(this._data.length);
//			System.err.println(this._means.length+"\t"+this._data.length+"\t"+k+"\t"+v);
			this._means[k] = this._data[v].copy();
		}
	}
	
	public void E(){
		for(int k = 0; k<this._numElems.length; k++){
			Arrays.fill(this._numElems, 0);
		}
		for(int i = 0; i<this._data.length; i++){
			DataPoint data = this._data[i];
			double mean_val = Double.POSITIVE_INFINITY;
			int mean_k = -1;
			for(int k = 0; k<this._means.length; k++){
				DataPoint mean = this._means[k];
				double val = mean.distance(data);
				if(val < mean_val){
					mean_k = k;
					mean_val = val;
				}
			}
			if(mean_k==-1){
				throw new RuntimeException("x:"+i+":"+mean_val+"\t"+data);
			}
			this._memberships[i] = mean_k;
			this._numElems[mean_k]++;
		}
	}
	
	public void M(){
		for(int i = 0; i<this._means.length; i++){
			this._means[i].reset();
		}
		for(int i = 0; i<this._data.length; i++){
			DataPoint data = this._data[i];
			int membership = this._memberships[i];
			this._means[membership].add(data);
		}
		for(int k = 0; k<this._numElems.length; k++){
			this._means[k].multiply(1.0/this._numElems[k]);
		}
//		System.out.println(Arrays.toString(this._means));
	}
	
}
