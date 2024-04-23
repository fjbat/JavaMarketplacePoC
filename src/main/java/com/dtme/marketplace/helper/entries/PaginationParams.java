package com.dtme.marketplace.helper.entries;

public class PaginationParams {
	private int take;
	private int skip;

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

	public PaginationParams(int take, int skip) {
		super();
		this.take = take;
		this.skip = skip;
	}

}
