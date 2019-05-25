package com.statnlp.hybridnetworks;

import java.util.Arrays;

public class IndexPair {

	int[] indices;
	
	public IndexPair(int[] indices){
		this.indices = indices;
	}

	public void set(int ith, int index){
		this.indices[ith] = index;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(indices);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexPair other = (IndexPair) obj;
		if (!Arrays.equals(indices, other.indices))
			return false;
		return true;
	}
	
	
}
