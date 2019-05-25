package com.statnlp.commons.types;

import java.util.HashMap;
import java.util.Iterator;

public class AttWordToken extends WordToken{
	
	private static final long serialVersionUID = -1492374100364145797L;
	
	//the attributes associated with the word token.
	private HashMap<String, String> _attMap;
	
	public AttWordToken(String name) {
		super(name);
		this._attMap = new HashMap<String, String>();
	}
	
	public void addAtt(String att, String value){
		this._attMap.put(att, value);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append("AttWord:");
		sb.append(this._name);
		Iterator<String> atts = this._attMap.keySet().iterator();
		while(atts.hasNext()){
			String att = atts.next();
			String value = this._attMap.get(att);
			sb.append(' ');
			sb.append(att);
			sb.append(':');
			sb.append(value);
		}
		sb.append(']');
		return sb.toString();
	}
	
}
