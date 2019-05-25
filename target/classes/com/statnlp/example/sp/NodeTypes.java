package com.statnlp.example.sp;

public enum NodeTypes {
	NUM(0), X(0), VAR(0), EQUAL_ROOT(1), ADD(2), SUB(2), SUB_R(2), MUL(2), DVI(2), DVI_R(2), EQU(2);

	int arity = 2;

	NodeTypes(int arity) {
		this.arity = arity;
	}

	public int getArity() {
		return this.arity;
	}
}
