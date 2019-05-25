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
public class HybridGrammar implements Serializable{
	
	private static final long serialVersionUID = 6966766440390761849L;
	
	private HashMap<HybridPattern, ArrayList<HybridPattern[]>> _rules0;
	private HashMap<HybridPattern, ArrayList<HybridPattern[]>> _rules1;
	private HashMap<HybridPattern, ArrayList<HybridPattern[]>> _rules2;
	
	private HashMap<String, HybridPattern> _str2pattern;
	private ArrayList<HybridPattern> _patternById;
	private HybridPattern[][] _patternsByArity;
	private HybridPattern[] _rootPatternByArity;
	
	private HybridPattern X;
	private HybridPattern Y;
	private HybridPattern w;
	
	public HybridGrammar(){
		this._rules0 = new HashMap<HybridPattern, ArrayList<HybridPattern[]>>();
		this._rules1 = new HashMap<HybridPattern, ArrayList<HybridPattern[]>>();
		this._rules2 = new HashMap<HybridPattern, ArrayList<HybridPattern[]>>();
		
		this._str2pattern = new HashMap<String, HybridPattern>();
		this._patternById = new ArrayList<HybridPattern>();
		this._patternsByArity = new HybridPattern[3][];
		this._rootPatternByArity = new HybridPattern[3];
	}

	public HybridPattern getw(){
		return this.w;
	}

	public HybridPattern getX(){
		return this.X;
	}

	public HybridPattern getY(){
		return this.Y;
	}
	
	public HybridPattern toHybridPattern(String form){
		if(this._str2pattern.containsKey(form))
			return this._str2pattern.get(form);
		HybridPattern p = new HybridPattern(form);
		p.setId(this._patternById.size());
		this._patternById.add(p);
		this._str2pattern.put(form, p);
		if(p.isX()) this.X = p;
		if(p.isY()) this.Y = p;
		if(p.isw()) this.w = p;
//		System.err.println(p+"\t"+p.getId());
		return p;
	}
	
	public HybridPattern getPatternById(int id){
		return this._patternById.get(id);
	}
	
	public void setPatternsByArity(int arity, HybridPattern[] patterns){
		this._patternsByArity[arity] = patterns;
	}
	
	public void setRootPatternByArity(int arity, HybridPattern pattern){
		System.err.println("ROOT pattern for arity "+arity+" is "+pattern);
		this._rootPatternByArity[arity] = pattern;
	}
	
	public HybridPattern getRootPatternByArity(int arity){
		return this._rootPatternByArity[arity];
	}
	
	public HybridPattern[] getPatternsByArity(int arity){
		return this._patternsByArity[arity];
	}
	
	public ArrayList<HybridPattern> getPatterns(){
		return this._patternById;
	}
	
	public HashMap<HybridPattern, ArrayList<HybridPattern[]>> getRules(int arity){
		if(arity == 0) return this._rules0;
		else if(arity == 1) return this._rules1;
		else if(arity == 2) return this._rules2;
		else return null;
	}
	
	public void addRule(int arity, HybridPattern lhs, HybridPattern[] RHS){
		
		HashMap<HybridPattern, ArrayList<HybridPattern[]>> rules;
		if(arity == 0){
			rules = this._rules0;
		} else if(arity == 1){
			rules = this._rules1;
		} else if(arity == 2){
			rules = this._rules2;
		} else {
			throw new RuntimeException("The arity "+arity+" is not supported.");
		}
		
		if(!rules.containsKey(lhs))
			rules.put(lhs, new ArrayList<HybridPattern[]>());
		rules.get(lhs).add(RHS);
	}
	
	public ArrayList<HybridPattern[]> getRHS(int arity, HybridPattern lhs){
		HashMap<HybridPattern, ArrayList<HybridPattern[]>> rules;
		if(arity == 0){
			rules = this._rules0;
		} else if(arity == 1){
			rules = this._rules1;
		} else if(arity == 2){
			rules = this._rules2;
		} else {
			throw new RuntimeException("The arity "+arity+" is not supported.");
		}
		
		return rules.get(lhs);
	}
	
}
