package com.example.equationparse;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Sentence;
import com.statnlp.example.sp.HybridGrammar;
import com.statnlp.example.sp.HybridPattern;
import com.statnlp.example.sp.SemTextDataManager;
import com.statnlp.example.sp.SemTextNetwork;
import com.statnlp.example.sp.SemanticUnit;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;

public class EquationParserFeatureManager extends FeatureManager {

	private static final long serialVersionUID = -3594609112471243181L;

	private HybridGrammar g;
	private SemTextDataManager dm;

	private enum FEATURE_TYPE {
		emission, transition, pattern, pos, neural
	};

	public EquationParserFeatureManager(GlobalNetworkParam param_g, HybridGrammar g, SemTextDataManager dm) {
		super(param_g);
		this.dm = dm;
		this.g = g;
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {

		SemTextNetwork stNetwork = (SemTextNetwork) network;
		EquationInstance inst = (EquationInstance) stNetwork.getInstance();
		Sentence sent = inst.getInput();

		long parent = stNetwork.getNode(parent_k);
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(parent);

		// if it is a leaf, but the pattern is not w.
		if (ids_parent[4] != 0 && children_k.length == 0) {
			throw new RuntimeException("xxx:" + Arrays.toString(ids_parent));
			// return FeatureArray.NEGATIVE_INFINITY;
		}
		// end index is 0
		if (ids_parent[0] == 0) {
			return FeatureArray.EMPTY;
		}

		// it is the root...
		if (ids_parent[1] == 0 && ids_parent[2] == 0 && ids_parent[3] == 0 && ids_parent[4] == 0) {

			if (children_k.length == 0) {
				return FeatureArray.EMPTY;
			}

			long child = stNetwork.getNode(children_k[0]);
			int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
			SemanticUnit c_unit = this.dm.getAllUnits().get(ids_child[3]);
			// System.out.println("transition: root "+c_unit.toString());
			int f = this._param_g.toFeature(network, FEATURE_TYPE.transition.name(), "ROOT", c_unit.toString());
			int[] fs = new int[] { f };
			return new FeatureArray(fs);

		} else {

			HybridPattern pattern_parent = this.g.getPatternById(ids_parent[4]);

			int eIndex = ids_parent[0];
			int bIndex = ids_parent[0] - ids_parent[1];
			int cIndex = -1;

			SemanticUnit p_unit = this.dm.getAllUnits().get(ids_parent[3]);
			SemanticUnit c_units[] = new SemanticUnit[children_k.length];

			HybridPattern pattern_children[] = new HybridPattern[children_k.length];
			for (int k = 0; k < children_k.length; k++) {
				long child = stNetwork.getNode(children_k[k]);
				int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
				pattern_children[k] = this.g.getPatternById(ids_child[4]);
				if (k == 1) {
					cIndex = ids_child[0] - ids_child[1];
				}
				c_units[k] = this.dm.getAllUnits().get(ids_child[3]);
			}

			return this.extract_helper(network, p_unit, c_units, pattern_parent, pattern_children, sent, bIndex, cIndex,
					eIndex);

		}
		// return FeatureArray.EMPTY;
	}

	private FeatureArray extract_helper(Network network, SemanticUnit p_unit, SemanticUnit[] c_units,
			HybridPattern pattern_parent, HybridPattern[] pattern_children, Sentence sent, int bIndex, int cIndex,
			int eIndex) {

		if (pattern_parent.isw()) {
			return FeatureArray.EMPTY;
		}

		else if (pattern_parent.isA() || pattern_parent.isB() || pattern_parent.isC()) {

			if (pattern_children.length != 1) {
				throw new RuntimeException("The pattern_children has size " + pattern_children.length);
			}

			FeatureArray fa = null;

			if (p_unit.isContextIndependent()) {
				if (!pattern_parent.isA()) {
					System.out.println("independent unit " + pattern_parent.getName() + " " + p_unit.toString());
				}
				StringBuilder sb_phrase = new StringBuilder();
				for (int index = bIndex; index < eIndex; index++) {
					sb_phrase.append(sent.get(index).getName());
					sb_phrase.append(' ');
				}
				String phrase = sb_phrase.toString().trim();
				ArrayList<String> phrases = this.dm.getPriorUnitToPhrases(p_unit);
				if (!phrases.contains(phrase)) {
					// System.out.println("xx[" + phrase + "]" + ">>" +
					// p_unit.toString());
					// System.out.println("return negative infinity");
					return FeatureArray.NEGATIVE_INFINITY;
				} else {
					// System.out.println("YES[" + phrase + "]" + ">>" +
					// p_unit.toString());
					return FeatureArray.EMPTY;
				}
			}
			if (EquationParserConfig.USE_PREDICT_NUMBER) {
				boolean isNumber = false;
				for (int index = bIndex; index < eIndex; index++) {
					isNumber = sent.get(index).isPredNumber();
					if (isNumber)
						break;
				}
				if (pattern_parent.isA() && (eIndex - bIndex) == 1 && isNumber && p_unit.getMRL().equals("NUM")) {
					return FeatureArray.EMPTY;
				} else if (pattern_parent.isA() && (eIndex - bIndex) == 1 && isNumber
						&& !p_unit.getMRL().equals("NUM")) {
					// System.out.println("infinity");
					return FeatureArray.NEGATIVE_INFINITY;
				}
			} else if (EquationParserConfig.USE_GOLD_NUMBER) {
				boolean isNumber = false;
				for (int index = bIndex; index < eIndex; index++) {
					isNumber = sent.get(index).isGoldNumber();
					if (isNumber)
						break;
				}
				if (pattern_parent.isA() && (eIndex - bIndex) == 1 && isNumber && p_unit.getMRL().equals("NUM")) {
					return FeatureArray.EMPTY;
				} else if (pattern_parent.isA() && (eIndex - bIndex) == 1 && isNumber
						&& !p_unit.getMRL().equals("NUM")) {
					// System.out.println("infinity");
					return FeatureArray.NEGATIVE_INFINITY;
				}
			}

			// SemTextNetworkConfig._SEMANTIC_PARSING_NGRAM = 1
			for (int historySize = 0; historySize <= EquationParserConfig._SEMANTIC_PARSING_NGRAM - 1; historySize++) {
				int prevWord;
				if (historySize == 0) {
					prevWord = 1;
				} else {
					prevWord = historySize;
				}

				// int[] fs = new int[1 + 1 + prevWord];
				ArrayList<Integer> fs = new ArrayList<>();
				int t = 0;

				// pattern -> p_unit -> c_pattern
				// fs[t++] = this._param_g.toFeature(network, FEATURE_TYPE.pattern.name(),
				// p_unit.toString(),
				// pattern_children[0].toString());
				int f = this._param_g.toFeature(network, FEATURE_TYPE.pattern.name(), p_unit.toString(),
						pattern_children[0].toString());
				fs.add(f);
				// System.out.println("root a b c: " + p_unit.toString() + " " +
				// pattern_children[0].toString());
				String output, input;

				ArrayList<String> wordsInWindow;
				wordsInWindow = new ArrayList<>();
				// historySize = 0
				for (int k = 0; k < historySize; k++) {
					String word = this.getForm(pattern_children[0], k - historySize, sent, eIndex - (historySize - k));
					wordsInWindow.add(word);
				}

				// single last word.
				output = p_unit.toString();
				for (int k = 0; k < historySize; k++) {
					output += wordsInWindow.get(k) + "|||";
				}
				input = "[END]";
				// emission -> p_unit+word/[X]/[Y] -> [END]
				// fs[t++] = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(),
				// output, input);
				f = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(), output, input);
				fs.add(f);
				// int fp = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(),
				// output, input);
				// fs.add(fp);

				// System.out.println("root a b c: " + output + " " + input);

				wordsInWindow = new ArrayList<>();
				for (int k = 0; k < historySize; k++) {
					wordsInWindow.add("[BEGIN]");
				}
				// prevWord = 1
				for (int w = 0; w < prevWord; w++) {
					// the first _prevWord words
					output = p_unit.toString();
					for (int k = 0; k < historySize; k++) {
						output += wordsInWindow.get(k) + "|||";
					}
					input = this.getForm(pattern_children[0], w, sent, bIndex + w);
					if (NetworkConfig.USE_NEURAL_FEATURES) {
						if (!input.equals("[X]") && !input.equals("[Y]")) {
							boolean first = true;
							String window = "";
							int N = NetworkConfig.NEURAL_WINDOW_SIZE;
							for (int offset = -N; offset <= N; offset++) {
								if (!first)
									window += NeuralConfig.IN_SEP;
								window += getWord(sent, bIndex + w, offset);
								first = false;
							}
							// fs[t++] = this._param_g.toFeature(network, FEATURE_TYPE.neural.name(),
							// output, window);
							fs.add(this._param_g.toFeature(network, FEATURE_TYPE.neural.name(), output, window));

						} else {
							// System.out.println("X/Y");
						}
					}
					// System.out.println("output "+output);
					// System.out.println("input "+input);
					// emission -> p_unit -> word/[X]/[Y]
					// fs[t++] = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(),
					// output, input);
					if (!NetworkConfig.USE_NEURAL_FEATURES || !NetworkConfig.REPLACE_ORIGINAL_EMISSION
							|| input.equals("[X]") || input.equals("[Y]")) {
						f = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(), output, input);
						fs.add(f);
						String poString = this.getPOS(pattern_children[0], w, sent, bIndex + w);
						if (EquationParserConfig.USE_POS_FEAT) {
							int fp = this._param_g.toFeature(network, FEATURE_TYPE.pos.name(), output, poString);
							fs.add(fp);
						}
					}

					// String ner = this.getNER(pattern_children[0], w, sent, bIndex + w);
					// int fl = this._param_g.toFeature(network, FEATURE_TYPE.NER.name(), output,
					// ner);

					wordsInWindow.add(input);
					wordsInWindow.remove(0);
				}
				int[] f_array = new int[fs.size()];
				for (int i = 0; i < fs.size(); i++) {
					f_array[i] = fs.get(i);
				}
				fa = new FeatureArray(f_array, fa);
			}

			return fa;
		}

