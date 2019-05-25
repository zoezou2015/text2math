package com.statnlp.neural;

import java.io.IOException;
import java.util.List;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.FloatValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.Value;
import org.zeromq.ZMQ;

public class RemoteNN {
	private boolean DEBUG = false;
	
	// Torch NN server information
	private ZMQ.Context context;
	private ZMQ.Socket requester;
	private String serverAddress = NeuralConfig.NEURAL_SERVER_PREFIX + NeuralConfig.NEURAL_SERVER_ADDRESS+":" + NeuralConfig.NEURAL_SERVER_PORT;
	
	// Reference to controller instance for updating weights and getting gradients
	private NNCRFInterface controller;
	
	// whether to use CRF's optimizer to optimize internal neural parameters
	private boolean optimizeNeural;
	
	public RemoteNN() {
		this(false);
	}
	
	public RemoteNN(boolean optimizeNeural) {
		context = ZMQ.context(1);
		requester = context.socket(ZMQ.REQ);
		requester.connect(serverAddress);
		this.optimizeNeural = optimizeNeural;
	}
	
	public void setController(NNCRFInterface controller) {
		this.controller = controller;
	}
	
	@SuppressWarnings("rawtypes")
	private void packList(MessageBufferPacker packer, String key, List arr){
		try {
			if(key!=null) packer.packString(key);
			packer.packArrayHeader(arr.size());
			for(Object a: arr){
				if(a instanceof Integer){
					int x = (Integer)a;
					packer.packInt(x);
				}else if(a instanceof String){
					String x = (String)a;
					packer.packString(x);
				}else if(a instanceof List){
					List x = (List)a;;
					packList(packer, null, x);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private double unpackDoubleOrInt(MessageUnpacker unpacker) throws IOException {
		Value v = unpacker.unpackValue();
		double x = 0.0;
		switch (v.getValueType()) {
		case FLOAT:
			FloatValue fv = v.asFloatValue();
			x = fv.toDouble();
			break;
		case INTEGER:
			IntegerValue iv = v.asIntegerValue();
			x = iv.toDouble();
			break;
		default: break;
		}
		return x;
	}
	
	public double[] initNetwork(List<Integer> numInputList, List<Integer> inputDimList, List<String> wordList,
						   String lang, List<String> embeddingList, List<Integer> embSizeList,
						   int outputDim, List<List<Integer>> vocab) {
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		try {
			packer.packMapHeader(16);
			packer.packString("cmd").packString("init");
			
			packList(packer, "numInputList", numInputList);
			packList(packer, "inputDimList", inputDimList);
			packList(packer, "wordList", wordList);
			packer.packString("lang").packString(lang);
			packList(packer, "embedding", embeddingList);
			packList(packer, "embSizeList", embSizeList);
			packer.packString("outputDim").packInt(outputDim);
			packer.packString("numLayer").packInt(NeuralConfig.NUM_LAYER);
			packer.packString("hiddenSize").packInt(NeuralConfig.HIDDEN_SIZE);
			packer.packString("activation").packString(NeuralConfig.ACTIVATION);
			packer.packString("dropout").packDouble(NeuralConfig.DROPOUT);
			packer.packString("optimizer").packString(NeuralConfig.OPTIMIZER);
			packer.packString("learningRate").packDouble(NeuralConfig.LEARNING_RATE);
			packer.packString("fixEmbedding").packBoolean(NeuralConfig.FIX_EMBEDDING);
			packList(packer, "vocab", vocab);
			packer.close();
			
			requester.send(packer.toByteArray(), 0);
			byte[] reply = requester.recv(0);
			double[] nnInternalWeights = null;
			if(optimizeNeural) {
				MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(reply);
				int size = unpacker.unpackArrayHeader();
				nnInternalWeights = new double[size];
				for (int i = 0; i < nnInternalWeights.length; i++) {
					nnInternalWeights[i] = unpackDoubleOrInt(unpacker);
				}
			}
			if (DEBUG) {
				System.out.println("Init returns " + new String(reply));
			}
			return nnInternalWeights;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public void forwardNetwork(boolean training) {
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		int mapSize = optimizeNeural? 3:2;
		try {
			packer.packMapHeader(mapSize);
			packer.packString("cmd").packString("fwd");
			packer.packString("training").packBoolean(training);
			
			if(optimizeNeural) {
				double[] nnInternalWeights = controller.getInternalNeuralWeights();
				packer.packString("weights");
				packer.packArrayHeader(nnInternalWeights.length);
				for (int i = 0; i < nnInternalWeights.length; i++) {
					packer.packDouble(nnInternalWeights[i]);
				}
			}
			packer.close();
			
			requester.send(packer.toByteArray(), 0);
			byte[] reply = requester.recv(0);
			
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(reply);
			int size = unpacker.unpackArrayHeader();
			double[] nnExternalWeights = new double[size];
			for (int i = 0; i < size; i++) {
				nnExternalWeights[i] = unpackDoubleOrInt(unpacker);
			}
			controller.updateExternalNeuralWeights(nnExternalWeights);
			unpacker.close();
			if (DEBUG) {
				System.out.println("Forward returns " + reply.toString());
			}
			
		} catch (IOException e) {
			System.err.println("Exception happened while forwarding network...");
			e.printStackTrace();
		}
		
		
	}
	
	public void backwardNetwork() {
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		try {
			packer.packMapHeader(2);
			packer.packString("cmd").packString("bwd");
			double[] grad = controller.getExternalNeuralGradients();
			packer.packString("grad");
			packer.packArrayHeader(grad.length);
			for (int i = 0; i < grad.length; i++) {
				packer.packDouble(grad[i]);
			}
			packer.close();
			requester.send(packer.toByteArray(), 0);
			
			byte[] reply = requester.recv(0);
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(reply);
			if(optimizeNeural) {
				int size = unpacker.unpackArrayHeader();
				double[] counts = new double[size];
				for (int i = 0; i < counts.length; i++) {
					counts[i] = unpackDoubleOrInt(unpacker);
				}
				controller.setInternalNeuralGradients(counts);
			}
			if (DEBUG) {
				System.out.println("Backward returns " + new String(reply));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	public void saveNetwork(String prefix) {
		try {
			MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
			packer.packMapHeader(2);
			packer.packString("cmd").packString("save");
			packer.packString("savePrefix").packString(prefix);
			packer.close();
			requester.send(packer.toByteArray(), 0);
			
			byte[] reply = requester.recv(0);
			if (DEBUG) {
				System.out.println("Save returns " + new String(reply));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadNetwork(String prefix) {
		try {
			MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
			packer.packMapHeader(2);
			packer.packString("cmd").packString("load");
			packer.packString("savePrefix").packString(prefix);
			packer.close();
			requester.send(packer.toByteArray(), 0);
			
			byte[] reply = requester.recv(0);
			if (DEBUG) {
				System.out.println("Save returns " + new String(reply));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void cleanUp() {
		requester.close();
		context.term();
	}
	
}
