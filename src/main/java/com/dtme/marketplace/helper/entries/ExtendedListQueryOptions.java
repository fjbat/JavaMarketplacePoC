package com.dtme.marketplace.helper.entries;

import java.util.Map;

import org.springframework.web.servlet.support.RequestContext;

import com.dtme.marketplace.entities.VendureEntity;
import com.dtme.marketplace.utils.Scalars.ID;

public class ExtendedListQueryOptions<T extends VendureEntity> {
    private String[] relations;
    private ID channelId;
    private Map<String, Object> where;
    public String[] getRelations() {
		return relations;
	}
	public void setRelations(String[] relations) {
		this.relations = relations;
	}
	public ID getChannelId() {
		return channelId;
	}
	public void setChannelId(ID channelId) {
		this.channelId = channelId;
	}
	public Map<String, Object> getWhere() {
		return where;
	}
	public void setWhere(Map<String, Object> where) {
		this.where = where;
	}
	public FindOneOptions<T> getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(FindOneOptions<T> orderBy) {
		this.orderBy = orderBy;
	}
	public String getEntityAlias() {
		return entityAlias;
	}
	public void setEntityAlias(String entityAlias) {
		this.entityAlias = entityAlias;
	}
	public RequestContext getCtx() {
		return ctx;
	}
	public void setCtx(RequestContext ctx) {
		this.ctx = ctx;
	}
	public Map<String, String> getCustomPropertyMap() {
		return customPropertyMap;
	}
	public void setCustomPropertyMap(Map<String, String> customPropertyMap) {
		this.customPropertyMap = customPropertyMap;
	}
	public boolean isIgnoreQueryLimits() {
		return ignoreQueryLimits;
	}
	public void setIgnoreQueryLimits(boolean ignoreQueryLimits) {
		this.ignoreQueryLimits = ignoreQueryLimits;
	}
	private FindOneOptions<T> orderBy;
    private String entityAlias;
    private RequestContext ctx;
    private Map<String, String> customPropertyMap;
    private boolean ignoreQueryLimits;

    // Constructor, getters, and setters
}
