package com.statnlp.neural;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;

public class NNCRFGlobalNetworkParam extends NNCRFInterface {
	
	private static final long serialVersionUID = 4984994803152483000L;

	private GlobalNetworkParam param_G;
	
	// "input" and "output" vocab
	private LinkedHashSet<String> inputSet = new LinkedHashSet<String>();
	private HashMap<Integer, String> idx2strOutput = new HashMap<Integer, String>();
	private ArrayList<HashMap<String, Integer>> fieldMapList = new ArrayList<HashMap<String, Integer>>();
	
	// maps the index in the flattened ``external'' weights from the NN to corresponding feature index
	private int[] externalWeightIndex;
		
	
	// reference to External Neural features
	private HashMap<String, HashMap<String, Integer>> neuralFeatureIntMap;
	
	// number of NN features
	private int _nnSize = 0;
	private int _usedNNSize = 0;
	
	// checks if i-th feature is a neural feature
	private boolean[] _isNNFeature;
	
	// prevent repeated new array allocations
	private double[] grads;
	private double[] notNNWeights, notNNCounts;

	public NNCRFGlobalNetworkParam(GlobalNetworkParam param_G) {
		super();
		this.param_G = param_G;
		this.neuralFeatureIntMap = param_G.getFeatureIntMap().get("neural");
	}
	
	@Override
	public void initializeInternalNeuralWeights() {
		// NN configuration
		
		List<Integer> numInputList = new ArrayList<Integer>();
		List<Integer> inputDimList = new ArrayList<Integer>();//Arrays.asList(idx2strInput.size());
		List<String> wordList = new ArrayList<String>();
		String lang = NeuralConfig.LANGUAGE;
		List<String> embList = NeuralConfig.EMBEDDING;
		List<Integer> embSizeList = NeuralConfig.EMBEDDING_SIZE;
		List<List<Integer>> vocab = makeVocab(numInputList, inputDimList, wordList);
		int outputDim = neuralFeatureIntMap.size();
		
		double[] nnInternalWeights = this.nn.initNetwork(numInputList, inputDimList, wordList, lang, embList, embSizeList, outputDim, vocab);
		if(nnInternalWeights != null) {
			_nnSize = nnInternalWeights.length;
		} else {
			_nnSize = 0;
		}
		_nnWeights = new double[_nnSize];
		_nnGrads = new double[_nnSize];
		setInternalNeuralWeights(nnInternalWeights);
	}
	
	public void setInternalNeuralWeights(double[] seed) {
		
		for(int k = 0; k<this._nnSize; k++) {
			this._nnWeights[k] = seed[k];
		}
//		if (NeuralConfig.NUM_LAYER > 0 || NeuralConfig.WORD_EMBEDDING_SIZE > 0) {
//			for(int k = 0; k<this._nnSize; k++) {
//				this._nnWeights[k] = seed[k];
//			}
//		} else { // trick for reproducing
//			for(int k = 0; k<this._nnSize; k++){
//				int weightIdx = internalWeightIndex[k];
//				if (weightIdx != -1) {
//					this._nnWeights[k] = seed[weightIdx];
//				} else {
//					this._nnWeights[k] = 0.0;
//				}
//			}
//		}
	}
	
	
	public void setInternalNeuralWeight(int f, double val){
		this._nnWeights[f] = val;
	}
	
	@Override
	public void updateExternalNeuralWeights(double[] weights) {
		for (int i = 0; i < externalWeightIndex.length; i++) {
			if (externalWeightIndex[i] != -1) {
				param_G.overRideWeight(externalWeightIndex[i], weights[i]);
			}
		}
	}

	@Override
	public double[] getExternalNeuralGradients() {
		double[] counts = param_G.getCounts();
		if (grads == null) {
			grads = new double[externalWeightIndex.length];
		}
		for (int i = 0; i < externalWeightIndex.length; i++) {
			int idx = externalWeightIndex[i];
			if (idx != -1) {
				grads[i] = counts[idx];
			} else {
				grads[i] = 0.0;
			}
		}
		return grads;
	}
	
	@Override
	public void setInternalNeuralGradients(double[] counts) {
		for (int i = 0; i < _nnSize; i++) {
			this._nnGrads[i] += counts[i];
		}
	}
	
	public int getNonNeuralAndInternalNeuralSize() {
		int size = param_G.countFeatures();
		int numNotNN = size - _usedNNSize;
		return numNotNN+_nnSize;
	}
	
