package com.dtme.marketplace.controllers;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.RequestContext;

import com.dtme.marketplace.entities.Role;
import com.dtme.marketplace.service.RoleService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class RoleResolver implements GraphQLQueryResolver, GraphQLMutationResolver {

    private final RoleService roleService;

    @Autowired
    public RoleResolver(RoleService roleService) {
        this.roleService = roleService;
    }

    public CompletableFuture<PaginatedList<Role>> roles(RequestContext ctx, QueryRolesArgs args, RelationPaths<Role> relations) {
        return CompletableFuture.supplyAsync(() -> roleService.findAll(ctx, args.getOptions(), relations));
    }

    public CompletableFuture<Role> role(RequestContext ctx, QueryRoleArgs args, RelationPaths<Role> relations) {
        return CompletableFuture.supplyAsync(() -> roleService.findOne(ctx, args.getId(), relations));
    }

    @Transactional
    public CompletableFuture<Role> createRole(RequestContext ctx, MutationCreateRoleArgs args) {
        return CompletableFuture.supplyAsync(() -> roleService.create(ctx, args.getInput()));
    }

    @Transactional
    public CompletableFuture<Role> updateRole(RequestContext ctx, MutationUpdateRoleArgs args) {
        return CompletableFuture.supplyAsync(() -> roleService.update(ctx, args.getInput()));
    }

    @Transactional
    public CompletableFuture<DeletionResponse> deleteRole(RequestContext ctx, MutationDeleteRoleArgs args) {
        return CompletableFuture.supplyAsync(() -> roleService.delete(ctx, args.getId()));
    }

    @Transactional
    public CompletableFuture<List<DeletionResponse>> deleteRoles(RequestContext ctx, MutationDeleteRolesArgs args) {
        return CompletableFuture.supplyAsync(() -> {
            List<DeletionResponse> deletionResponses = new ArrayList<>();
            for (ID id : args.getIds()) {
                deletionResponses.add(roleService.delete(ctx, id));
            }
            return deletionResponses;
        });
    }
}
