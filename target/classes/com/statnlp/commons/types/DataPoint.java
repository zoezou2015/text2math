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
package com.statnlp.commons.types;

import java.io.Serializable;
import java.util.Arrays;

import com.statnlp.commons.ml.opt.MathsVector;

/**
 * @author wei_lu
 *
 */
public class DataPoint implements Serializable{
	
	private static final long serialVersionUID = -2611188937952909789L;
	
	protected double[] _vec;
	
	public DataPoint(double[] vec){
		this._vec = vec;
		if(Double.isNaN(this._vec[0])){
			throw new RuntimeException("x");
		}
	}
	
	public DataPoint copy(){
		return new DataPoint(this._vec.clone());
	}
	
	public double[] getVec(){
		return this._vec;
	}
	
	public void add(DataPoint pt){
		for(int k = 0; k< pt._vec.length; k++){
			this._vec[k] += pt._vec[k];
		}
	}
	
	public void multiply(double v){
		for(int k = 0; k< this._vec.length; k++){
			this._vec[k] *= v;
		}
	}
	
	public double distance(DataPoint pt){
		return MathsVector.distance(_vec, pt._vec);
	}
	
	public void reset(){
		Arrays.fill(this._vec, 0.0);
	}
	
}
