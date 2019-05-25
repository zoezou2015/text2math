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
public abstract class TokenArray implements LinearChain{
	
	private static final long serialVersionUID = -6634757515655762172L;
	
	protected Token[] _tokens;
	
	public TokenArray(Token[] tokens){
		this._tokens = tokens;
	}
	
	@Deprecated
	@Override
	public Token get(int hIndex, int wIndex) {
		throw new RuntimeException("This is not used.");
	}
	
	@Override
	public Token get(int index) {
		return this._tokens[index];
	}
	
	@Override
	public int length() {
		return this._tokens.length;
	}
	
}
