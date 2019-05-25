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
import java.util.HashSet;

public class WordUtil {
	
	public static HashSet<String> _func_words;
	
	public static String getNEWordType(String word){
		if(word.equals("\"") || word.equals("\'")){
			return "TYPE_Quote";
		}
		if(isFunctionWord(word)){
			return "TYPE_Function";
		}
		if(isPunctuationMark(word)){
			return "TYPE_Punctuation";
		}
		if(isInitialCaps(word)){
			return "TYPE_Capitalized";
		}
		if(isAllLowerCase(word)){
			return "TYPE_LowerCase";
		}
		return "TYPE_Other";
	}
	
	public static void setFunctionWords(ArrayList<String> func_words){
		_func_words = new HashSet<String>();
		for(String func_word : func_words){
			_func_words.add(func_word);
		}
	}
	
	public static boolean isFunctionWord(String word){
		if(isReserved(word)) return false;
		return _func_words.contains(word);
	}
	
	private static String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	
	public static boolean isMonth(String word){
		if(isReserved(word)) return false;
		for(String month : months){
			if(month.equalsIgnoreCase(word))
				return true;
		}
		return false;
	}
	
	public static String normalizeMonth(String word){
		if(isReserved(word)) return word;
		if(isMonth(word))
			return "*MONTH*";
		return word;
	}
	
	public static String normalizeDigits(String word){
		if(containsLetter(word)) return word;
		//1996-08-29
		char[] ch = word.toCharArray();
		StringBuilder sb = new StringBuilder();
		for(char c : ch){
			if(Character.isDigit(c)){
				sb.append("*D*");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static boolean isReserved(String word){
		return word.startsWith("*") && word.endsWith("*");
	}
	
	public static boolean containsLetter(String word){
		char[] ch = word.toCharArray();
		for(char c : ch){
			if(Character.isLetter(c))
				return true;
		}
		return false;
	}
	
	public static boolean containsDigit(String word){
		if(isReserved(word)) return false;
		if(word.length()==0)
			return false;
		for(char c : word.toCharArray()){
			if(Character.isDigit(c)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isAllUpperCase(String word){
		if(isReserved(word)) return false;
		if(word.length()==0)
			return false;
		for(char c : word.toCharArray()){
			if(!Character.isUpperCase(c)){
				return false;
			}
		}
		return true;
	}
	
	public static boolean isAllLowerCase(String word){
		if(isReserved(word)) return false;
		if(word.length()==0)
			return false;
		for(char c : word.toCharArray()){
			if(!Character.isLowerCase(c)){
				return false;
			}
		}
		return true;
	}
	
	public static boolean isInitialCaps(String word){
		if(isReserved(word)) return false;
		return word.length() >0 && Character.isUpperCase(word.charAt(0));
	}
	
	public static boolean isInitialCapsOnly(String word){
		if(isReserved(word)) return false;
		return word.length() > 1 && Character.isUpperCase(word.charAt(0)) && isAllLowerCase(word.substring(1));
	}
	
	public static boolean isAllDigits(String word){
		if(isReserved(word)) return false;
		if(word.length()==0)
			return false;
		for(char c : word.toCharArray()){
			if(!Character.isDigit(c)){
				return false;
			}
		}
		return true;
	}
	
	public static boolean isAllLetters(String word){
		if(isReserved(word)) return false;
		if(word.length()==0)
			return false;
		for(char c : word.toCharArray()){
			if((c>='a' && c<='z') || (c>='A' && c<='Z')){
			} else {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isNumber(String word){
		if(isReserved(word)) return false;
		try{
			Double.parseDouble(word);
		} catch(Exception e){
			return false;
		}
		return true;
	}
	
	public static boolean isAllAlphaNumeric(String word){
		if(isReserved(word)) return false;
		if(word.length()==0)
			return false;
		for(char c : word.toCharArray()){
			if(!Character.isDigit(c) && !Character.isLetter(c)){
				return false;
			}
		}
		return true;
	}
	
	public static boolean containsDots(String word){
		if(isReserved(word)) return false;
		for(char c : word.toCharArray()){
			if(c=='.')
				return true;
		}
		return false;
	}

	public static boolean isRomanNumber(String word){
		if(isReserved(word)) return false;
		for(char c : word.toCharArray()){
			if(c=='I' || c=='V' || c=='X'){
			} else {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isSingleChar(String word){
		if(isReserved(word)) return false;
		return word.length() == 1;
	}
	
	public static boolean isLonelyInitial(String word){
		if(isReserved(word)) return false;
		return isAllUpperCase(word) && word.endsWith(".");
	}
	
	public static boolean isPunctuationMark(String word){
		if(isReserved(word)) return false;
		if(word.equals(".") || word.equals(",") || word.equals("!") || word.equals("?") || word.equals(":") || word.equals(";")
				|| word.equals("\"") || word.equals("\'") || word.equals("(") || word.equals(")") || word.equals("-")){
			return true;
		}
		return false;
	}
	
	public static boolean isURL(String word){
		if(isReserved(word)) return false;
		word = word.toLowerCase();
		return word.startsWith("http://")
				|| word.startsWith("https://")
				|| word.startsWith("ftp://")
				|| word.startsWith("www.")
				|| word.endsWith(".com")
				|| word.endsWith(".net")
				|| word.endsWith(".org")
				|| word.indexOf(".com/") !=-1
				|| word.indexOf(".net/") !=-1
				|| word.indexOf(".org/") !=-1;
	}
	
}
