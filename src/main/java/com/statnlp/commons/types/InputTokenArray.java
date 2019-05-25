package com.statnlp.commons.types;

import java.io.Serializable;
import java.util.Arrays;

public class InputTokenArray implements Serializable{
	
	private static final long serialVersionUID = 8181520592866135065L;
	
	protected InputToken[] _tokens;
	
	public InputTokenArray(InputToken[] tokens){
		this._tokens = tokens;
	}
	
	public InputToken[] getTokens(){
		return this._tokens;
	}
	
	public int size(){
		return this._tokens.length;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof InputTokenArray){
			InputTokenArray a = (InputTokenArray)o;
			return Arrays.equals(this._tokens, a._tokens);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(this._tokens);
	}
	
	@Override
	public String toString(){
		return "InputTokenArray:"+Arrays.toString(this._tokens);
	}
	
}