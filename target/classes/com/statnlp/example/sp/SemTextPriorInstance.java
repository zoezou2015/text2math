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

import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

/**
 * @author wei_lu
 *
 */
public class SemTextPriorInstance extends SemTextInstance{
	
	private static final long serialVersionUID = -693367615199855342L;
	
	private int _cept_id;
	
	public SemTextPriorInstance(int instanceId, double weight, int cept_id, WordToken word) {
		super(instanceId, weight, new Sentence(new WordToken[]{word}), null, null);
		this._cept_id = cept_id;
	}
	
	public int getCeptId(){
		return this._cept_id;
	}
	
}
