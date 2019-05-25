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
package com.statnlp.commons.ml.opt;

import com.statnlp.commons.ml.opt.LBFGS.ExceptionWithIflag;

public interface Optimizer {

	public void setObjective(double f);

	public void setVariables(double[] x);
	
	public void setGradients(double[] g);
	
	public double getObjective();
	
	public double[] getVariables();
	
	public double[] getGradients();
	
	public boolean optimize() throws ExceptionWithIflag;
	
	public String name();
}