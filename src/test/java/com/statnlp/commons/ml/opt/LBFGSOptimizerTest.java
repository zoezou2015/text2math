package com.statnlp.commons.ml.opt;

import java.util.Arrays;

import com.statnlp.commons.ml.opt.LBFGS.ExceptionWithIflag;

public class LBFGSOptimizerTest {

    private static double square(double x){
    	return x * x;
    }
    
    //v : dim = 3
    private static double getObj(double[] v, double factor){
    	double numerator = Math.exp(v[0]) + Math.exp(v[1]);
    	double denominator = Math.exp(v[0]) + Math.exp(v[1]) + Math.exp(v[2]);
    	double result = numerator/denominator * factor;
    	return -result;
    }

    private static double getLogObj(double[] v){
    	double numerator1 = Math.exp(v[0]) + Math.exp(v[1]);
//    	double numerator2 = Math.exp(v[1]) + Math.exp(v[2]);
    	double denominator1 = Math.exp(v[0]) + Math.exp(v[1]) + Math.exp(v[2]);
//    	double denominator2 = Math.exp(v[0]) + Math.exp(v[1]) + Math.exp(v[2]);
    	double result = Math.log(numerator1) - Math.log(denominator1);
//    			+ Math.log(numerator2) - Math.log(denominator2);
    	return -(result);
    }
    
    private static double[] getGradient(double[] v, double factor){
    	double g[] = new double[v.length];
    	double denominator = Math.exp(v[0]) + Math.exp(v[1]) + Math.exp(v[2]);
    	g[0] = -Math.exp(v[0]+v[2])/Math.pow(denominator, 2) * factor;
    	g[1] = -Math.exp(v[1]+v[2])/Math.pow(denominator, 2) * factor;
    	g[2] = Math.exp(v[2])*(Math.exp(v[0]) + Math.exp(v[1]))/Math.pow(denominator, 2) * factor;
    	return g;
    }
    
    private static double[] getLogGradient(double[] v){
    	double g[] = new double[v.length];
    	double denominator = Math.exp(v[0]) + Math.exp(v[1]) + Math.exp(v[2]);
    	g[0] = -Math.exp(v[0]+v[2])/denominator/(Math.exp(v[0]) + Math.exp(v[1]));
    	g[1] = -Math.exp(v[1]+v[2])/denominator/(Math.exp(v[0]) + Math.exp(v[1]));
    	g[2] = Math.exp(v[2])/denominator;
    	return g;
    }
    

    private static double[] getGradient_alternative(double[] v){
    	double g[] = getGradient(v,1.0);
    	
    	double obj = getObj(v, 1.0);
    	double g_p[] = getLogGradient(v);
    	
    	for(int k = 0; k<g_p.length; k++){
    		g_p[k] *= obj;
    	}
    	
    	System.err.println(Arrays.toString(g));
    	System.err.println(Arrays.toString(g_p));
    	System.exit(1);
    	
    	return g_p;
    }
    
    private static double[] getGradient_approx(double[] v){
    	double g_p[] = getGradient(v,1);
    	
    	double g[] = new double[v.length];
    	double v1[] = (double[])v.clone();
    	double step = 0.001;
    	double f_diff;
    	
    	v1[0] += step;
    	f_diff = getObj(v1,1) - getObj(v,1);
    	g[0] = f_diff / step;
    	v1[0] -= step;
    	
    	v1[1] += step;
    	f_diff = getObj(v1,1) - getObj(v,1);
    	g[1] = f_diff / step;
    	v1[1] -= step;
    	
    	v1[2] += step;
    	f_diff = getObj(v1,1) - getObj(v,1);
    	g[2] = f_diff / step;
    	v1[2] -= step;
    	
    	System.err.println(Arrays.toString(g_p));
    	System.err.println(Arrays.toString(g));
    	System.exit(1);
    	
    	return g;
    }
    
    private static double[] getLogGradient_approx(double[] v){
    	double g_p[] = getLogGradient(v);
    	
    	double g[] = new double[v.length];
    	double v1[] = (double[])v.clone();
    	double step = 0.001;
    	double f_diff;
    	
    	v1[0] += step;
    	f_diff = getLogObj(v1) - getLogObj(v);
    	g[0] = f_diff / step;
    	v1[0] -= step;
    	
    	v1[1] += step;
    	f_diff = getLogObj(v1) - getLogObj(v);
    	g[1] = f_diff / step;
    	v1[1] -= step;
    	
    	v1[2] += step;
    	f_diff = getLogObj(v1) - getLogObj(v);
    	g[2] = f_diff / step;
    	v1[2] -= step;
    	
    	System.err.println(Arrays.toString(g_p));
    	System.err.println(Arrays.toString(g));
    	System.exit(1);
    	
    	return g;
    }
    