		else if (pattern_parent.isX())

		{
			if (c_units[0].isContextIndependent()) {
				// System.out.println("wow pattern X with context independent
				// child unit " + c_units[0].toString());
				return FeatureArray.EMPTY;
			}
			if (EquationParserConfig.USE_PREDICT_NUMBER) {
				boolean isNumber = false;
				for (int index = bIndex; index < eIndex; index++) {
					isNumber = sent.get(index).isPredNumber();
					if (isNumber)
						break;
				}
				if ((eIndex - bIndex) == 1 && isNumber && c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.EMPTY;
				} else if ((eIndex - bIndex) == 1 && isNumber && !c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.NEGATIVE_INFINITY;
				}
			} else if (EquationParserConfig.USE_GOLD_NUMBER) {
				boolean isNumber = false;
				for (int index = bIndex; index < eIndex; index++) {
					isNumber = sent.get(index).isGoldNumber();
					if (isNumber)
						break;
				}
				if ((eIndex - bIndex) == 1 && isNumber && c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.EMPTY;
				} else if ((eIndex - bIndex) == 1 && isNumber && !c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.NEGATIVE_INFINITY;
				}
			}

			// transition -> p_unit -> c_unit
			int f = this._param_g.toFeature(network, FEATURE_TYPE.transition.name(), p_unit.toString() + ":0",
					c_units[0].toString());
			// System.out.println("c_units[0].toString().toString() " +
			// c_units[0].toString().toString());
			// System.out.println("c_units[0].toString() " +
			// c_units[0].toString());

			int[] fs = new int[] { f };
			return new FeatureArray(fs);
		}

