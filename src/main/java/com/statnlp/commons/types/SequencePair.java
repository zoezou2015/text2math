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

import java.util.ArrayList;

public class SequencePair<SRC, TGT> {
	
	private ArrayList<SRC> _src;
	private ArrayList<TGT> _tgt;
	private double _weight;
	
	public SequencePair(ArrayList<SRC> src, ArrayList<TGT> tgt, double weight){
		this._src = src;
		this._tgt = tgt;
		this._weight = weight;
	}
	
	public ArrayList<SRC> getSrc(){
		return this._src;
	}
	
	public ArrayList<TGT> getTgt(){
		return this._tgt;
	}
	
	public double getWeight(){
		return this._weight;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this._weight);
		sb.append('\n');
		for(int i = 0; i<this._src.size(); i++){
			if(i!=0) sb.append(' ');
			sb.append(this._src.get(i));
		}
		sb.append('\n');
		for(int i = 0; i<this._tgt.size(); i++){
			if(i!=0) sb.append(' ');
			sb.append(this._tgt.get(i));
		}
		sb.append('\n');
		return sb.toString();
	}
}
