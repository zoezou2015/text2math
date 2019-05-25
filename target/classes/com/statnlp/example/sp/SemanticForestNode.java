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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.example.equationparse.Variable;
import com.statnlp.commons.types.Token;
import com.statnlp.commons.types.WordToken;

/**
 * @author wei_lu
 *
 */
public class SemanticForestNode implements Token, Comparable<SemanticForestNode> {

	private static final long serialVersionUID = 4324674986215421825L;

	private int _id;
	private int _hIndex;// height
	private int _wIndex;// width
	private SemanticUnit _unit;
	private SemanticForestNode[][] _children;// the size must be the same as the arity.
	private ArrayList<SemanticForestNode> _allNodes;// all nodes below this now, including the current one.
	private boolean _isRoot = false;
	private double _score = Double.NEGATIVE_INFINITY;
	private String _info;
	private String _align;
	private String varId = "NULL";
	private double numValue = 0;
	private String valueString = "";
	private ArrayList<WordToken> tokens = new ArrayList<>();

	public static SemanticForestNode createRootNode(int height) {
		return new SemanticForestNode(height);
	}

	private SemanticForestNode(int height) {
		this._hIndex = height;
		this._wIndex = 1000;
		this._children = new SemanticForestNode[1][];
		this._isRoot = true;
	}

	public SemanticForestNode(SemanticUnit unit, int hIndex) {
		this._unit = unit;
		this._hIndex = hIndex;
		this._wIndex = unit.getId();
		if (this._wIndex > 1000) {
			throw new RuntimeException("You have too many units.. currently max is 1000.");
		}
		this._children = new SemanticForestNode[this.arity()][];
	}

	public void setInfo(String info) {
		this._info = info;
	}

	public String getInfo() {
		return this._info;
	}

	public void setVarId(String var) {
		this.varId = var;
	}

	public String getVarId() {
		return this.varId;
	}

	public void setNumValue(double value) {
		this.numValue = value;
	}

