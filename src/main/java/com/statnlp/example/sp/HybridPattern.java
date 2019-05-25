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

import com.statnlp.commons.types.Token;

/**
 * @author wei_lu
 *
 */
public class HybridPattern implements Token{
	
	private static final long serialVersionUID = -3236262436003910366L;
	
	private int _id;
	private String _name;
	
	public HybridPattern(String name){
		this._name = name;
	}
	
	public int minLen(){
		return this._name.length();
	}
	
	public int maxLen(){
		int len = 0;
		for(int k = 0; k<this._name.length(); k++){
			char c = this._name.charAt(k);
			if(c=='X' || c=='Y' || c=='W'){
				return Integer.MAX_VALUE;
			} else {
				len ++;
			}
		}
		return len;
	}
	
	public boolean isA(){
		return this._name.equals("A");
	}
	
	public boolean isB(){
		return this._name.equals("B");
	}
	
	public boolean isC(){
		return this._name.equals("C");
	}
	
	public boolean isw(){
		return this._name.equals("w");
	}
	
	public boolean isW(){
		return this._name.equals("W");
	}
	
	public boolean isX(){
		return this._name.equals("X");
	}
	
	public boolean isY(){
		return this._name.equals("Y");
	}
	
	public char getFormat(int index){
		if(index>=0)
			return this._name.charAt(index);
		return this._name.charAt(this._name.length()+index);
	}
	
	public void setId(int id){
		this._id = id;
	}
	
	@Override
	public int hashCode(){
		return this._name.hashCode() + 7;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof HybridPattern){
			HybridPattern p = (HybridPattern)o;
			return this._name.equals(p._name);
		}
		return false;
	}
	
	@Override
	public int getId() {
		return this._id;
	}
	
	@Override
	public String getName() {
		return this._name;
	}
	
	@Override
	public String toString(){
		return "PATTERN:"+this._name;
	}
	
}
