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

import com.statnlp.commons.AttributedWord;

public abstract class TextSpan implements Serializable{
	
	private static final long serialVersionUID = -271990869644409937L;
	
	public AttributedWord[] _words;
	public int _bIndex;
	public int _eIndex;
	
	public TextSpan(AttributedWord[] words){
		this(words, 0, words.length);
	}
	
	public TextSpan(AttributedWord[] words, int bIndex, int eIndex){
		this._words = words;
		this._bIndex = bIndex;
		this._eIndex = eIndex;
	}

	public void expandAtt_BOW(){
		this.bowExpandAttributes("WORD", 5);
	}

	public void expandAtt_WORD(){
		this.crossExpandAttributes("WORD", 3);
	}
	
	public void expandAtt_WORD_simple(){
		this.linearExpandAttributes("WORD", 3);
	}
	
	public void expandAtt_POS(){
		this.crossExpandAttributes("POS", 3);
	}
	
	public void expandAtt_POS_simple(){
		this.linearExpandAttributes("POS", 3);
	}
	
	public void expandAtt_NER_simple(){
		this.linearExpandAttributes("NER", 3);
	}
	
	public void expandAtt_NE_WORD_TYPE(){
		this.crossExpandAttributes("NE_WORD_TYPE", 3);
	}
	
	public void expandAtt_NE_WORD_TYPE_simple(){
		this.linearExpandAttributes("NE_WORD_TYPE", 3);
	}
	
	private void bowExpandAttributes(String att, int window_size){
		for(int word_index = 0; word_index<this.length(); word_index++){
			this.bowExpandAttributes(att, window_size, word_index);
		}
	}
	
	private void bowExpandAttributes(String att, int window_size, int word_index){
		AttributedWord word = this.getWord(word_index);
		for(int k = 1; k<=window_size; k++){
			if(word_index-k>=0){
				ArrayList<String> vals = this.getWord(word_index-k).getAttribute(att);
				for(String val : vals){
					word.addAttribute(att+"-bow", val);
				}
			}
			if(word_index+k<this.length()){
				ArrayList<String> vals = this.getWord(word_index+k).getAttribute(att);
				for(String val : vals){
					word.addAttribute(att+"-bow", val);
				}
			}
		}
		ArrayList<String> vals = this.getWord(word_index).getAttribute(att);
		for(String val : vals){
			word.addAttribute(att+"-bow", val);
		}
	}
	
	private void linearExpandAttributes(String att, int window_size){
		for(int word_index = 0; word_index<this.length(); word_index++){
			this.linearExpandAttributes(att, window_size, word_index);
		}
	}
	
	private void linearExpandAttributes(String att, int window_size, int word_index){
		String[] prev, next;
		prev = new String[window_size];
		next = new String[window_size];
		for(int k = 0; k<window_size; k++){
			prev[k] = "";
			next[k] = "";
		}
		AttributedWord word = this.getWord(word_index);
		for(int k = 1; k<=window_size; k++){
			if(word_index-k>=0){
				ArrayList<String> vals = this.getWord(word_index-k).getAttribute(att);
				for(String val : vals){
					word.addAttribute(att+"-"+k, val);
				}
			}
			if(word_index+k<this.length()){
				ArrayList<String> vals = this.getWord(word_index+k).getAttribute(att);
				for(String val : vals){
					word.addAttribute(att+"+"+k, val);
				}
			}
		}
	}
	
	private void crossExpandAttributes(String att, int window_size){
		for(int word_index = 0; word_index<this.length(); word_index++){
			this.crossExpandAttributes(att, window_size, word_index);
		}
	}

	private void crossExpandAttributes(String att, int window_size, int word_index){
		String[] prev, next;
		prev = new String[window_size];
		next = new String[window_size];
		for(int k = 0; k<window_size; k++){
			prev[k] = "";
			next[k] = "";
		}
		
		AttributedWord word = this.getWord(word_index);
		for(int k = 1; k<=window_size; k++){
			if(word_index-k>=0)
				prev[k-1] = this.getWord(word_index-k).getAttribute(att).get(0);
			if(word_index+k<this.length())
				next[k-1] = this.getWord(word_index+k).getAttribute(att).get(0);
		}
		
		for(int len = 1; len<=window_size+1; len++){
//			for(int bIndex=word_index-window_size; bIndex+len<=word_index+window_size+1; bIndex++){
//				int eIndex = bIndex + len;
//				int offset = word_index - bIndex;
//				String att_new = att+":"+len+"|"+offset;
//				String val_new = "";
//				for(int index=bIndex; index<eIndex; index++){
//					String v = "";
//					if(index>=0 && index<this.length()){
//						v = this.getWord(index).getAttribute(att).get(0);
//					}
//					val_new +="/"+v;
//				}
//				word.addAttribute(att_new, val_new);
//			}
			
			for(int bIndex=word_index-len+1; bIndex<=word_index; bIndex++){
				int eIndex = bIndex + len;
				int offset = word_index - bIndex;
				String att_new = att+":"+len+"|"+offset;
				String val_new = "";
				for(int index=bIndex; index<eIndex; index++){
					String v = "";
					if(index>=0 && index<this.length()){
						v = this.getWord(index).getAttribute(att).get(0);
					}
					val_new +="/"+v;
				}
				word.addAttribute(att_new, val_new);
			}
		}
	}
	
	public AttributedWord getWord(int pos){
		return this._words[pos+this._bIndex];
	}
	
	public int length(){
		return this._eIndex - this._bIndex;
	}
	
	public String toLine(){
		StringBuilder sb = new StringBuilder();
		for(int k = 0; k<this._words.length; k++){
			if(k!=0) sb.append(' ');
			sb.append(this.getWord(k).getName());
		}
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof TextSpan){
			TextSpan span = (TextSpan)o;
			if(this.length()!=span.length())
				return false;
			for(int k = 0; k<this.length(); k++){
				if(!this.getWord(k).getName().equals(span.getWord(k).getName()))
					return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int k = 0; k<this._words.length; k++){
			if(k!=0) sb.append(' ');
			sb.append(this.getWord(k).getName());
		}
		return sb.toString();
	}
	
}