		else if (pattern_parent.isY()) {
			if (c_units[0].isContextIndependent()) {
				// System.out.println("wow pattern Y with context independent
				// child unit " + c_units[0].toString());
				return FeatureArray.EMPTY;
			}
			if (EquationParserConfig.USE_PREDICT_NUMBER) {
				boolean isNumber = false;
				for (int index = bIndex; index < eIndex; index++) {
					isNumber = sent.get(index).isPredNumber();
					if (isNumber)
						break;
				}
				if ((eIndex - bIndex) == 1 && isNumber && c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.EMPTY;
				} else if ((eIndex - bIndex) == 1 && isNumber && !c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.NEGATIVE_INFINITY;
				}
			} else if (EquationParserConfig.USE_GOLD_NUMBER) {
				boolean isNumber = false;
				for (int index = bIndex; index < eIndex; index++) {
					isNumber = sent.get(index).isGoldNumber();
					if (isNumber)
						break;
				}
				if ((eIndex - bIndex) == 1 && isNumber && c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.EMPTY;
				} else if ((eIndex - bIndex) == 1 && isNumber && !c_units[0].getMRL().equals("NUM")) {
					return FeatureArray.NEGATIVE_INFINITY;
				}
			}
			// transition -> p_unit -> c_unit
			int f = this._param_g.toFeature(network, FEATURE_TYPE.transition.name(), p_unit.toString() + ":1",
					c_units[0].toString().toString());
			int[] fs = new int[] { f };
			return new FeatureArray(fs);
		}
		// [W]/[w]
		else if (pattern_children.length == 1) {
			return FeatureArray.EMPTY;
		}

