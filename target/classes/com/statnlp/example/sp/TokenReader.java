/*
 * Copyright 2006 Yuk Wah Wong (The University of Texas at Austin).
 * 
 * This file is part of the WASP distribution.
 *
 * WASP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * WASP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WASP; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package com.statnlp.example.sp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Reads text from a stream, and breaks the text into tokens.
 * 
 * @author ywwong
 *
 */
public class TokenReader {

	private BufferedReader in;
	
	public TokenReader(BufferedReader in) {
		this.in = in;
	}
	
	public TokenReader(InputStream in) {
		this.in = new BufferedReader(new InputStreamReader(in));
	}
	
	/**
	 * Closes this stream.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void close() throws IOException {
		in.close();
	}
	
	/**
	 * Reads a line of text and breaks it into tokens.  A line is considered to be terminated by any 
	 * one of a line feed (<code>'\n'</code>), a carriage return (<code>'\r'</code>), or a carriage 
	 * return followed immediately by a linefeed.  A token is a <code>String</code> with no whitespace
	 * characters in it.
	 * 
	 * @return a <code>String</code> array containing all tokens in the line; <code>null</code> if the 
	 * end of the stream has been reached.
	 * @throws IOException if an I/O error occurs.
	 */
	public String[] readLine() throws IOException {
		String line = in.readLine();
		if (line == null)
			return null;
		StringTokenizer st = new StringTokenizer(line);
		String[] results = new String[st.countTokens()];
		for(int k = 0; k<results.length; k++){
			results[k] = st.nextToken();
		}
		return results;
	}
	
}
