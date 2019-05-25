package com.statnlp.neural;

import java.util.Arrays;
import java.util.List;

public class NeuralConfig {
	public static int NEURAL_SERVER_PORT = 6666;
	public static String NEURAL_SERVER_PREFIX = "tcp://";
	public static String NEURAL_SERVER_ADDRESS = "172.18.240.32";
	
	public static String LANGUAGE = "en";
	public static List<String> EMBEDDING = (List<String>) Arrays.asList("none");
	public static List<Integer> EMBEDDING_SIZE = (List<Integer>) Arrays.asList(100);
	public static int NUM_LAYER = 0;
	public static int HIDDEN_SIZE = 100;
	public static String ACTIVATION = "tanh";
	public static double DROPOUT = 0;
	public static String OPTIMIZER = "sgd";
	public static double LEARNING_RATE = 0.001;
	public static boolean FIX_EMBEDDING = false;
	
	public static String OUT_SEP = "#OUT#";
	public static String IN_SEP = "#IN#";
}
