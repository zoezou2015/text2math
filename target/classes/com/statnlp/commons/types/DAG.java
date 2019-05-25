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
/**
 * 
 */
package com.statnlp.commons.types;

import java.io.Serializable;

/**
 * @author wei_lu
 *
 */
public interface DAG extends Serializable{
	
	//hIndex -- the height of the node in the DAG.
	//wIndex -- the width id of the node in the DAG.
	public Token get(int hIndex, int wIndex);
	
	//get the node by id, the nodes in the DAG are sorted in topological order.
	public Token get(int id);
	
}