package com.example.mathsolver;

public class MathSolverConfig {
	public static String dataDir = "data/mathsolver/";
	// public static String dataDir = "data/annotatedproblem/";

	// public static String textDir = "data/annotatedproblemtext/";
	public static String textDir = "data/problemtext/";
	public static String dataset = "";
	public static int _SEMANTIC_FOREST_MAX_DEPTH = 8; // 5
	public static int _SEMANTIC_PARSING_NGRAM = 2;// 2//1
	public static boolean USE_PREDICT_NUMBER = false;
	public static boolean USE_GOLD_NUMBER = false;
	public static boolean USE_POS_FEAT = true;
	public static int MAX_NUMBER_VARIABLE = 2;
	public static boolean checkValidPair = false;
	public static boolean USE_SUFFIX_X = false;
	public static boolean NO_X = false;
	public static boolean USE_LEXICON = true;
	public static boolean NUM_CONSTRAINT = true;
	public static boolean EQUAL_ROOT = false; // Only use "=" but no X
	public static boolean NO_REVERSE_OP = false;

	public static String[] roy_dataset = { "allArith", "allArithLex", "allArithTmpl", "aggregate", "aggregateLex",
			"aggregateTmpl", };

	// public static double tunedL2(String dataset) {
	// if (dataset.equals("addsub")) {
	// return 0.02;
	// } else if (dataset.equals("all")) {
	// return 0.03;
	// } else if (dataset.equals("addsub")) {
	// return 0.02;
	// } else {
	// throw new RuntimeException("Unknown dataset. You need to configure it
	// yourself!");
	// }
	// }
}
