package com.dtme.marketplace.helper.entries;

import java.util.HashMap;
import java.util.Map;

import com.dtme.marketplace.entities.VendureEntity;

public class FilterParameter<T extends VendureEntity> extends HashMap<String, Object> {

	public FilterParameter() {
		super();
	}

	public FilterParameter(Map<? extends String, ?> m) {
		super(m);
	}

	// Define the operators enums for different types
	public enum StringOperators {
		EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH;
	}

	public enum NumberOperators {
		EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL;
	}

	public enum BooleanOperators {
		EQUALS;
	}

	public enum DateOperators {
		EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL;
	}

	// Define the _and and _or properties
	private FilterParameter<T>[] _and;
	private FilterParameter<T>[] _or;

	public FilterParameter<T>[] get_and() {
		return _and;
	}

	public void set_and(FilterParameter<T>[] _and) {
		this._and = _and;
	}

	public FilterParameter<T>[] get_or() {
		return _or;
	}

	public void set_or(FilterParameter<T>[] _or) {
		this._or = _or;
	}
}
