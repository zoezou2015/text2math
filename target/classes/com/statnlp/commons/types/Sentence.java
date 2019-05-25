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

/**
 * @author wei_lu
 *
 */
public class Sentence extends TokenArray{
	
	private static final long serialVersionUID = 9100609441891803234L;
	
	public Sentence(WordToken[] tokens) {
		super(tokens);
	}
	
	@Override
	public WordToken get(int index) {
		return (WordToken)this._tokens[index];
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int k = 0; k<this._tokens.length; k++){
			if(k!=0) sb.append(' ');
			sb.append(this._tokens[k].getName());
		}
		return sb.toString();
	}

}
