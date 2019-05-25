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
package com.statnlp.commons.types;

import java.io.Serializable;

/**
 * A base class representing an instance, to hold the surface form (e.g., the words of a sentence) of a 
 * training or test instance.<br>
 * This instance can be converted into a {@link Network} using the {@link NetworkCompiler}.<br>
 * Note that it is important to call the {@link #setLabeled()} method on training data, as otherwise this 
 * instance will not be considered during training
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class Instance implements Serializable{
	
	private static final long serialVersionUID = 4998596827132890817L;
	
	/** The ID of this instance*/
	protected int _instanceId;
	/**
	 * The weight (importance) of this instance.<br>
	 * This is also used in the network score calculation, with negative weight for unlabeled.
	 */
	protected double _weight;
	/** Whether current instance represents a labeled instance */
	protected boolean _isLabeled;
	/** The labeled version of this instance, if exists, null otherwise */
	private Instance _labeledInstance;
	/** The unlabeled version of this instance, if exists, null otherwise */
	private Instance _unlabeledInstance;
	
	/**
	 * Create an instance.
	 * The instance id should not be zero.
	 * @param instanceId
	 * @param weight
	 */
	public Instance(int instanceId, double weight){
		if(instanceId==0)
			throw new RuntimeException("The instance id is "+instanceId);
		this._instanceId = instanceId;
		this._weight = weight;
	}
	
	/**
	 * Returns the instance ID
	 * @return
	 */
	public int getInstanceId(){
		return this._instanceId;
	}
	
	/**
	 * Sets the instance ID
	 * @param instanceId
	 */
	public void setInstanceId(int instanceId){
		this._instanceId = instanceId;
	}
	
	/**
	 * Returns the instance weight
	 * @return
	 */
	public double getWeight(){
		return this._weight;
	}
	
	/**
	 * Sets the instance weight
	 * @param weight
	 */
	public void setWeight(double weight){
		this._weight = weight;
	}
	
	/**
	 * The size of this instance, usually the length of the input sequence
	 * @return
	 */
	public abstract int size();
	
	/**
	 * Whether this instance is a labeled instance (as opposed to unlabeled instance)
	 * @return
	 */
	public boolean isLabeled(){
		return this._isLabeled;
	}
	
	/**
	 * Set this instance as a labeled instance
	 */
	public void setLabeled(){
		this._isLabeled = true;
	}
	
	/**
	 * Set this instance as an unlabeled instance
	 */
	public void setUnlabeled(){
		this._isLabeled = false;
	}
	
	/**
	 * Returns the labeled instance<br>
	 * If this instance is a labeled instance, this will return itself
	 * @return
	 */
	public Instance getLabeledInstance(){
		if(isLabeled()){
			return this;
		}
		return this._labeledInstance;
	}
	
	/**
	 * Sets the labeled instance
	 * @param inst
	 */
	public void setLabeledInstance(Instance inst){
		this._labeledInstance = inst;
	}
	
	/**
	 * Returns the unlabeled instance<br>
	 * If this instance is an unlabeled instance, this will return itself
	 * @return
	 */
	public Instance getUnlabeledInstance(){
		return this._unlabeledInstance;
	}
	
	/**
	 * Sets the unlabeled instance
	 * @param inst
	 */
	public void setUnlabeledInstance(Instance inst){
		this._unlabeledInstance = inst;
	}
	
	/**
	 * Return the duplicate (i.e., clone) of the current instance
	 * @return
	 */
	public abstract Instance duplicate();
	
	public abstract void removeOutput();
	public abstract void removePrediction();
	
	public abstract Object getInput();
	public abstract Object getOutput();
	public abstract Object getPrediction();
	
	public abstract boolean hasOutput();
	public abstract boolean hasPrediction();
	
	public abstract void setPrediction(Object o);
	
}