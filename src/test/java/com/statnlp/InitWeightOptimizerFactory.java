/**
 * 
 */
package com.statnlp;

import java.util.HashMap;

import com.statnlp.commons.ml.opt.Optimizer;
import com.statnlp.commons.ml.opt.OptimizerFactory;

/**
 * 
 */
public class InitWeightOptimizerFactory extends OptimizerFactory {
	
	private static final long serialVersionUID = 462325492055929006L;
	private OptimizerFactory realOptimizerFactory;
	private HashMap<String, HashMap<String, HashMap<String, Double>>> featureWeightMap;
	
	public InitWeightOptimizerFactory(HashMap<String, HashMap<String, HashMap<String, Double>>> featureWeightMap){
		this(featureWeightMap, null);
	}
	
	public InitWeightOptimizerFactory(HashMap<String, HashMap<String, HashMap<String, Double>>> featureWeightMap, OptimizerFactory realOptimizer){
		super();
		this.featureWeightMap = featureWeightMap;
		this.realOptimizerFactory = realOptimizer;
	}

	/* (non-Javadoc)
	 * @see com.statnlp.commons.ml.opt.OptimizerFactory#create(int)
	 */
	@Override
	public Optimizer create(int numWeights) {
		throw new IllegalArgumentException();
	}
	
	@Override
	public Optimizer create(int numWeights, HashMap<String, HashMap<String, HashMap<String, Integer>>> featureIntMap){
		double[] initialWeights = new double[numWeights];
		for(String type: featureIntMap.keySet()){
			HashMap<String, HashMap<String, Integer>> outputToInputInt = featureIntMap.get(type);
			HashMap<String, HashMap<String, Double>> outputToInputWeight = featureWeightMap.get(type);
			for(String output: outputToInputInt.keySet()){
				HashMap<String, Integer> inputToInt = outputToInputInt.get(output);
				HashMap<String, Double> inputToWeight = outputToInputWeight.get(output);
				for(String input: inputToInt.keySet()){
					initialWeights[inputToInt.get(input)] = inputToWeight.get(input);
				}
			}
		}
		if(realOptimizerFactory == null){
			return new InitWeightOptimizer(initialWeights);
		} else {
			return new InitWeightOptimizer(initialWeights, realOptimizerFactory.create(numWeights));
		}
	}

}
