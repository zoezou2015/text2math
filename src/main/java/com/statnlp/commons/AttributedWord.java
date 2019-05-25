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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class AttributedWord extends Word{
	
	private static final long serialVersionUID = -3254961188381878044L;
	
	private HashMap<String, ArrayList<String>> _attrs;
	
	public AttributedWord(String word){
		super(word);
		this._attrs = new HashMap<String, ArrayList<String>>();
		this.addAttribute("WORD", word);
		this.addNERAttributes();
	}
	
	public Set<String> getAttributes(){
		return this._attrs.keySet();
	}
	
	public boolean hasAttribute(String att){
		return this._attrs.containsKey(att);
	}
	
	public ArrayList<String> getAttribute(String attName){
		if(!this._attrs.containsKey(attName))
			throw new RuntimeException("The attribute "+attName+" does not exist.");
		return this._attrs.get(attName);
	}
	
	private void addNERAttributes(){
		
		String curr = this.getName();
		String type = "NER";
		
		if(WordUtil.isAllAlphaNumeric(curr)){
			this.addAttribute(type, "isAllAlphaNumeric-yes");
		} else {
			this.addAttribute(type, "isAllAlphaNumeric-no");
		}
		
		if(WordUtil.isAllDigits(curr)){
			this.addAttribute(type, "isAllDigits-yes");
		} else {
			this.addAttribute(type, "isAllDigits-no");
		}
		
		if(WordUtil.isAllLowerCase(curr)){
			this.addAttribute(type, "isAllLowerCase-yes");
		} else {
			this.addAttribute(type, "isAllLowerCase-no");
		}
		
		if(WordUtil.isAllUpperCase(curr)){
			this.addAttribute(type, "isAllUpperCase-yes");
		} else {
			this.addAttribute(type, "isAllUpperCase-no");
		}
		
		if(WordUtil.isInitialCaps(curr)){
			this.addAttribute(type, "isInitialCaps-yes");
		} else {
			this.addAttribute(type, "isInitialCaps-no");
		}
		
		if(WordUtil.isLonelyInitial(curr)){
			this.addAttribute(type, "isLonelyInitial-yes");
		} else {
			this.addAttribute(type, "isLonelyInitial-no");
		}
		
		if(WordUtil.isPunctuationMark(curr)){
			this.addAttribute(type, "isPunctuationMark-yes");
		} else {
			this.addAttribute(type, "isPunctuationMark-no");
		}
		
		if(WordUtil.isRomanNumber(curr)){
			this.addAttribute(type, "isRomanNumber-yes");
		} else {
			this.addAttribute(type, "isRomanNumber-no");
		}
		
		if(WordUtil.isSingleChar(curr)){
			this.addAttribute(type, "isSingleChar-yes");
		} else {
			this.addAttribute(type, "isSingleChar-no");
		}
		
		if(WordUtil.isURL(curr)){
			this.addAttribute(type, "isURL-yes");
		} else {
			this.addAttribute(type, "isURL-no");
		}
		
		if(WordUtil.containsDigit(curr)){
			this.addAttribute(type, "containsDigit-yes");
		} else {
			this.addAttribute(type, "containsDigit-no");
		}
		
		if(WordUtil.containsDots(curr)){
			this.addAttribute(type, "containsDots-yes");
		} else {
			this.addAttribute(type, "containsDots-no");
		}
		
	}
	
	public void addAttribute(String attName, String attValue){
		if(!this._attrs.containsKey(attName))
			this._attrs.put(attName, new ArrayList<String>());
		this._attrs.get(attName).add(attValue);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		Iterator<String> attNames = this._attrs.keySet().iterator();
		while(attNames.hasNext()){
			String attName = attNames.next();
			ArrayList<String> attValues = this._attrs.get(attName);
			sb.append("|");
			sb.append(attName);
			for(String attValue : attValues){
				sb.append(":");
				sb.append(attValue);
			}
		}
		return sb.toString();
	}
	
}