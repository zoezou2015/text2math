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


import java.util.Arrays;

import com.statnlp.commons.ml.opt.LBFGS.ExceptionWithIflag;

/**
 * The gradient descent optimizer.<br>
 * This includes all variants of the vanilla gradient descent, such as:
 * <ol>
 * <li>AdaGrad (http://www.jmlr.org/papers/volume12/duchi11a/duchi11a.pdf)</li>
 * <li>AdaDelta (http://www.matthewzeiler.com/pubs/googleTR2012/googleTR2012.pdf)</li>
 * <li>RMSProp (http://cs231n.github.io/neural-networks-3/#anneal)</li>
 * <li>AdaM (http://cs231n.github.io/neural-networks-3/#anneal)</li>
 * </ol>
 * @author Aldrian Obaja <aldrianobaja.m@gmail.com>
 *
 */
public class GradientDescentOptimizer implements Optimizer{
	
	public static final int DEFAULT_MAX_STAGNANT_ITER_COUNT = 40;
	
	private double learningRate;
	private double[] _x;
	private double[] _g;
	private double _obj;
	private double prevGradients[];
	private double prevSqGradients[];
	private double prevDelta[];
	
	// Store the best values so far
	private double bestObj;
	private int bestIterNum;
	private double bestX[];
	private double bestGradients[];
	private double bestSqGradients[];
	private double bestDelta[];
	
	/**
	 * The number of iterations without improvement before the trigger is activated.<br>
	 * A trigger can be a decaying parameter or change of adaptive method
	 */
	public static int maxStagnantIterCount = DEFAULT_MAX_STAGNANT_ITER_COUNT;
	
	/**
	 * The strategy for adaptive gradient descent.<br>
	 * In a strategy there might be more than one standard adaptive methods (AdaGrad, AdaDelta, etc.) 
	 * @author Aldrian Obaja <aldrianobaja.m@gmail.com>
	 *
	 */
	public static enum AdaptiveStrategy {
		NONE,
		ADAGRAD,
		ADADELTA,
		ADADELTA_DECAYING,
		ADADELTA_THEN_ADAGRAD,
		ADADELTA_THEN_GD,
		ADADELTA_THEN_STOP,
		RMSPROP,
		ADAM,
	}
	
	/**
	 * The list of supported adaptive method<br>
	 * These are the standard adaptive methods
	 * @author Aldrian Obaja <aldrianobaja.m@gmail.com>
	 *
	 */
	public static enum AdaptiveMethod {
		NONE,
		ADAGRAD,
		ADADELTA,
		RMSPROP,
		ADAM,
	}
	
	private AdaptiveStrategy adaptiveStrategy;
	private AdaptiveMethod currentAdaptiveMethod;
	private double adadeltaPhi;
	private double adadeltaEps;
	private double adadeltaGradDecay;
	
	private double rmsPropDecay;
	private double rmsPropEps;
	
	private double adamBeta1;
	private double adamBeta2;
	private double adamEps;
	
	private int iterNum;
	
	public GradientDescentOptimizer(AdaptiveStrategy adaptiveStrategy, double learningRate, double adadeltaPhi, double adadeltaEps, double adadeltaGradDecay, double rmsPropDecay, double rmsPropEps, double adamBeta1, double adamBeta2, double adamEps, int weightLength){
		this.prevGradients = new double[weightLength];
		this.prevSqGradients = new double[weightLength];
		this.prevDelta = new double[weightLength];
		
		this.adaptiveStrategy = adaptiveStrategy;
		
		this.learningRate = learningRate;
		
		this.adadeltaPhi = adadeltaPhi;
		this.adadeltaEps = adadeltaEps;
		this.adadeltaGradDecay = adadeltaGradDecay;
		
		this.rmsPropDecay = rmsPropDecay;
		this.rmsPropEps = rmsPropEps;
		
		this.adamBeta1 = adamBeta1;
		this.adamBeta2 = adamBeta2;
		this.adamEps = adamEps;
		
		this.iterNum = 0;
		
		switch(this.adaptiveStrategy){
		case NONE:
			currentAdaptiveMethod = AdaptiveMethod.NONE;
			break;
		case ADAGRAD:
			currentAdaptiveMethod = AdaptiveMethod.ADAGRAD;
			break;
		case ADADELTA:
		case ADADELTA_DECAYING:
		case ADADELTA_THEN_ADAGRAD:
		case ADADELTA_THEN_GD:
		case ADADELTA_THEN_STOP:
			currentAdaptiveMethod = AdaptiveMethod.ADADELTA;
			break;
		case RMSPROP:
			currentAdaptiveMethod = AdaptiveMethod.RMSPROP;
			break;
		case ADAM:
			currentAdaptiveMethod = AdaptiveMethod.ADAM;
			break;
		}
		
		bestX = new double[weightLength];
		bestGradients = new double[weightLength];
		bestSqGradients = new double[weightLength];
		bestDelta = new double[weightLength];
		bestObj = Double.MAX_VALUE;
		reset();
	}
	
