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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author wei_lu
 *
 */
public class SemTextDataManager implements Serializable {

	private static final long serialVersionUID = 7131629077691962523L;

	private HashMap<SemanticUnit, SemanticUnit> _unitMap;
	private ArrayList<SemanticUnit> _unitList;

	private HashMap<SemanticType, ArrayList<SemanticUnit>> _type2units;

	private HashMap<SemanticType, SemanticType> _typeMap;
	private ArrayList<SemanticType> _typeList;
	private ArrayList<SemanticUnit> _rootUnits;

	private HashMap<SemanticUnit, ArrayList<SemanticUnit>> _validUnitPair_0;
	private HashMap<SemanticUnit, ArrayList<SemanticUnit>> _validUnitPair_1;

	private HashMap<SemanticUnit, ArrayList<String>> _prior2phrases;

	private boolean _semanticUnitsFixed = false;

	public SemTextDataManager() {
		this._unitMap = new HashMap<>();
		this._unitList = new ArrayList<>();

		this._type2units = new HashMap<>();

		this._typeMap = new HashMap<>();
		this._typeList = new ArrayList<>();

		this._rootUnits = new ArrayList<>();

		this._validUnitPair_0 = new HashMap<>();
		this._validUnitPair_1 = new HashMap<>();

		this._prior2phrases = new HashMap<>();
	}

	public ArrayList<String> getPriorUnitToPhrases(SemanticUnit unit) {
		return this._prior2phrases.get(unit);
	}

	public void addPriorUnitToPhrases(SemanticUnit unit, String phrase) {
		if (!this._prior2phrases.containsKey(unit)) {
			this._prior2phrases.put(unit, new ArrayList<String>());
		}
		ArrayList<String> phrases = this._prior2phrases.get(unit);
		if (phrases.contains(phrase)) {
			System.err.println("The phrase already exists:[" + phrase + "]");
			System.exit(1);
		}
		phrases.add(phrase);
	}

	public void addValidUnitPair(SemanticUnit parent, SemanticUnit child, int index) {
		HashMap<SemanticUnit, ArrayList<SemanticUnit>> map = index == 0 ? this._validUnitPair_0 : this._validUnitPair_1;
		if (!map.containsKey(parent))
			map.put(parent, new ArrayList<SemanticUnit>());

		ArrayList<SemanticUnit> children = map.get(parent);
		if (!children.contains(child)) {
			children.add(child);
		}
	}

	// public boolean isValidUnitPair(SemanticUnit parent, SemanticUnit child, int
	// index) {
	// HashMap<SemanticUnit, ArrayList<SemanticUnit>> map = index == 0 ?
	// this._validUnitPair_0 : this._validUnitPair_1;
	// if (!map.containsKey(parent))
	// return false;
	//
	// // if the child is context independent..
	// if (parent.getRHS()[index].equals(child.getLHS()) &&
	// child.isContextIndependent()) {
	// return true;
	// }
	//
	// ArrayList<SemanticUnit> children = map.get(parent);
	// return children.contains(child);
	// }
	public boolean isValidUnitPair(SemanticUnit parent, SemanticUnit child, int index) {
		HashMap<SemanticUnit, ArrayList<SemanticUnit>> map = index == 0 ? this._validUnitPair_0 : this._validUnitPair_1;
		if (!map.containsKey(parent))
			return false;

		// if the child is context independent..
		// if (parent.getRHS()[index].equals(child.getLHS()) &&
		// child.isContextIndependent()) {
		// return true;
		// }
		if (child.isContextIndependent()) {
			return true;
		}

		ArrayList<SemanticUnit> children = map.get(parent);
		return children.contains(child);
	}

	public void recordRootUnit(SemanticUnit unit) {
		int index = this._rootUnits.indexOf(unit);
		if (index < 0) {
			this._rootUnits.add(unit);
		}
	}

	public ArrayList<SemanticUnit> getRootUnits() {
		return this._rootUnits;
	}

	public ArrayList<SemanticType> getAllTypes() {
		return this._typeList;
	}

	public ArrayList<SemanticUnit> getAllUnits() {
		return this._unitList;
	}

	public ArrayList<SemanticUnit> getUnitsByType(SemanticType type) {
		return this._type2units.get(type);
	}

	public SemanticUnit toSemanticUnit(String lhs, String[] name, String[] rhs, String mrl, String[] rhsTokens) {
		SemanticType type_lhs = this.toSemanticType(lhs);
		SemanticType[] type_rhs = new SemanticType[rhs.length];
		for (int k = 0; k < type_rhs.length; k++)
			type_rhs[k] = this.toSemanticType(rhs[k]);
		SemanticUnit unit = new SemanticUnit(type_lhs, name, type_rhs, mrl, rhsTokens);
		if (this._unitMap.containsKey(unit))
			return this._unitMap.get(unit);
		if (this._semanticUnitsFixed) {
			return unit;
		}
		unit.setId(this._unitList.size());
		this._unitList.add(unit);
		this._unitMap.put(unit, unit);
		if (!this._type2units.containsKey(type_lhs)) {
			this._type2units.put(type_lhs, new ArrayList<SemanticUnit>());
		}
		this._type2units.get(type_lhs).add(unit);
		return unit;
	}

	public SemanticUnit toSemanticUnit(String name) {
		SemanticUnit unit = new SemanticUnit(name);
		if (this._unitMap.containsKey(unit))
			return this._unitMap.get(unit);
		if (this._semanticUnitsFixed) {
			return unit;
		}
		unit.setId(this._unitList.size());
		this._unitList.add(unit);
		this._unitMap.put(unit, unit);
		// if (!this._type2units.containsKey(type_lhs)) {
		// this._type2units.put(type_lhs, new ArrayList<SemanticUnit>());
		// }
		// this._type2units.get(type_lhs).add(unit);
		return unit;
	}

	public void fixSemanticUnits() {
		this._semanticUnitsFixed = true;
	}

	public SemanticType toSemanticType(String type_form) {
		SemanticType type = new SemanticType(type_form);
		if (this._typeMap.containsKey(type))
			return this._typeMap.get(type);
		type.setId(this._typeList.size());
		this._typeList.add(type);
		this._typeMap.put(type, type);
		return type;
	}

}
