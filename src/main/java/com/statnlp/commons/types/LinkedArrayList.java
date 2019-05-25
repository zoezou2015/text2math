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
import java.util.ArrayList;

public class LinkedArrayList<X> implements Serializable{
	
	private static final long serialVersionUID = -8117631037991246949L;
	
	private ArrayList<X> _list;
	private LinkedArrayList<X> _remainList;
	private int _size = -1;
	
	public LinkedArrayList(ArrayList<X> list){
		this._list = list;
	}
	
	public LinkedArrayList(ArrayList<X> list, LinkedArrayList<X> remainList){
		this._list = list;
		this._remainList = remainList;
	}

	//note that this will simply append these features to the FRONT of the list.
	public LinkedArrayList<X> append(ArrayList<X> features){
		return new LinkedArrayList<X>(features, this);
	}
	
	//get the element at position index.
	public X get(int index){
		if(index>=this.size() || index<0){
			throw new RuntimeException("The index "+index+" is out of the range: 0-"+this.size());
		}
		if(index<this._list.size()){
			return this._list.get(index);
		} else {
			return this._remainList.get(index-this._list.size());
		}
	}
	
	//this returns the size of the list.
	public int size(){
		if(this._size >= 0){
			return this._size;
		}
		if(this._remainList == null){
			this._size = this._list.size();
		} else {
			this._size = this._list.size() + this._remainList.size();
		}
		return this._size;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		int size = this.size();
		for(int k = 0; k<size; k++){
			if(k!=0) sb.append(' ');
			sb.append(this.get(k));
		}
		return sb.toString();
	}
}