	public double getNumValue() {
		return this.numValue;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SemanticForestNode))
			return false;

		SemanticForestNode node = (SemanticForestNode) o;
		if (this.getUnit() != node.getUnit()) {
			return false;
		}
		return Arrays.deepEquals(this._children, node._children);
	}

	public void setScore(double score) {
		this._score = score;
	}

	public double getScore() {
		return this._score;
	}

	public int getHeight() {
		int height = 0;
		for (SemanticForestNode[] child : this._children) {
			for (SemanticForestNode c : child)
				height = Math.max(c.getHeight(), height);
		}
		return height + 1;
	}

	public void setAlignment(String align) {
		this._align = align;
	}

	public String getAlignment() {
		return this._align;
	}

	public String getValueString() {
		if (valueString.equals("")) {
			if (this._unit.getMRL().equals("EQU"))
				return "=";
			else if (this._unit.getMRL().equals("ADD")) {
				return "+";
			} else if (this._unit.getMRL().equals("SUB")) {
				return "-";
			} else if (this._unit.getMRL().equals("SUB_R")) {
				return "#";
			} else if (this._unit.getMRL().equals("MUL")) {
				return "*";
			} else if (this._unit.getMRL().equals("DVI")) {
				return "/";
			} else if (this._unit.getMRL().equals("DVI_R")) {
				return "$";
			} else if (this._unit.getMRL().equals("VAR")) {
				return "V";
			} else if (this._unit.getMRL().equals("NUM")) {
				return this.getNumValue() + "";
			}
		}
		return valueString;
	}

	public void setValueString(String valueString) {
		this.valueString = valueString;
	}

	public boolean isRoot() {
		return this._isRoot;
	}

	public int arity() {
		if (this.isRoot())
			return 1;
		return this._unit.arity();
	}

	public int getHIndex() {
		return this._hIndex;
	}

	public int getWIndex() {
		return this._wIndex;
	}

	public void setChildren(int index, SemanticForestNode[] child) {
		try {
			this._children[index] = child;
		} catch (Exception e) {
			System.err.println("xx:" + this.getUnit() + "\t" + child.length);
			for (int k = 0; k < child.length; k++) {
				System.err.println(k + ":" + child[k].getUnit());
			}
			throw new RuntimeException("x");
		}
	}

	public SemanticForestNode[][] getChildren() {
		return this._children;
	}

	// including itself
	public int countAllChildren() {
		if (this._children == null) {
			return 1;
		}
		int count = 0;
		for (SemanticForestNode[] child : this._children) {
			for (SemanticForestNode c : child) {
				count += c.countAllChildren();
			}
		}
		return count + 1;
	}

	// including itself
	public ArrayList<SemanticForestNode> getAllNodes() {
		if (this._allNodes != null) {
			return this._allNodes;
		}
		this._allNodes = new ArrayList<>();
		for (SemanticForestNode[] child : this._children) {
			for (SemanticForestNode c : child) {
				ArrayList<SemanticForestNode> subChildren = c.getAllNodes();
				for (SemanticForestNode subChild : subChildren) {
					int index = Collections.binarySearch(this._allNodes, subChild);
					if (index < 0) {
						this._allNodes.add(-1 - index, subChild);
					}
				}
			}
		}
		this._allNodes.add(this);
		return this._allNodes;
	}

	public SemanticUnit getUnit() {
		return this._unit;
	}

	public void setId(int id) {
		this._id = id;
	}

	@Override
	public int getId() {
		return this._id;
	}

	@Override
	public String getName() {
		return this._unit.toString() + "[" + this._hIndex + "," + this._wIndex + "]";
	}

	@Override
	public int compareTo(SemanticForestNode node) {
		if (this._hIndex != node._hIndex)
			return this._hIndex - node._hIndex;
		return this._wIndex - node._wIndex;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.toStringHelper_Simplify(-1, sb, 0);
		return sb.toString();
	}

	private void toStringHelper_Simplify(int childId, StringBuilder sb, int indent) {
		for (int k = 0; k < indent; k++)
			sb.append('\t');
		if (childId != -1) {
			sb.append("child-" + childId + ":");
		}
		if (this.isRoot()) {
			sb.append(this._align);
			sb.append('\n');
		} else {
			sb.append(this._unit.getMRL() + "\t" + this._align);
			sb.append('\n');
		}

		if (this._children != null) {
			for (int i = 0; i < this._children.length; i++) {
				SemanticForestNode[] child = this._children[i];
				for (int k = 0; k < indent; k++)
					sb.append('\t');
				sb.append("[");
				sb.append('\n');
				for (int k = 0; k < child.length; k++) {
					SemanticForestNode c = child[k];
					c.toStringHelper_Simplify(k, sb, indent + 1);
				}
				for (int k = 0; k < indent; k++)
					sb.append('\t');
				sb.append("]");
				sb.append('\n');
			}
		}
	}

	private void toStringHelper(int childId, StringBuilder sb, int indent) {
		for (int k = 0; k < indent; k++)
			sb.append('\t');
		if (childId != -1) {
			sb.append("child-" + childId + ":");
		}
		if (this.isRoot()) {
			sb.append("ROOT" + "\t#children:" + this.countAllChildren() + "[" + this.getHIndex() + ","
					+ this.getWIndex() + "]" + this.getHeight() + "\t" + Math.exp(this.getScore()) + "\t" + this._info);
			sb.append('\n');
		} else {
			sb.append(this._unit.getMRL() + "\t#children:" + this.countAllChildren() + "[" + this.getHIndex() + ","
					+ this.getWIndex() + "]" + this.getHeight() + "\t" + Math.exp(this.getScore()) + "\t" + this._info);
			sb.append('\n');
		}

		if (this._children != null) {
			for (int i = 0; i < this._children.length; i++) {
				SemanticForestNode[] child = this._children[i];
				for (int k = 0; k < indent; k++)
					sb.append('\t');
				sb.append("[");
				sb.append('\n');
				for (int k = 0; k < child.length; k++) {
					SemanticForestNode c = child[k];
					c.toStringHelper(k, sb, indent + 1);
				}
				for (int k = 0; k < indent; k++)
					sb.append('\t');
				sb.append("]");
				sb.append('\n');
			}
		}
	}

	public String toEquation() {
		StringBuilder sb = new StringBuilder();
		return this.toEquation_Helper(sb);
	}

	private String toEquation_Helper(StringBuilder sb) {
		if (this._children.length == 2) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			SemanticForestNode[] child1 = this._children[1];
			SemanticForestNode c1 = child1[0];
			return c0.toEquation_Helper(sb) + this.getValueString() + c1.toEquation_Helper(sb);
		} else if (this._children.length == 1) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			return c0.toEquation_Helper(sb);
		} else if (this._children.length == 0) {
			return this.getValueString();
		}
		return "";
	}

	public String toMathExpression() {
		StringBuilder sb = new StringBuilder();
		return this.toMathExpression_Helper(sb);
	}

	private String toMathExpression_Helper(StringBuilder sb) {
		if (this._children.length == 2) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			SemanticForestNode[] child1 = this._children[1];
			SemanticForestNode c1 = child1[0];
			return "(" + c0.toMathExpression_Helper(sb) + this.getValueString() + c1.toMathExpression_Helper(sb) + ")";
		} else if (this._children.length == 1) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			return c0.toMathExpression_Helper(sb);

		} else if (this._children.length == 0) {
			return this.getValueString();
		}
		return "";
	}

	public String toSpacedMathExpression() {
		StringBuilder sb = new StringBuilder();
		return this.toSpacedMathExpression_Helper(sb);
	}

	private String toSpacedMathExpression_Helper(StringBuilder sb) {
		if (this._children.length == 2) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			SemanticForestNode[] child1 = this._children[1];
			SemanticForestNode c1 = child1[0];
			return "( " + c0.toSpacedMathExpression_Helper(sb) + this.getValueString()
					+ c1.toSpacedMathExpression_Helper(sb) + " )";
		} else if (this._children.length == 1) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			return " " + c0.toSpacedMathExpression_Helper(sb) + " ";

		} else if (this._children.length == 0) {
			return " " + this.getValueString() + " ";
		}
		return " ";
	}

	public ArrayList<Double> getAllNumbers() {
		ArrayList<Double> numbers = new ArrayList<>();
		this.getAllNumbers_Helper(numbers);
		return numbers;
	}

	private void getAllNumbers_Helper(ArrayList<Double> numbers) {
		if (this._children.length == 2) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			SemanticForestNode[] child1 = this._children[1];
			SemanticForestNode c1 = child1[0];
			c0.getAllNumbers_Helper(numbers);
			c1.getAllNumbers_Helper(numbers);
		} else if (this._children.length == 1) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			c0.getAllNumbers_Helper(numbers);
		}
		if (!isRoot() && this._unit.getMRL().equals("NUM"))
			numbers.add(this.getNumValue());
	}

	public ArrayList<Variable> getAllVariables() {
		ArrayList<Variable> variables = new ArrayList<>();

		this.getAllVariables_Helper(variables);
		return variables;
	}

	private void getAllVariables_Helper(ArrayList<Variable> numbers) {
		if (this._children.length == 2) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			SemanticForestNode[] child1 = this._children[1];
			SemanticForestNode c1 = child1[0];
			c0.getAllVariables_Helper(numbers);
			c1.getAllVariables_Helper(numbers);
		} else if (this._children.length == 1) {
			SemanticForestNode[] child0 = this._children[0];
			SemanticForestNode c0 = child0[0];
			c0.getAllVariables_Helper(numbers);
		}
		if (!isRoot() && this._unit.getMRL().equals("VAR")) {
			int size = numbers.size() + 1;
			ArrayList<WordToken> wordTokens = getTokens();
			int start = wordTokens.get(0).getCharOffsetBegin();
			int end = wordTokens.get(0).getCharOffsetEnd() + 1;
			String id = "V" + size;
			StringBuilder sb = new StringBuilder(wordTokens.get(0).getName());
			for (int i = 1; i < wordTokens.size(); i++) {
				end = wordTokens.get(i).getCharOffsetEnd();
				sb.append(" " + wordTokens.get(i).getName());
			}
			Variable var = new Variable(id, start, end, sb.toString().trim());
			numbers.add(var);
		}
	}

	public ArrayList<WordToken> getTokens() {
		return tokens;
	}

	public void setTokens(ArrayList<WordToken> tokens) {
		this.tokens = tokens;
	}

}
