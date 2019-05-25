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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author wei_lu
 *
 */
public class GeoqueryEvaluator {
	
	public static void main(String[] args) {
		GeoqueryEvaluator eval = new GeoqueryEvaluator();
		
		ArrayList<String> pred_trees = new ArrayList<String>();
		pred_trees.add("x");
		pred_trees.add("answer(high_point_1(state(next_to_2(stateid('mississippi')))))");
		ArrayList<String> gold_trees = new ArrayList<String>();
		gold_trees.add("answer(high_point_1(state(next_to_2(stateid('mississippi')))))");
		gold_trees.add("answer(high_point_1(state(next_to_2(stateid('mississippi')))))");
		try {
			eval.eval(pred_trees, gold_trees);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String toGeoQuery(SemanticForest tree){
		
		StringBuilder sb = new StringBuilder();
		this.toGeoQueryHelper(sb, tree.getRoot());
		return sb.toString();
		
	}
	
	private void toGeoQueryHelper(StringBuilder sb, SemanticForestNode node){
		
		if(node.isRoot()){
			this.toGeoQueryHelper(sb, node.getChildren()[0][0]);
			return;
		}
		
		SemanticUnit unit = node.getUnit();
		if(unit.arity() == 0){
			String[] rhs_tokens = unit.getRHSTokens();
			for(int k = 0; k < rhs_tokens.length; k++){
				sb.append(rhs_tokens[k]);
			}
		}
		
		else if(unit.arity() == 1){
			SemanticForestNode child1 = node.getChildren()[0][0];
			
			String[] rhs_tokens = unit.getRHSTokens();
			for(int k = 0; k < rhs_tokens.length; k++){
				if(rhs_tokens[k].startsWith("*")){
					this.toGeoQueryHelper(sb, child1);
				} else {
					sb.append(rhs_tokens[k]);
				}
			}
		}
		
		else if(unit.arity() == 2){
			SemanticForestNode child1 = node.getChildren()[0][0];
			SemanticForestNode child2 = node.getChildren()[1][0];
			
			String[] rhs_tokens = unit.getRHSTokens();
			int index = 0;
			for(int k = 0; k < rhs_tokens.length; k++){
				if(rhs_tokens[k].startsWith("*")){
					if(index==0)
						this.toGeoQueryHelper(sb, child1);
					else 
						this.toGeoQueryHelper(sb, child2);
					index++;
				} else {
					sb.append(rhs_tokens[k]);
				}
			}
		}
		
	}
	
	public void eval(ArrayList<String> pred_trees, ArrayList<String> gold_trees) throws IOException{
		
		boolean[][] results = evaluate(pred_trees, gold_trees);
		int corr = 0;
		for(int k = 0; k<results.length; k++){
			if(results[k][0]) corr ++;
		}
		System.err.println("Geoquery execution accuracy = " + (double)corr/results.length+"="+corr+"/"+results.length);
		
	}
	

	public boolean[][] evaluate(ArrayList<String> pred_trees, ArrayList<String> gold_trees) throws IOException {
		
		String path_to_sicstus_exec;
		String path_to_eval;
		Scanner scan;
		scan = new Scanner(new File("path_to_sicstus"));
		path_to_sicstus_exec = scan.nextLine().trim();
		scan.close();
		
		scan = new Scanner(new File("path_to_eval"));
		path_to_eval = scan.nextLine().trim();
		scan.close();
		
		String evalDir = path_to_eval;
		String execFile = path_to_sicstus_exec;
		File geobaseFile = new File(evalDir, "geobase.pl");
		File geoqueryFile = new File(evalDir, "geoquery.pl");
		File evalFile = new File(evalDir, "eval.pl");
		File dataFile = File.createTempFile("eval", ".pl");
		File outputFile = File.createTempFile("eval", ".out");
		dataFile.deleteOnExit();
		outputFile.deleteOnExit();
		PrintWriter dataOut = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)));
		dataOut.println(":-compile('"+geobaseFile.getPath()+"').");
		dataOut.println(":-compile('"+geoqueryFile.getPath()+"').");
		dataOut.println(":-compile('"+evalFile.getPath()+"').");
		dataOut.print(":-eval([");
		
		boolean first = true;
		int size = pred_trees.size();
		boolean[][] isCorrect = new boolean[size][];
		for (int i = 0; i < size; ++i) {
			String pred_tree = pred_trees.get(i);
			String gold_tree = gold_trees.get(i);
			isCorrect[i] = new boolean[1];
			for (int j = 0; j < isCorrect[i].length; ++j) {
				isCorrect[i][j] = pred_tree.equals(gold_tree);
				//if (!isCorrect[i][j]) 
				{
					if (first)
						first = false;
					else
						dataOut.print(',');
					dataOut.print(i);
					dataOut.print(',');
					dataOut.print(j);
					dataOut.print(',');
					dataOut.print(gold_tree);
					dataOut.print(',');
					dataOut.print(fixSpace(pred_tree));
				}
			}
		}
		
		dataOut.println("]).");
		dataOut.println(":-halt.");
		dataOut.close();

		// run Geoquery evaluation scripts on SICSTUS
		try {
			String cmd = execFile+" -l "+dataFile.getPath();
			Process proc = Runtime.getRuntime().exec(cmd);
			PrintStream out = new PrintStream(new FileOutputStream(outputFile), true);
			Thread outThread = new InputStreamWriter(proc.getInputStream(), out);
			outThread.start();
			new InputStreamWriter(proc.getErrorStream(), System.err).start();
			int exitVal = proc.waitFor();
			if (exitVal != 0) {
				return isCorrect;
			}
			outThread.join();
			out.close();
		} catch (IOException e) {
			return isCorrect;
		} catch (InterruptedException e) {
			return isCorrect;
		}
		// read output of the evaluation scripts
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(outputFile)));
		String[] line;
		while ((line = in.readLine()) != null) {
			for(int k = 0; k < 3; k++) {
				line[k] = line[k].replaceAll("'", "");
			}
			if (line[2].equals("y"))
				isCorrect[Integer.parseInt(line[0])][Integer.parseInt(line[1])] = true;
		}
		in.close();
		return isCorrect;
		
	}
	

	public String fixSpace(String s){
		return s;
//		
//		StringBuilder sb = new StringBuilder();
//		char[] ch = s.toCharArray();
//		for(int i = 0; i<ch.length; i++){
//			if(ch[i]==' '){
//				//ignore.
//			} else {
//				if(ch[i]=='\''){
//					sb.append(' ');
//					sb.append(ch[i]);
//					sb.append(' ');
//				} else {
//					sb.append(ch[i]);
//				}
//			}
//		}
//		return sb.toString();
	}
}