	public void reset(){
		this.iterNum = 0;
		this.bestIterNum = 0;
		this.bestObj = Double.MAX_VALUE;
		Arrays.fill(this.prevGradients, 0.0);
		Arrays.fill(this.prevSqGradients, 0.0);
		Arrays.fill(this.prevDelta, 0.0);
		Arrays.fill(this.bestX, 0.0);
		Arrays.fill(this.bestGradients, 0.0);
		Arrays.fill(this.bestSqGradients, 0.0);
		Arrays.fill(this.bestDelta, 0.0);
	}
	
	public double getLearningRate(){
		return this.learningRate;
	}
	
	@Override
	public void setVariables(double[] x){
		this._x = x;
	}
	
	@Override
	public void setObjective(double obj){
		this._obj = obj;
	}
	
	@Override
	public void setGradients(double[] g){
		this._g = g;
	}

	@Override
	public double getObjective() {
		return _obj;
	}

	@Override
	public double[] getVariables() {
		return _x;
	}

	@Override
	public double[] getGradients() {
		return _g;
	}
	
	public boolean optimize() throws ExceptionWithIflag{
//		double sum = 0.0;
//		for(int k=0; k<this._x.length; k++){
//			sum += Math.pow(this._g[k], 2);
//		}
//		sum = Math.sqrt(sum);
//		System.err.println("L2norm: "+sum);
		
		boolean currentIsBest = checkAndSetAndIsBest();
		if(!currentIsBest && (this.iterNum - this.bestIterNum >= maxStagnantIterCount)){
			if(adaptiveStrategy == AdaptiveStrategy.ADADELTA_THEN_GD && currentAdaptiveMethod != AdaptiveMethod.NONE){
				copyBest();
				
				// First time change, configure learning rate from AdaDelta
				for(int k=0; k<this._x.length; k++){
					this.learningRate = Math.min(this.learningRate, Math.sqrt((adadeltaEps+prevSqGradients[k])/(adadeltaEps+prevDelta[k])));
				}
				currentAdaptiveMethod = AdaptiveMethod.NONE;
				System.out.println("[AdaDelta]Change to gradient descent with learning rate = "+this.learningRate);
			} else if(adaptiveStrategy == AdaptiveStrategy.ADADELTA_THEN_ADAGRAD && currentAdaptiveMethod != AdaptiveMethod.ADAGRAD){
				copyBest();
				
				// First time change, configure learning rate from AdaDelta
				for(int k=0; k<this._x.length; k++){
					prevSqGradients[k] = this.learningRate * (adadeltaEps+prevSqGradients[k])/(adadeltaEps+prevDelta[k]);
				}
				currentAdaptiveMethod = AdaptiveMethod.ADAGRAD;
				System.err.println("[AdaDelta]Change to AdaGrad with learning rate = "+this.learningRate);
			} else if(adaptiveStrategy == AdaptiveStrategy.ADADELTA_DECAYING){
				copyBest();
				
				adadeltaEps /= 2;
				System.err.println("[AdaDelta]Reset from obj = "+this._obj+", new eps = "+adadeltaEps);
			} else if(adaptiveStrategy == AdaptiveStrategy.ADADELTA_THEN_STOP){
				copyBest();
				return true;
			}
		}
//		clipGradients();
		for(int k = 0; k<this._x.length; k++){
			if(currentAdaptiveMethod == AdaptiveMethod.NONE){ // Normal (S)GD
				this._x[k] -= this.learningRate * this._g[k];
				
			} else if(currentAdaptiveMethod == AdaptiveMethod.ADAGRAD) { // based on http://www.jmlr.org/papers/volume12/duchi11a/duchi11a.pdf
				prevSqGradients[k] += Math.pow(this._g[k], 2);
				double updateCoef = this.learningRate;
				if(prevSqGradients[k]!=0.0){
					updateCoef /= Math.sqrt(prevSqGradients[k]);
				}
				this._x[k] -= updateCoef * this._g[k];
				
			} else if (currentAdaptiveMethod == AdaptiveMethod.ADADELTA){ // based on http://www.matthewzeiler.com/pubs/googleTR2012/googleTR2012.pdf
				prevGradients[k] = adadeltaGradDecay*prevGradients[k] + (1-adadeltaGradDecay)*this._g[k]; // An attempt to reduce high jumps
				prevSqGradients[k] = adadeltaPhi*prevSqGradients[k] + (1-adadeltaPhi)*Math.pow(this._g[k], 2);
				double update = Math.sqrt(prevDelta[k]+adadeltaEps)/Math.sqrt(prevSqGradients[k]+adadeltaEps) * prevGradients[k];
				prevDelta[k] = adadeltaPhi*prevDelta[k] + (1-adadeltaPhi)*Math.pow(update, 2);
				this._x[k] -= update;
				
			} else if (currentAdaptiveMethod == AdaptiveMethod.RMSPROP){ // based on http://cs231n.github.io/neural-networks-3/#anneal
				prevSqGradients[k] = rmsPropDecay * prevSqGradients[k] + (1-rmsPropDecay) * Math.pow(this._g[k], 2);
				this._x[k] -= (this.learningRate / (Math.sqrt(prevSqGradients[k]) + rmsPropEps)) * this._g[k];
				
			} else if (currentAdaptiveMethod == AdaptiveMethod.ADAM){ // based on http://cs231n.github.io/neural-networks-3/#anneal
				prevGradients[k] = adamBeta1 * prevGradients[k] + (1-adamBeta1)*this._g[k];
				prevSqGradients[k] = adamBeta2 * prevSqGradients[k] + (1-adamBeta2) * Math.pow(this._g[k], 2);
				// bias correction
				double biasCorrection1 = 1 - Math.pow(adamBeta1, this.iterNum+1);
				double biasCorrection2 = 1 - Math.pow(adamBeta2, this.iterNum+1);
				double correctedLearningRate = this.learningRate*Math.sqrt(biasCorrection2)/biasCorrection1;
				this._x[k] -= (correctedLearningRate / (Math.sqrt(prevSqGradients[k]) + adamEps)) * prevGradients[k];
			}
		}
		this.iterNum += 1; 
		return false;
	}
	
//	private void clipGradients(){
//		double sum = 0.0;
//		for(int k=0; k<this._x.length; k++){
//			sum += Math.pow(this._g[k], 2);
//		}
//		sum = Math.sqrt(sum);
//		sum = sum/Math.min(30, sum);
//		for(int k=0; k<this._x.length; k++){
//			this._g[k] /= sum;
//		}
//	}
	
