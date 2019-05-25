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

import java.util.Arrays;

import com.statnlp.commons.ml.opt.MathsVector;

/**
 * @author wei_lu
 *
 */
public class VectorWord extends DataPoint{
	
	private static final long serialVersionUID = -26697157096407981L;
	
	private String _word;
	private double[] _vec_new;
	
	public VectorWord(String word, double[] vec){
		super(vec);
		this._word = word;
	}
	
	public double[] getNewVec(){
		if(this._vec_new!=null){
			return this._vec_new;
		}
		double[][] coordinates = new double[this._vec.length][this._vec.length];
		for(int k = 0; k<this._vec.length; k++){
			coordinates[k][k] = 1.0;
		}
		this._vec_new = new double[this._vec.length];
		for(int k = 0; k<this._vec.length; k++){
			this._vec_new[k] = 1.0-MathsVector.cosineSim(this._vec, coordinates[k]);
		}
		return this._vec_new;
	}
	
	public String getWord(){
		return this._word;
	}
	
	//exponential normalization...
	public void expNorm(){
		double[] vec_new = new double[this._vec.length];
		double sum = 0;
		for(int k = 0; k<this._vec.length; k++){
			sum += Math.exp(this._vec[k]);
		}
		for(int k = 0; k<this._vec.length; k++){
			vec_new[k] = Math.exp(this._vec[k])/sum;
		}
		
		this._vec = vec_new;
	}

	public double positiveSim(VectorWord vw){
		double sim = 0;
		for(int k = 0; k<this._vec.length; k++){
			if(this._vec[k] > 0 && vw._vec[k] > 0){
				sim += this._vec[k] * vw._vec[k];
			}
		}
		sim/=MathsVector.positiveNorm(this._vec);
		sim/=MathsVector.positiveNorm(vw._vec);
		return sim;
	}

	public double expSim(VectorWord vw){
		double sim = 0;
		for(int k = 0; k<this._vec.length; k++){
			sim += Math.exp(this._vec[k] + vw._vec[k]);
		}
		sim/=MathsVector.expNorm(this._vec);
		sim/=MathsVector.expNorm(vw._vec);
		return sim;
	}
	
	public void addOffset(double offset){
		for(int k = 0; k<this._vec.length; k++){
			this._vec[k] += offset;
		}
	}

	public double newSim(VectorWord vw){
		if(this._vec_new==null){
			getNewVec();
		}
		double sim = 0;
		for(int k = 0; k<this._vec_new.length; k++){
			sim += this._vec_new[k] * vw._vec_new[k];
		}
		sim/=MathsVector.norm(this._vec_new);
		sim/=MathsVector.norm(vw._vec_new);
		return sim;
	}
	
	public double sim(VectorWord vw){
		double sim = 0;
		for(int k = 0; k<this._vec.length; k++){
			sim += this._vec[k] * vw._vec[k];
		}
		sim/=MathsVector.norm(this._vec);
		sim/=MathsVector.norm(vw._vec);
		return sim;
	}
	
	@Override
	public String toString(){
		return this._word + ":" + Arrays.toString(this._vec);
	}
	
}
