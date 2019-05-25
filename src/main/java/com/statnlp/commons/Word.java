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

import com.statnlp.commons.types.Token;

public class Word implements Token{
	
	private static final long serialVersionUID = 6430618612816357597L;
	
	public static Word X = new Word("[X]");
	public static Word Y = new Word("[Y]");
	public static Word START = new Word("[START]");
	public static Word FINISH = new Word("[FINISH]");
	public static Word BEGIN = new Word("[BEGIN]");
	public static Word END = new Word("[END]");
	public static Word UNKNOWN = new Word("[UNKNOWN]");
	
	private String _form;
	
	public Word(String form){
//		this._form = WordUtil.normalizeDigits(form);
		this._form = form;
		//luwei: I don't remember why we need to do the following??
//		StringBuilder sb = new StringBuilder();
//		for(char ch : form.toCharArray()){
//			if(ch=='+')
//				sb.append("*PLUS*");
//			else
//				sb.append(ch);
//		}
//		this._form = sb.toString();
	}

	public static Word[] toWords(String sentence){
		String[] s = sentence.split("\\s");
		Word[] r = new Word[s.length];
		for(int k = 0; k<s.length; k++)
			r[k] = new Word(s[k]);
		return r;
	}

	public static Word[] toWords_forHALIGN(String sentence){
		String[] s = sentence.split("\\s");
		Word[] r = new Word[s.length+2];
		r[0] = Word.START;
		for(int k = 0; k<s.length; k++)
			r[k+1] = new Word(s[k]);
		r[s.length+1] = Word.FINISH;
		return r;
	}
	
	@Override
	public int getId() {
		return 0;
	}
	
	@Override
	public String getName(){
		return this._form;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Word)
			return this._form.equals(((Word)o)._form);
		return false;
	}
	
	@Override
	public int hashCode(){
		return this._form.hashCode() + 7;
	}
	
	@Override
	public String toString(){
		return this._form;
	}

}
