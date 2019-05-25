package com.statnlp.hybridnetworks;

public class ValueIndexPair {
    public double val;
    public int[] bestListIdx;
		
    public ValueIndexPair(double val, int[] bestListIdx) {
	this.val = val;
	this.bestListIdx = bestListIdx;
    }

    public int compareTo(ValueIndexPair other) {
	if(val < other.val)
	    return -1;
	if(val > other.val)
	    return 1;
	return 0;
    }
		
}