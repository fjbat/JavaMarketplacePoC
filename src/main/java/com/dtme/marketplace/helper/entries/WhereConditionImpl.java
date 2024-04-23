package com.dtme.marketplace.helper.entries;

import java.util.Map;

public class WhereConditionImpl implements WhereCondition {
	private String clause;
	private Map<String, Object> parameters;

	@Override
	public String getClause() {
		return clause;
	}

	@Override
	public Map<String, Object> getParameters() {
		return parameters;
	}

	// Add setters or constructor to initialize the fields
}
