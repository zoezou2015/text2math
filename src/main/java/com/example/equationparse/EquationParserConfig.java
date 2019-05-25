package com.example.equationparse;

public class EquationParserConfig {
	public static String dataDir = "data/equationparsebrat/";
	// public static String dataDir = "data/annotatedproblem/";

	// public static String textDir = "data/annotatedproblemtext/";
	public static String textDir = "data/problemtext/";

	public static String featureDir = dataDir + "feature/";
	public static int _SEMANTIC_FOREST_MAX_DEPTH = 6;
	public static int _SEMANTIC_PARSING_NGRAM = 2;// 2//1
	public static boolean USE_PREDICT_NUMBER = false;
	public static boolean USE_GOLD_NUMBER = false;
	public static boolean USE_POS_FEAT = true;
	public static int MAX_NUMBER_VARIABLE = 2;
	public static boolean checkValidPair = true;
}
