/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.statnlp.hybridnetworks;

import java.io.Serializable;

public class FeatureArray implements Serializable {

	private static final long serialVersionUID = 9170537017171193020L;

	private double _score;
	private int[] _fs;
	private boolean _isLocal = false;

	public static final FeatureArray EMPTY = new FeatureArray(new int[0]);
	public static final FeatureArray NEGATIVE_INFINITY = new FeatureArray(-10000);
	// public static final FeatureArray NEGATIVE_INFINITY = new
	// FeatureArray(Double.NEGATIVE_INFINITY);

	/**
	 * Merges the features in <code>fs</code> and in <code>next</code>
	 * 
	 * @deprecated This one is inefficient. Use {@link #FeatureArray(int[])}
	 *             instead.
	 * @param fs
	 * @param next
	 */
	@Deprecated
	public FeatureArray(int[] fs, FeatureArray next) {
		this._fs = fs;
		if (next != null && next._fs != null) {
			this._fs = new int[fs.length + next._fs.length];
			for (int i = 0; i < fs.length; i++) {
				this._fs[i] = fs[i];
			}
			for (int i = 0; i < next._fs.length; i++) {
				this._fs[i + fs.length] = next._fs[i];
			}
		}
	}

	/**
	 * Construct a feature array containing the features identified by their indices
	 * 
	 * @param fs
	 */
	public FeatureArray(int[] fs) {
		this._fs = fs;
		this._isLocal = false;
	}

	private FeatureArray(double score) {
		this._score = score;
	}

	public FeatureArray toLocal(LocalNetworkParam param) {
		if (this == NEGATIVE_INFINITY) {
			return this;
		}
		if (this._isLocal) {
			return this;
		}

		int length = this._fs.length;
		if (NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY) {
			for (int fs : this._fs) {
				if (fs == -1) {
					length--;
				}
			}
		}

		int[] fs_local = new int[length];
		int localIdx = 0;
		for (int k = 0; k < this._fs.length; k++, localIdx++) {
			if (this._fs[k] == -1 && NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY) {
				localIdx--;
				continue;
			}
			if (!NetworkConfig.PARALLEL_FEATURE_EXTRACTION || NetworkConfig.NUM_THREADS == 1 || param._isFinalized) {
				fs_local[localIdx] = param.toLocalFeature(this._fs[k]);
			} else {
				fs_local[localIdx] = this._fs[k];
			}
			if (fs_local[localIdx] == -1) {
				throw new RuntimeException("The local feature got an id of -1 for " + this._fs[k]);
			}
		}

		FeatureArray fa = new FeatureArray(fs_local);
		fa._isLocal = true;
		return fa;
	}

	public int[] getCurrent() {
		return this._fs;
	}

	public void update(LocalNetworkParam param, double count) {
		if (this == NEGATIVE_INFINITY) {
			return;
		}

		// if(!this._isLocal)
		// throw new RuntimeException("This feature array is not local");

		int[] fs_local = this.getCurrent();
		for (int f_local : fs_local) {
			param.addCount(f_local, count);
		}
	}

	/**
	 * Return the sum of weights of the features in this array
	 * 
	 * @param param
	 * @return
	 */
	public double getScore(LocalNetworkParam param) {
		if (this == NEGATIVE_INFINITY) {
			return this._score;
		}
		if (!this._isLocal != param.isGlobalMode()) {
			throw new RuntimeException(
					"This FeatureArray is local? " + this._isLocal + "; The param is " + param.isGlobalMode());
		}

		// if the score is negative infinity, it means disabled.
		if (this._score == Double.NEGATIVE_INFINITY) {
			return this._score;
		}

		this._score = this.computeScore(param, this.getCurrent());

		return this._score;
	}

	private double computeScore(LocalNetworkParam param, int[] fs) {
		if (!this._isLocal != param.isGlobalMode()) {
			throw new RuntimeException(
					"This FeatureArray is local? " + this._isLocal + "; The param is " + param.isGlobalMode());
		}

		double score = 0.0;
		for (int f : fs) {
			if (f != -1) {
				score += param.getWeight(f);
			}
		}
		return score;
	}

	// returns the number of elements in the feature array
	public int size() {
		int size = this._fs.length;
		return size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int k = 0; k < this._fs.length; k++) {
			if (k != 0)
				sb.append(' ');
			sb.append(this._fs[k]);
		}
		sb.append(']');
		return sb.toString();
	}

	@Override
	public int hashCode() {
		int code = 0;
		for (int i = 0; i < this._fs.length; i++) {
			code ^= this._fs[i];
		}
		return code;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FeatureArray) {
			FeatureArray fa = (FeatureArray) o;
			for (int k = 0; k < this._fs.length; k++) {
				if (this._fs[k] != fa._fs[k]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}