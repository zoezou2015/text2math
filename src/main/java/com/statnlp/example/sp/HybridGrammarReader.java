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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * @author wei_lu
 *
 */
public class HybridGrammarReader {
	
	public static void main(String args[])throws FileNotFoundException{
		
		String filename = "data/hybridgrammar.txt";
		
		HybridGrammar g = read(filename);
		
		for(int k=0; k<3; k++){
			System.err.println("arity:"+k);
			System.err.println(g.getRules(k));
			System.err.println();
		}
		
	}
	
	public static HybridGrammar read(String filename) throws FileNotFoundException{
		
		HybridGrammar g = new HybridGrammar();
		
		Scanner scan = new Scanner(new File(filename));
		
		while(scan.hasNextLine()){
			
			String line = scan.nextLine().trim();
			
			if(line.equals("")){
				continue;
			}
			
			if(line.indexOf("#")!=-1){
				int index = line.indexOf("#");
				line = line.substring(0, index).trim();
				if(line.equals("")){
					continue;
				}
			}
			
			int index;
			
			if(line.equals("all patterns")){
				line = scan.nextLine().trim();
				String[] forms = line.split("\\s");
				for(String form : forms){
					g.toHybridPattern(form);
				}
			}
			
			else if(line.equals("arity=0")){
				line = scan.nextLine().trim();
				String[] forms = line.split("\\s");
				HybridPattern p_root = g.toHybridPattern(forms[forms.length-1]);
				g.setRootPatternByArity(0, p_root);
				{
					HybridPattern[] ps = new HybridPattern[forms.length];
					for(int k = 0; k<forms.length; k++){
						ps[k] = g.toHybridPattern(forms[k]);
					}
					g.setPatternsByArity(0, ps);
				}

				while(!(line=scan.nextLine()).trim().equals(".")){
					index = line.indexOf("=>");
					String lhs_form = line.substring(0, index).trim();
					HybridPattern lhs = g.toHybridPattern(lhs_form);
					
					String[] RHS_form = line.substring(index+2).trim().split("\\|");
					for(String rhs_form : RHS_form){
						String[] ps_form = rhs_form.trim().split("\\s");
						HybridPattern[] RHS = new HybridPattern[ps_form.length];
						for(int k = 0; k<RHS.length; k++){
							RHS[k] = g.toHybridPattern(ps_form[k]);
						}
						g.addRule(0, lhs, RHS);
					}
				}
			}
			
			else if(line.equals("arity=1")){
				line = scan.nextLine().trim();
				String[] forms = line.split("\\s");
				HybridPattern p_root = g.toHybridPattern(forms[forms.length-1]);
				g.setRootPatternByArity(1, p_root);
				{
					HybridPattern[] ps = new HybridPattern[forms.length];
					for(int k = 0; k<forms.length; k++){
						ps[k] = g.toHybridPattern(forms[k]);
					}
					g.setPatternsByArity(1, ps);
				}
				
				while(!(line=scan.nextLine()).trim().equals(".")){
					index = line.indexOf("=>");
					String lhs_form = line.substring(0, index).trim();
					HybridPattern lhs = g.toHybridPattern(lhs_form);
					
					String[] RHS_form = line.substring(index+2).trim().split("\\|");
					for(String rhs_form : RHS_form){
						String[] ps_form = rhs_form.trim().split("\\s");
						HybridPattern[] RHS = new HybridPattern[ps_form.length];
						for(int k = 0; k<RHS.length; k++){
							RHS[k] = g.toHybridPattern(ps_form[k]);
						}
						g.addRule(1, lhs, RHS);
					}
				}
			}
			
			else if(line.equals("arity=2")){
				line = scan.nextLine().trim();
				String[] forms = line.split("\\s");
				HybridPattern p_root = g.toHybridPattern(forms[forms.length-1]);
				g.setRootPatternByArity(2, p_root);
				{
					HybridPattern[] ps = new HybridPattern[forms.length];
					for(int k = 0; k<forms.length; k++){
						ps[k] = g.toHybridPattern(forms[k]);
					}
					g.setPatternsByArity(2, ps);
				}

				while(!(line=scan.nextLine()).trim().equals(".")){
					index = line.indexOf("=>");
					String lhs_form = line.substring(0, index).trim();
					HybridPattern lhs = g.toHybridPattern(lhs_form);
					
					String[] RHS_form = line.substring(index+2).trim().split("\\|");
					for(String rhs_form : RHS_form){
						String[] ps_form = rhs_form.trim().split("\\s");
						HybridPattern[] RHS = new HybridPattern[ps_form.length];
						for(int k = 0; k<RHS.length; k++){
							RHS[k] = g.toHybridPattern(ps_form[k]);
						}
						g.addRule(2, lhs, RHS);
					}
				}
			}
			
			else {
				System.err.println(line);
			}
		}
		
		scan.close();
		
		return g;
		
	}

}
