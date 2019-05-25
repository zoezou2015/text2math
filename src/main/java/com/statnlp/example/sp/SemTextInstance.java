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
package com.statnlp.example.sp;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

/**
 * @author wei_lu
 *
 */
public class SemTextInstance extends Instance{
	
	private static final long serialVersionUID = -8190693110092491424L;
	
	private Sentence _input;
	private SemanticForest _output;
	private String _mrl;
	private SemanticForest _prediction;
	
	public SemTextInstance(int instanceId, double weight, Sentence input, SemanticForest output, String mrl) {
		super(instanceId, weight);
		this._input = input;
		this._output = output;
		this._mrl = mrl;
	}
	
	public String getMRL(){
		return this._mrl;
	}
	
	@Override
	public int size() {
		return -1;
	}
	
	@Override
	public SemTextInstance duplicate() {
		SemTextInstance inst = new SemTextInstance(this._instanceId, this._weight, this._input, this._output, this._mrl);
		inst.setPrediction(this._prediction);
		return inst;
	}
	
	@Override
	public void removeOutput() {
		this._output = null;
	}
	
	@Override
	public void removePrediction() {
		this._prediction = null;
	}
	
	@Override
	public Sentence getInput() {
		return this._input;
	}
	
	@Override
	public SemanticForest getOutput() {
		return this._output;
	}
	
	@Override
	public SemanticForest getPrediction() {
		return this._prediction;
	}
	
	@Override
	public boolean hasOutput() {
		return this._output != null;
	}
	
	@Override
	public boolean hasPrediction() {
		return this._prediction != null;
	}
	
	@Override
	public void setPrediction(Object prediction) {
		this._prediction = (SemanticForest)prediction;
	}
	
}