	/**
	 * Check whether current objective value is the best, and if it is, set as the current best.<br>
	 * Then return whether the current objective value is the best
	 * @return
	 */
	private boolean checkAndSetAndIsBest(){
		if(this._obj < this.bestObj){
			this.bestObj = this._obj;
			for(int k=0; k<this._x.length; k++){
				this.bestX[k] = this._x[k];
				this.bestDelta[k] = this.prevDelta[k];
				this.bestGradients[k] = this.prevGradients[k];
				this.bestSqGradients[k] = this.prevSqGradients[k];
			}
			this.bestIterNum = this.iterNum;
			return true;
		}
		return false;
	}
	
	/**
	 * Copy the parameters related to the best objective value to current parameters,
	 * except the iteration number, which stays the same (and update the iteration number of the best
	 * objective value as the current iteration)
	 */
	private void copyBest(){
		this._obj = this.bestObj;
		for(int k=0; k<this._x.length; k++){
			this._x[k] = this.bestX[k];
			this.prevDelta[k] = this.bestDelta[k];
			this.prevGradients[k] = this.bestGradients[k];
			this.prevSqGradients[k] = this.bestSqGradients[k];
			this.bestIterNum = this.iterNum;
		}
	}
	
	public String name(){
		return "Gradient Descent Optimizer with adaptive strategy: "+this.adaptiveStrategy.name();
	}
}
