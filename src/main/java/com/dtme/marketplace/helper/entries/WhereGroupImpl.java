package com.dtme.marketplace.helper.entries;

import java.util.ArrayList;
import java.util.List;

public class WhereGroupImpl implements WhereGroup {
	private LogicalOperator operator;
	private List<WhereCondition> conditions = new ArrayList<>();

	@Override
	public LogicalOperator getOperator() {
		return operator;
	}

	@Override
	public List<WhereCondition> getConditions() {
		return conditions;
	}

	// Add setters or constructor to initialize the fields
}
