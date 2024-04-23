package com.dtme.marketplace.helper.entries;

import java.util.Map;

public interface WhereCondition {
    String getClause();
    Map<String, Object> getParameters();
}

