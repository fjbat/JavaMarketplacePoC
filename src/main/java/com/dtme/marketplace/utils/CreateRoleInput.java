package com.dtme.marketplace.utils;

import java.util.List;

public class CreateRoleInput {
    private List<Scalars.ID> channelIds;
    public List<Scalars.ID> getChannelIds() {
		return channelIds;
	}
	public void setChannelIds(List<Scalars.ID> channelIds) {
		this.channelIds = channelIds;
	}
	public Scalars.String getCode() {
		return code;
	}
	public void setCode(Scalars.String code) {
		this.code = code;
	}
	public Scalars.String getDescription() {
		return description;
	}
	public void setDescription(Scalars.String description) {
		this.description = description;
	}
	public List<Permission> getPermissions() {
		return permissions;
	}
	public void setPermissions(List<Permission> permissions) {
		this.permissions = permissions;
	}
	private Scalars.String code;
    private Scalars.String description;
    private List<Permission> permissions;

    // Getters and setters
}