		else {
			FeatureArray fa = null;
			// NetworkConfig._SEMANTIC_PARSING_NGRAM ＝ 1
			for (int historySize = 0; historySize <= EquationParserConfig._SEMANTIC_PARSING_NGRAM - 1; historySize++) {
				int prevWord;
				if (historySize == 0) {
					prevWord = 1;
				} else {
					prevWord = historySize;
				}

				// prevWord = 1
				// int[] fs = new int[prevWord];
				String output, input;
				ArrayList<Integer> fs = new ArrayList<>();
				ArrayList<String> wordsInWindow = new ArrayList<>();
				for (int k = 0; k < historySize; k++) {
					// System.err.println(pattern_parent + "\t" + pattern_children[0] + "\t" + (k -
					// prevWord) + "\t"
					// + (cIndex - (prevWord - k)) + "\t" + k);
					String word = this.getForm(pattern_children[0], k - historySize, sent, cIndex - (historySize - k));
					wordsInWindow.add(word);
				}

				for (int w = 0; w < prevWord; w++) {
					// the first _prevWord words
					output = p_unit.toString();
					// historySize = 0
					for (int k = 0; k < historySize; k++) {
						output += wordsInWindow.get(k) + "|||";
					}

					input = this.getForm(pattern_children[1], w, sent, cIndex + w);
					if (NetworkConfig.USE_NEURAL_FEATURES) {
						if (!input.equals("[X]") && !input.equals("[Y]")) {
							boolean first = true;
							String window = "";
							int N = NetworkConfig.NEURAL_WINDOW_SIZE;
							for (int offset = -N; offset <= N; offset++) {
								if (!first)
									window += NeuralConfig.IN_SEP;
								window += getWord(sent, cIndex + w, offset);
								first = false;
							}
							// fs[t++] = this._param_g.toFeature(network,FEATURE_TYPE.neural.name(), output,
							// window);
							fs.add(this._param_g.toFeature(network, FEATURE_TYPE.neural.name(), output, window));
						} else {
							// System.out.println("X/Y");
						}
					}
					// emission -> p_unit -> word/[X]/[Y]
					// fs[w] = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(),
					// output, input);
					if (!NetworkConfig.USE_NEURAL_FEATURES || !NetworkConfig.REPLACE_ORIGINAL_EMISSION
							|| input.equals("[X]") || input.equals("[Y]")) {
						int f = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(), output, input);
						fs.add(f);
						if (EquationParserConfig.USE_POS_FEAT) {
							String poString = this.getPOS(pattern_children[1], w, sent, cIndex + w);
							int fp = this._param_g.toFeature(network, FEATURE_TYPE.pos.name(), output, poString);
							fs.add(fp);
						}
					}
					// int f = this._param_g.toFeature(network, FEATURE_TYPE.emission.name(),
					// output, input);
					// fs.add(f);
					// String poString = this.getPOS(pattern_children[1], w, sent, cIndex + w);
					// int fp = this._param_g.toFeature(network, FEATURE_TYPE.pos.name(), output,
					// poString);
					// fs.add(fp);

					wordsInWindow.add(input);
					wordsInWindow.remove(0);
				}
				int[] f_array = new int[fs.size()];
				for (int i = 0; i < fs.size(); i++) {
					f_array[i] = fs.get(i);
				}
				fa = new FeatureArray(f_array, fa);
			}
			return fa;

		}

	}

	/**
	 * Return a word sentence[index] if p[offset] is w or W; or return pattern [X]
	 * or [Y]
	 * 
	 * @param p
	 * @param offset
	 * @param sent
	 * @param index
	 * @return
	 */
	private String getForm(HybridPattern p, int offset, Sentence sent, int index) {
		// get the pattern element index of offset
		char c = p.getFormat(offset);
		// System.out.println("getForm HybridPattern " + p + " index: " + offset + "
		// char: " + c);
		// if c is referring word(s), then return sentence[index]
		if (c == 'w' || c == 'W') {
			// System.out.println("getForm c=='w' ||
			// c=='W'"+sent.get(index).getName());
			return sent.get(index).getName();
		}
		// throw error if c is not w,W,X or Y which are all patterns we have
		if (c != 'X' && c != 'Y') {
			throw new RuntimeException("Invalid:" + p + "\t" + c);
		}
		// System.out.println("getForm "+"["+c+"]");
		// finally return pattern X or Y
		return "[" + c + "]";

	}

	private String getLemma(HybridPattern p, int offset, Sentence sent, int index) {
		// get the pattern element index of offset
		char c = p.getFormat(offset);
		// System.out.println("getForm HybridPattern "+p + "index: "+offset);
		// if c is referring word(s), then return sentence[index]
		if (c == 'w' || c == 'W') {
			// System.out.println("getForm c=='w' ||
			// c=='W'"+sent.get(index).getName());
			return sent.get(index).getLemma();
		}
		// throw error if c is not w,W,X or Y which are all patterns we have
		if (c != 'X' && c != 'Y') {
			throw new RuntimeException("Invalid:" + p + "\t" + c);
		}
		// System.out.println("getForm "+"["+c+"]");
		// finally return pattern X or Y
		return "[" + c + "]";

	}

	private String getNER(HybridPattern p, int offset, Sentence sent, int index) {
		// get the pattern element index of offset
		char c = p.getFormat(offset);
		// System.out.println("getForm HybridPattern "+p + "index: "+offset);
		// if c is referring word(s), then return sentence[index]
		if (c == 'w' || c == 'W') {
			// System.out.println("getForm c=='w' ||
			// c=='W'"+sent.get(index).getName());
			return sent.get(index).getNER();
		}
		// throw error if c is not w,W,X or Y which are all patterns we have
		if (c != 'X' && c != 'Y') {
			throw new RuntimeException("Invalid:" + p + "\t" + c);
		}
		// System.out.println("getForm "+"["+c+"]");
		// finally return pattern X or Y
		return "[" + c + "]";

	}

	private String getPOS(HybridPattern p, int offset, Sentence sent, int index) {
		// get the pattern element index of offset
		char c = p.getFormat(offset);
		// System.out.println("getForm HybridPattern "+p + "index: "+offset);
		// if c is referring word(s), then return sentence[index]
		if (c == 'w' || c == 'W') {
			// System.out.println("getForm c=='w' ||
			// c=='W'"+sent.get(index).getName());
			return sent.get(index).getPOS();
		}
		// throw error if c is not w,W,X or Y which are all patterns we have
		if (c != 'X' && c != 'Y') {
			throw new RuntimeException("Invalid:" + p + "\t" + c);
		}
		// System.out.println("getForm "+"["+c+"]");
		// finally return pattern X or Y
		return "[" + c + "]";

	}

	private String getWord(Sentence sent, int index, int offset) {
		int target = index + offset;
		if (target == -1) {
			return "<S>";
		} else if (target == sent.length()) {
			return "</S>";
		} else if (target >= 0 && target < sent.length()) {
			if (sent.get(target).getName().equals("")) { // for multiple whitespaces..
				return "<UNK>";
			}
			return sent.get(target).getName();
		} else {
			return "<PAD>";
		}
	}
}
