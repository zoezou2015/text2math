package com.statnlp.hybridnetworks;

public class BinaryHeap { 
    private int DEFAULT_CAPACITY; 
    private int currentSize; 
    private ValueIndexPair[] theArray;
  
    public BinaryHeap(int def_cap) {
	DEFAULT_CAPACITY = def_cap;
	theArray = new ValueIndexPair[DEFAULT_CAPACITY+1]; 
	// theArray[0] serves as dummy parent for root (who is at 1) 
	// "largest" is guaranteed to be larger than all keys in heap
	theArray[0] = new ValueIndexPair(Double.POSITIVE_INFINITY,new int[]{-1,-1});          
	currentSize = 0; 
    } 
  
    public ValueIndexPair getMax() { 
	return theArray[1]; 
    }
  
    private int parent(int i) { return i / 2; } 
    private int leftChild(int i) { return 2 * i; } 
    private int rightChild(int i) { return 2 * i + 1; } 
  
    public void add(ValueIndexPair e) { 
   
		// bubble up: 
		int where = currentSize + 1; // new last place 
		while ( e.compareTo(theArray[parent(where)]) > 0 ){ 
		    theArray[where] = theArray[parent(where)]; 
		    where = parent(where); 
		} 
		theArray[where] = e; currentSize++;
    }
 
    public ValueIndexPair removeMax() {
		ValueIndexPair min = theArray[1];
		theArray[1] = theArray[currentSize];
		currentSize--;
		boolean switched = true;
		// bubble down
		for ( int parent = 1; switched && parent < currentSize; ) {
		    switched = false;
		    int leftChild = leftChild(parent);
		    int rightChild = rightChild(parent);
	
		    if(leftChild <= currentSize) {
			// if there is a right child, see if we should bubble down there
			int largerChild = leftChild;
			if ((rightChild <= currentSize) && 
			    (theArray[rightChild].compareTo(theArray[leftChild])) > 0){
			    largerChild = rightChild; 
			}
			if (theArray[largerChild].compareTo(theArray[parent]) > 0) {      
			    ValueIndexPair temp = theArray[largerChild];
			    theArray[largerChild] = theArray[parent];
			    theArray[parent] = temp;
			    parent = largerChild;
			    switched = true;
			}
		    }
		} 
		return min;
    }
 
}