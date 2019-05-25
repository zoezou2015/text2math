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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies an input stream to an output stream.
 * 
 * @author ywwong
 *
 */
public class InputStreamWriter extends Thread {

    private InputStream in;
    private OutputStream out;
    private boolean closeIn;
    private boolean closeOut;
    
    public InputStreamWriter(InputStream in, OutputStream out, boolean closeIn, boolean closeOut) {
        this.in = in;
        this.out = out;
        this.closeIn = closeIn;
        this.closeOut = closeOut;
    }
    
    public InputStreamWriter(InputStream in, OutputStream out) {
    	this(in, out, false, false);
    }
    
    public void run() {
        try {
        	int c;
        	while ((c = in.read()) >= 0)
        		out.write(c);
        	if (closeIn)
        		in.close();
            if (closeOut)
            	out.close();
        } catch (IOException e) {}
    }
    
}