    public static void main(String args[]) throws ExceptionWithIflag{
    	
    	LBFGSOptimizer opt = new LBFGSOptimizer();
    	
//    	double lambda = Math.abs(Math.random()*10);
//    	System.err.println("lambda="+lambda);
    	
		double f; 
    	
    	double[] x;
    	
    	x = new double[]{(Math.random())*100, (-Math.random())*100, (Math.random())*100};
    	
    	getGradient_approx(x);
    	getLogGradient_approx(x);
    	getGradient_alternative(x);
    	
//    	x = new double[]{-100,-100,100};
    	x = new double[]{20, 10, 20};
    	System.err.println(x[0]+".."+x[1]+".."+x[2]);
    	opt.setVariables(x);
    	
    	while(true){
        	f = getLogObj(x);
        	double[] g = getLogGradient(x);
        	opt.setObjective(f);
        	opt.setGradients(g);
        	boolean done = opt.optimize();
        	if(done) break;
        	System.err.println("Log Obj="+getLogObj(x));
        	System.err.println("g:"+Arrays.toString(g));
        	System.err.println("x:"+Arrays.toString(x));
    	}
    	
    	double obj_old = getLogObj(x);
    	double old_x[] = x.clone();
    	
    	double factor = 1/getLogObj(x);
    	
    	if(Double.isInfinite(factor)){
    		System.err.println("DONE. REACHED GLOBAL MIN");
    		System.exit(1);
    	}
    	
    	System.err.println("FACTOR="+factor);
    	System.err.println("===========");
    	System.err.println("===========");
    	System.err.println("===========");
    	System.err.println("===========");
    	
//    	double factor = 1E10;
    	
    	while(true){
        	f = getObj(x, factor);
        	double[] g = getGradient(x, factor);
        	System.err.println("Obj="+getObj(x, factor));
        	System.err.println("g:"+Arrays.toString(g));
        	System.err.println("x:"+Arrays.toString(x));
        	opt.setObjective(f);
        	opt.setGradients(g);
        	boolean done = opt.optimize();
        	if(done) break;
        	System.err.println("g:"+Arrays.toString(g));
        	System.err.println("x:"+Arrays.toString(x));
    	}
    	
    	System.err.println("+++++++++++");
    	System.err.println("+++++++++++");
    	System.err.println("+++++++++++");
    	System.err.println("+++++++++++");
    	
    	while(true){
        	f = getLogObj(x);
        	double[] g = getLogGradient(x);
        	opt.setObjective(f);
        	opt.setGradients(g);
        	boolean done = opt.optimize();
        	if(done) break;
        	System.err.println("Log Obj="+getLogObj(x));
        	System.err.println("g:"+Arrays.toString(g));
        	System.err.println("x:"+Arrays.toString(x));
    	}
    	
    	double[] g = getGradient(x, factor);
    	System.err.println("g:"+Arrays.toString(g));
    	System.err.println("x:\t"+Arrays.toString(x));
    	System.err.println("old x:\t"+Arrays.toString(old_x));
    	
    	System.err.println("Obj="+getObj(x, factor));
    	System.err.println("Log Obj="+getLogObj(x));
    	System.err.println("Old Obj="+obj_old);
    }
	
    public static void main2(String args[]) throws ExceptionWithIflag{
    	
    	LBFGSOptimizer opt = new LBFGSOptimizer();
    	
    	double lambda = Math.abs(Math.random()*10);
    	System.err.println("lambda="+lambda);
    	
		double v; 
    	double f; 
    	
    	double[] x;
    	
    	double A = (Math.random()-.5);
    	double B = (Math.random()-.5);
    	
    	x = new double[]{(Math.random()-.5)*100, (Math.random()-.5)*100};
    	System.err.println(x[0]+".."+x[1]);
    	opt.setVariables(x);
    	
    	while(true){
    		v = Math.exp(A*x[0]+B*x[1]);
        	f = -1.0/(1+v)+lambda*(square(x[0])+square(x[1])+square(x[0]-x[1]))-100*(x[0]);
        	double[] g = {A*v/square(1+v)+2*lambda*x[0]+2*lambda*(x[0]-x[1])-100,B*v/square(1+v)+2*lambda*x[1]+2*lambda*(x[1]-x[0])};
        	opt.setObjective(f);
        	opt.setGradients(g);
        	boolean done = opt.optimize();
        	if(done) break;
    	}
    	
		v = Math.exp(A*x[0]+B*x[1]);
    	double f1 = -1.0/(1+v)+lambda*(square(x[0])+square(x[1])+square(x[0]-x[1]));
    	
    	for(int i = 0; i<x.length; i++)
    		System.err.println("x["+i+"]="+x[i]);
    	
    	double a = x[0];
    	double b = x[1];
    	System.err.println("f1="+f1);
    	
    	lambda = lambda*3;
    	
    	x = new double[]{(Math.random()-.5)*100, (Math.random()-.5)*100, (Math.random()-.5)*100};
    	System.err.println(x[0]+".."+x[1]+".."+x[2]);
    	opt.setVariables(x);
    	
    	while(true){
    		double x0p = x[0]+x[2];
    		double x1p = x[1]+x[2];
    		v = Math.exp(A*x0p+B*x1p);
        	f = -1.0/(1+v)+lambda*(square(x[0])+square(x[1])+square(x[2]))-100*x[0]-100*x[2];
        	double[] g = {A*v/square(1+v)+2*lambda*x[0]-100,B*v/square(1+v)+2*lambda*x[1],(A+B)*v/square(1+v)+2*lambda*x[2]-100};
        	opt.setObjective(f);
        	opt.setGradients(g);
        	boolean done = opt.optimize();
        	if(done) break;
    	}
    	
		double x0p = x[0]+x[2];
		double x1p = x[1]+x[2];
		v = Math.exp(A*x0p+B*x1p);
    	double f2 = -1.0/(1+v)+lambda*(square(x[0])+square(x[1])+square(x[2]));
    	
    	for(int i = 0; i<x.length; i++){
    		System.err.println("x["+i+"]="+x[i]);
    	}
    	System.err.println("f2="+f2);
    	
    	System.err.println("DIFF:");
    	System.err.println(x0p-a);
    	System.err.println(x1p-b);
    	System.err.println(x[0]+x[1]-x[2]);
    	System.err.println(f2-f1);
    	
    }

}
