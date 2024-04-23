package com.dtme.marketplace.controllers;

import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestContext;

import com.dtme.marketplace.entities.Channel;
import com.dtme.marketplace.entities.Role;
import com.dtme.marketplace.service.RoleService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class RoleEntityResolver implements GraphQLResolver<Role> {

	private final RoleService roleService;

	@Autowired
	public RoleEntityResolver(RoleService roleService) {
		this.roleService = roleService;
	}

	public List<Channel> channels(RequestContext ctx, Role role) {
		
			if (role.getChannels() != null) {
				return role.getChannels();
			}
			return roleService.getChannelsForRole(ctx, role.getId());
		
	}
}
