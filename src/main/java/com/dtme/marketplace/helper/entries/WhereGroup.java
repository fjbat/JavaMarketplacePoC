package com.dtme.marketplace.helper.entries;

import java.util.ArrayList;
import java.util.List;

public interface WhereGroup {
    LogicalOperator getOperator();
    List<WhereCondition> getConditions();
}

