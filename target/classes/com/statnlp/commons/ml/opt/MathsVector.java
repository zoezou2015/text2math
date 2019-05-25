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
package com.statnlp.commons.ml.opt;

import java.io.Serializable;
import java.util.Arrays;

public class MathsVector implements Serializable{
	
	private static final long serialVersionUID = -6493241066565944244L;
	
	private double[] _v;
	private double _L1 = -1;
	private double _L2 = -1;

	public MathsVector(double[] v){
		this._v = v;
	}

	//only if the entry is positive.
	public static double positiveSquare(double[] x){
		double v = 0.0;
		for(int k = 0; k<x.length; k++){
			if(x[k]>0){
				v += x[k] * x[k];
			}
		}
		return v;
	}

	public static double expSquare(double[] x){
		double v = 0.0;
		for(int k = 0; k<x.length; k++)
			v += Math.exp(x[k]) * Math.exp(x[k]);
		return v;
	}
	
	public static double square(double[] x){
		double v = 0.0;
		for(int k = 0; k<x.length; k++)
			v += x[k] * x[k];
		return v;
	}
	
	public MathsVector(int size){
		this._v = new double[size];
	}
	
	public int size(){
		return this._v.length;
	}

	public static double dotProd(double[] v1, double[] v2){
		assert v1.length == v2.length;
		double v = 0.0;
		for(int k = 0; k<v1.length; k++)
			v += v1[k] * v2[k];
		return v;
	}

	public static double positiveNorm(double[] v){
		return Math.sqrt(positiveSquare(v));
	}

	public static double expNorm(double[] v){
		return Math.sqrt(expSquare(v));
	}

	public static double norm(double[] v){
		return Math.sqrt(square(v));
	}
	
	public static double distance(double[] v1, double[] v2){
		assert v1.length == v2.length;
		double d = 0.0;
		for(int k = 0; k<v1.length; k++)
			d += (v1[k]-v2[k]) * (v1[k]-v2[k]);
		return Math.sqrt(d);
	}
	
	public static double cosineSim(double[] v1, double[] v2){
		assert v1.length == v2.length;
		return dotProd(v1, v2)/norm(v1)/norm(v2);
	}
	
	public double dotProd(MathsVector u){
		assert u.size() == this.size();
		double r = 0.0;
		for(int k = 0; k<u.size(); k++)
			r += this._v[k]*u._v[k];
		return r;
	}

	public void add(MathsVector u){
		assert this.size() == u.size();
		for(int k = 0; k<u.size(); k++)
			this._v[k] += u._v[k];
	}
	
	public double square(){
		double v = L2_norm();
		return v * v;
	}
	
	public double L2_norm(){
		if(this._L2 >=0)
			return this._L2;
		this._L2 = 0.0;
		for(double u : this._v)
			this._L2 += u * u;
		this._L2 = Math.sqrt(this._L2);
		return this._L2;
	}
	
	public double L1_norm(){
		if(this._L1 >=0)
			return this._L1;
		this._L1 = 0.0;
		for(double u : this._v)
			this._L1 += Math.abs(u);
		return this._L1;
	}
	
	@Override
	public String toString(){
		return Arrays.toString(this._v);
	}
	
}