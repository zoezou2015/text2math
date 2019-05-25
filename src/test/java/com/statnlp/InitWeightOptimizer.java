/**
 * 
 */
package com.statnlp;

import com.statnlp.commons.ml.opt.LBFGS.ExceptionWithIflag;
import com.statnlp.commons.ml.opt.Optimizer;

/**
 * An optimizer that first initialize the weight according to specified value,
 * and then optimize using the given optimizer, or stops if there is no given optimizer.
 */
public class InitWeightOptimizer implements Optimizer {
	
	private Optimizer realOptimizer;
	private boolean firstCall;
	private double[] initialWeights;
	private double[] variables;
	private double objective;
	private double[] gradients;

	public InitWeightOptimizer(double[] initialWeights) {
		this(initialWeights, null);
	}
	
	public InitWeightOptimizer(double[] initialWeights, Optimizer realOptimizer){
		this.realOptimizer = realOptimizer;
		this.firstCall = true;
		this.initialWeights = initialWeights;
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.Optimizer#setObjective(double)
	 */
	@Override
	public void setObjective(double f) {
		if(realOptimizer == null){
			this.objective = f;
		} else {
			realOptimizer.setObjective(f);
		}
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.Optimizer#setVariables(double[])
	 */
	@Override
	public void setVariables(double[] x) {
		if(realOptimizer == null){
			this.variables = x;
		} else {
			realOptimizer.setVariables(x);
		}
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.Optimizer#setGradients(double[])
	 */
	@Override
	public void setGradients(double[] g) {
		if(realOptimizer == null){
			this.gradients = g;
		} else {
			realOptimizer.setGradients(g);
		}
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.Optimizer#getObjective()
	 */
	@Override
	public double getObjective() {
		if(realOptimizer == null){
			return this.objective;
		} else {
			return realOptimizer.getObjective();
		}
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.Optimizer#getVariables()
	 */
	@Override
	public double[] getVariables() {
		if(realOptimizer == null){
			return this.variables;
		} else {
			return realOptimizer.getVariables();
		}
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.Optimizer#getGradients()
	 */
	@Override
	public double[] getGradients() {
		if(realOptimizer == null){
			return this.gradients;
		} else {
			return realOptimizer.getGradients();
		}
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.Optimizer#optimize()
	 */
	@Override
	public boolean optimize() throws ExceptionWithIflag {
		if(this.firstCall){
			double[] variables = getVariables();
			for(int i=0; i<variables.length; i++){
				variables[i] = initialWeights[i];
			}
			this.firstCall = false;
			return false;
		} else {
			if(realOptimizer == null){
				return true;
			} else {
				return realOptimizer.optimize();
			}
		}
	}
	
	public String name(){
		if(realOptimizer == null){
			return "InitWeightOptimizer";
		} else {
			return realOptimizer.name() + " [+InitWeightOptimizer]";
		}
	}

}
