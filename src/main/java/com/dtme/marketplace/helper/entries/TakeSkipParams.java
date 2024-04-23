package com.dtme.marketplace.helper.entries;

public class TakeSkipParams {
	private int take; public TakeSkipParams(int take, int skip) {
		super();
		this.take = take;
		this.skip = skip;
	}

	public int getTake() {
		return take;
	}

	public void setTake(int take) {
		this.take = take;
	}

	public int getSkip() {
		return skip;
	}

	public void setSkip(int skip) {
		this.skip = skip;
	}

	private int skip;

	

}
