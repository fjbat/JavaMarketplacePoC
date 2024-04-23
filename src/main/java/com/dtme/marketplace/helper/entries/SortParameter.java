package com.dtme.marketplace.helper.entries;

import java.util.Map;

import com.dtme.marketplace.entities.VendureEntity;

public class SortParameter<T extends VendureEntity> {
    private Map<String, SortOrder> fields;

	public Map<String, SortOrder> getFields() {
		return fields;
	}

	public void setFields(Map<String, SortOrder> fields) {
		this.fields = fields;
	}

    // Constructors, getters, and setters
}