	public void getNonNeuralAndInternalNeuralWeights(double[] concatWeights, double[] concatCounts) {
		int size = param_G.countFeatures();
		int numNotNN = size - _usedNNSize;
		if (notNNWeights == null) {
			notNNWeights = new double[numNotNN];
			notNNCounts = new double[numNotNN];
		}
		int j = 0;
		double[] weights = param_G.getWeights();
		double[] counts = param_G.getCounts();
		for (int i = 0; i < size; i++) {
			if (!isNNFeature(i)) {
				notNNWeights[j] = weights[i];
				notNNCounts[j] = counts[i];
				j++;
			}
		}
		concatArray(concatWeights, this.notNNWeights, this._nnWeights);
		concatArray(concatCounts, this.notNNCounts, this._nnGrads);
	}
	
	public void updateNonNeuralAndInternalNeuralWeights(double[] concatWeights) {
		unpackArray(concatWeights, this.notNNWeights, this._nnWeights);
		int size = param_G.countFeatures();
		int j = 0;
		for (int i = 0; i < size; i++) {
			if (!isNNFeature(i)) {
				param_G.setWeight(i, notNNWeights[j]);
				j++;
			}
		}
	}
	
	private List<List<Integer>> makeVocab(List<Integer> numInputList, List<Integer> inputDimList, List<String> wordList) {
		List<List<Integer>> vocab = new ArrayList<List<Integer>>();
		
		for (String output : neuralFeatureIntMap.keySet()) {
			idx2strOutput.put(idx2strOutput.size(), output);
			for (String input : neuralFeatureIntMap.get(output).keySet()) {
				String[] fields = input.split(NeuralConfig.OUT_SEP);
				if (fieldMapList.isEmpty()) {
					for(int i=0;i<fields.length;i++) {
						fieldMapList.add(new HashMap<String, Integer>());
						String[] elements = fields[i].split(NeuralConfig.IN_SEP);
						numInputList.add(elements.length);
						inputDimList.add(0);
					}
						
				}
				ArrayList<Integer> entry = new ArrayList<Integer>();
				for(int i=0;i<fields.length;i++) {
					String[] elements = fields[i].split(NeuralConfig.IN_SEP);
					HashMap<String, Integer> fieldMap = fieldMapList.get(i);
					for (int j=0;j<elements.length;j++) {
						if(!fieldMap.containsKey(elements[j])) {
							int fieldIdx = NetworkConfig.IS_INDEXED_NEURAL_FEATURES? Integer.parseInt(elements[j]):fieldMap.size();
							fieldMap.put(elements[j], fieldIdx);
							inputDimList.set(i, inputDimList.get(i)+1);
							if (NeuralConfig.EMBEDDING.get(i).equals("glove")
							|| NeuralConfig.EMBEDDING.get(i).equals("polyglot")) {
								wordList.add(elements[j]);
							}
						}
						entry.add(fieldMap.get(elements[j])+1); // 1-indexing
					}
				}
				if (!inputSet.contains(input)) {
					inputSet.add(input);
					vocab.add(entry);
				}
			}
		}
		externalWeightIndex = new int[idx2strOutput.size()*inputSet.size()];
		int i = 0;
		for (String input : inputSet) {
			for (int j = 0; j < idx2strOutput.size(); j++) {
				String output = idx2strOutput.get(j);
				Integer idx = neuralFeatureIntMap.get(output).get(input);
				if (idx != null) {
					externalWeightIndex[i*idx2strOutput.size()+j] = idx;
					setNNFeature(idx);
					_usedNNSize++;
				} else {
					externalWeightIndex[i*idx2strOutput.size()+j] = -1;
				}
			}
			i++;
		}
		return vocab;
	}
	
	public boolean isNNFeature(int f) {
		return _isNNFeature[f];
	}
	
	private synchronized void setNNFeature(int f){
		if (_isNNFeature == null) {
			_isNNFeature = new boolean[param_G.countFeatures()];
			Arrays.fill(_isNNFeature, false);
		}
		_isNNFeature[f] = true;
	}
	
	public synchronized void setNNCounts(double[] counts){
		for (int i = 0; i < counts.length; i++) {
			this._nnGrads[i] += counts[i];
		}
	}
	
	// helper functions
	private void unpackArray(double[] arr, double[] a, double[] b) {
		int m = a.length;
		int n = b.length;
		for (int i = 0; i < m; i++) a[i] = arr[i];
		for (int i = 0; i < n; i++) b[i] = arr[i+m];
	}
	
	private void concatArray(double[] ret, double[] a, double[] b) {
		for (int i = 0; i < a.length; i++) ret[i] = a[i];
		for (int i = 0; i < b.length; i++) ret[i+a.length] = b[i];
	}
}
