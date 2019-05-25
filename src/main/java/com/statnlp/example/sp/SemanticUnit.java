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

import java.util.Arrays;

import com.statnlp.commons.types.Token;

/**
 * @author wei_lu
 *
 */
public class SemanticUnit implements Token {

	private static final long serialVersionUID = 802406508503541117L;

	private String[] _name;// function name
	private int _id;
	private SemanticType _LHS;
	private SemanticType[] _RHS;
	private String _mrl;
	// if the unit is context independent, it means it can serve as the child of any
	// unit which is compatible to itself.
	private boolean _isContextIndependent;
	private int arity;
	// this will be useful for recovering mrls.
	private String[] _rhsTokens;

	public SemanticUnit(String name) {
		this(null, new String[] { name }, null, null, null);
	}

	public SemanticUnit(SemanticType LHS, String[] name, SemanticType[] RHS, String mrl, String[] rhsTokens) {
		this._LHS = LHS;
		this._name = name;
		this._RHS = RHS;
		this._mrl = mrl;
		this._isContextIndependent = false;
		this._rhsTokens = rhsTokens;
	}

	public String[] getRHSTokens() {
		return this._rhsTokens;
	}

	public void setContextIndependent() {
		this._isContextIndependent = true;
	}

	public boolean isContextIndependent() {
		return this._isContextIndependent;
	}

	public SemanticType getLHS() {
		return this._LHS;
	}

	public SemanticType[] getRHS() {
		return this._RHS;
	}

	public String getMRL() {
		// return this._mrl;
		return this._name[0];
	}

	public int arity() {
		return this.arity;
	}

	public void setArity(int arity) {
		this.arity = arity;
	}

	public void setId(int id) {
		this._id = id;
	}

	@Override
	public int getId() {
		return this._id;
	}

	@Override
	public String getName() {
		return Arrays.toString(this._name);
	}

	@Override
	public int hashCode() {
		// return (this._LHS.hashCode() + 7) ^ (Arrays.hashCode(this._name)) ^
		// (Arrays.hashCode(this._RHS) + 7)
		// ^ (this._mrl.hashCode() + 7);
		return 1 ^ (Arrays.hashCode(this._name)) ^ 1 ^ 1;
	}

	@Override
	public boolean equals(Object o) {
		// if (o instanceof SemanticUnit) {
		// SemanticUnit su = (SemanticUnit) o;
		// return this._LHS.equals(su._LHS) && Arrays.equals(this._RHS, su._RHS) &&
		// Arrays.equals(this._name, su._name)
		// && this._mrl.equals(su._mrl);
		// }
		if (o instanceof SemanticUnit) {
			SemanticUnit su = (SemanticUnit) o;
			return Arrays.equals(this._name, su._name);
		}
		return false;
	}

	@Override
	public String toString() {
		return this._name[0];
		// StringBuilder sb = new StringBuilder();
		// sb.append("UNIT:");
		// sb.append(this._LHS);
		// sb.append(" => ");
		// sb.append(Arrays.toString(this._name));
		// for(int k = 0; k<this._RHS.length; k++){
		// sb.append(' ');
		// sb.append(this._RHS[k]);
		// }
		// return sb.toString();
	}

}