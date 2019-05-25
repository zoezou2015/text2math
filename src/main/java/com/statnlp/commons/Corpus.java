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
package com.statnlp.commons;

import java.io.Serializable;
import java.util.ArrayList;

import com.statnlp.commons.types.Instance;

public class Corpus implements Serializable{
	
	private static final long serialVersionUID = 1745123048791380303L;
	
	protected ArrayList<Instance> _instances;
	
	public Corpus(){
		this._instances = new ArrayList<Instance>();
	}
	
	public void add(Instance inst){
		this._instances.add(inst);
	}
	
	public int size(){
		return this._instances.size();
	}
	
	public Instance get(int k){
		return this._instances.get(k);
	}
	
}