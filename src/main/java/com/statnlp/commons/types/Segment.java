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

/**
 * Defines a segment described by begin index and end index.
 * @author Lu Wei <luwei@statnlp.com>
 *
 */
public class Segment implements Serializable, Comparable<Segment>{
	
	private static final long serialVersionUID = 7669804479975787795L;
	private int _bIndex;
	private int _eIndex;
	
	public Segment(int bIndex, int eIndex){
		this._bIndex = bIndex;
		this._eIndex = eIndex;
	}
	
	/**
	 * Returns the begin index of this segment
	 * @return
	 */
	public int getBIndex(){
		return this._bIndex;
	}
	
	/**
	 * Returns the end index of this segment
	 * @return
	 */
	public int getEIndex(){
		return this._eIndex;
	}
	
	/**
	 * Returns the length of this segment
	 * @return
	 */
	public int length(){
		return this._eIndex - this._bIndex;
	}
	
	/**
	 * Returns whether this segment is completely contained within another segment, or
	 * whether this segment completely contains another segment.
	 * @param seg
	 * @return
	 */
	public boolean nestedWith(Segment seg){
		if(this._bIndex >= seg._bIndex && this._eIndex <= seg._eIndex){
			return true;
		}
		if(this._bIndex <= seg._bIndex && this._eIndex >= seg._eIndex){
			return true;
		}
		return false;
	}
	
	/**
	 * Returns whether this segment overlaps with the specified segment.
	 * @param seg
	 * @return
	 */
	public boolean overlapsWith(Segment seg){
		if(this._bIndex<=seg._bIndex && this._eIndex>seg._bIndex){
			return true;
		}
		if(this._bIndex<seg._eIndex && this._eIndex>=seg._eIndex){
			return true;
		}
		if(seg._bIndex<=this._bIndex && seg._eIndex>this._bIndex){
			return true;
		}
		if(seg._bIndex<this._eIndex && seg._eIndex>=this._eIndex){
			return true;
		}
		return false;
	}
	
	/**
	 * Returns whether this segment is completely disjoint with the specified segment.
	 * @param seg
	 * @return
	 */
	public boolean noOverlapWith(Segment seg){
		if(this._eIndex<=seg._bIndex){
			return true;
		}
		if(seg._eIndex<=this._bIndex){
			return true;
		}
		return false;
	}
	
	@Override
	public int compareTo(Segment seg) {
		if(this._bIndex!=seg._bIndex)
			return this._bIndex - seg._bIndex;
		if(this._eIndex!=seg._eIndex)
			return this._eIndex - seg._eIndex;
		return 0;
	}
	
	@Override
	public int hashCode(){
		return (this._bIndex + 7) ^ (this._eIndex + 7);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Segment){
			Segment seg = (Segment)o;
			return this._bIndex == seg._bIndex && this._eIndex == seg._eIndex;
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "["+this._bIndex+","+this._eIndex+")";
	}

}