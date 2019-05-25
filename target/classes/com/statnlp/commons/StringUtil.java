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

import java.util.StringTokenizer;

public class StringUtil {
	
	public static int numTokens(String input){
		StringTokenizer st = new StringTokenizer(input);
		return st.countTokens();
	}
	
	public static String stripSpaces(String input){
		StringTokenizer st = new StringTokenizer(input);
		StringBuilder sb = new StringBuilder();
		while(st.hasMoreTokens()){
			sb.append(" ");
			sb.append(st.nextToken());
		}
		
		return sb.toString().trim();
	}
	
	public static String stripXMLTags(String input){
		StringBuilder sb = new StringBuilder();
		
		boolean record = true;
		char[] chs = input.toCharArray();
		for(char ch : chs){
			if(ch=='<'){
				sb.append(' ');
				record = false;
			} else if(ch=='>'){
				sb.append(' ');
				record = true;
			} else if(record){
				if(ch=='-' || ch=='/'){
					sb.append(' ');
					sb.append(ch);
					sb.append(' ');
				} else {
					sb.append(ch);
				}
			}
		}
		
		String output = sb.toString();
		
		return stripSpaces(output);
	}
	
}
