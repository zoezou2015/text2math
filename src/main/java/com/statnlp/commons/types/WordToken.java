package com.statnlp.commons.types;

public class WordToken extends InputToken {

	private static final long serialVersionUID = -1296542134339296118L;

	private String tag;
	private String aTag;
	private int headIndex;
	private String entity;
	private String depLabel;
	private boolean goldNumber = false;
	private boolean predNumber = false;
	private boolean isNumber = false;
	private String lemma;
	private String ner;
	private String pos;
	private int charOffsetBegin = -1;
	private int charOffsetEnd = -1;
	private int tokenId = -1;
	private double numberVal = 0;

	public WordToken(String name) {
		super(name);
		this.tag = "";
		this.headIndex = -1;
		this.entity = "O";
		this.aTag = "";
	}

	public WordToken(String name, String tag) {
		super(name);
		this.tag = tag;
		this.headIndex = -1;
		this.entity = "O";
		this.aTag = tag.substring(0, 1);
	}

	public WordToken(String name, String tag, int headIndex) {
		super(name);
		this.tag = tag;
		this.headIndex = headIndex;
		this.entity = "O";
		this.aTag = tag.substring(0, 1);
	}

	public WordToken(String name, String tag, int headIndex, String entity) {
		super(name);
		this.tag = tag;
		this.headIndex = headIndex;
		this.entity = entity;
		this.aTag = tag.substring(0, 1);
	}

	public WordToken(String name, String tag, int headIndex, String entity, String depLabel) {
		super(name);
		this.tag = tag;
		this.headIndex = headIndex;
		this.entity = entity;
		this.aTag = tag.substring(0, 1);
		this.depLabel = depLabel;
	}

	public String getTag() {
		return this.tag;
	}

	public String getATag() {
		return this.aTag;
	}

	public void setHead(int index) {
		this.headIndex = index;
	}

	public int getHeadIndex() {
		return this.headIndex;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public String getEntity() {
		return this.entity;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getLemma() {
		return this.lemma;
	}

	public void setNER(String ner) {
		this.ner = ner;
	}

	public String getNER() {
		return this.ner;
	}

	public void setCharOffsetBegin(int charOffsetBegin) {
		this.charOffsetBegin = charOffsetBegin;
	}

	public int getCharOffsetBegin() {
		return this.charOffsetBegin;
	}

	public void setCharOffsetEnd(int charOffsetEnd) {
		this.charOffsetEnd = charOffsetEnd;
	}

	public int getCharOffsetEnd() {
		return this.charOffsetEnd;
	}

	public void setPOS(String pos) {
		this.pos = pos;
	}

	public String getPOS() {
		return this.pos;
	}

	public void setGoldNumber(boolean goldNumber) {
		this.goldNumber = goldNumber;
	}

	public boolean isGoldNumber() {
		return this.goldNumber;
	}

	public void setPredNumber(boolean predNumber) {
		this.predNumber = predNumber;
	}

	public boolean isPredNumber() {
		return this.predNumber;
	}

	public double getNumberVal() {
		return numberVal;
	}

	public void setNumberVal(double numberVal) {
		this.numberVal = numberVal;
	}

	public boolean isNumber() {
		return isNumber;
	}

	public void setNumber(boolean isNumber) {
		this.isNumber = isNumber;
	}

	public String getDepLabel() {
		return depLabel;
	}

	public void setDepLabel(String depLabel) {
		this.depLabel = depLabel;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof WordToken) {
			WordToken w = (WordToken) o;
			return w._name.equals(this._name) && w.tag.equals(this.tag) && (w.headIndex == this.headIndex)
					&& w.entity.equals(this.entity);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this._name.hashCode() + this.tag.hashCode() + this.headIndex + this.entity.hashCode() + 7;
	}

	@Override
	public String toString() {
		if (!tag.equals(""))
			return "Word:" + this._name + "/" + tag + "," + headIndex + "," + entity;
		return "WORD:" + this._name;
	}

}