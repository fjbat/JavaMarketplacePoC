package com.dtme.marketplace.helper.entries;

import java.util.Map;

import com.dtme.marketplace.entities.VendureEntity;

public class ListQueryOptions<T extends VendureEntity> {
    private Integer take;
    public Integer getTake() {
		return take;
	}
	public void setTake(Integer take) {
		this.take = take;
	}
	public Integer getSkip() {
		return skip;
	}
	public void setSkip(Integer skip) {
		this.skip = skip;
	}
	public Map<String, SortParameter<T>> getSort() {
		return sort;
	}
	public void setSort(Map<String, SortParameter<T>> sort) {
		this.sort = sort;
	}
	public Map<String, FilterParameter<T>> getFilter() {
		return filter;
	}
	public void setFilter(Map<String, FilterParameter<T>> filter) {
		this.filter = filter;
	}
	public LogicalOperator getFilterOperator() {
		return filterOperator;
	}
	public void setFilterOperator(LogicalOperator filterOperator) {
		this.filterOperator = filterOperator;
	}
	private Integer skip;
    private Map<String, SortParameter<T>> sort;
    private Map<String, FilterParameter<T>> filter;
    private LogicalOperator filterOperator;

    // Constructors, getters, and setters
}

