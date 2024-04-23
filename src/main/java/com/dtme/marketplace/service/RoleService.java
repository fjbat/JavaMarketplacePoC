package com.dtme.marketplace.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.servlet.support.RequestContext;

import com.dtme.marketplace.entities.Channel;
import com.dtme.marketplace.utils.Permission;
import com.dtme.marketplace.repos.ChannelRepository;
import com.dtme.marketplace.repos.RoleRepository;
import com.dtme.marketplace.utils.CreateRoleInput;
import com.dtme.marketplace.entities.Role;
import com.dtme.marketplace.helper.ConfigService;
import com.dtme.marketplace.helper.ListQueryBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {
    
    private final TransactionalConnection connection;
    private final ChannelService channelService;
    private final ListQueryBuilder listQueryBuilder;
    private final ConfigService configService;
    private final EventBus eventBus;

    @Autowired
    public RoleService(TransactionalConnection connection, ChannelService channelService, 
                       ListQueryBuilder listQueryBuilder, ConfigService configService, EventBus eventBus) {
        this.connection = connection;
        this.channelService = channelService;
        this.listQueryBuilder = listQueryBuilder;
        this.configService = configService;
        this.eventBus = eventBus;
    }

    public void initRoles() {
        ensureSuperAdminRoleExists();
        ensureCustomerRoleExists();
        ensureRolesHaveValidPermissions();
    }

    public Page<Role> findAll(RequestContext ctx, ListQueryOptions<Role> options, 
                                       List<String> relations) {
        return listQueryBuilder.build(Role.class, options, relations, ctx)
            .getManyAndCount()
            .map(pair -> {
                List<Role> items = pair.getFirst().stream()
                    .filter(item -> activeUserCanReadRole(ctx, item))
                    .collect(Collectors.toList());
                return new PaginatedList<>(items, pair.getSecond());
            });
    }
    public Role findOne(RequestContext ctx, ID roleId, List<String> relations) {
        return connection.getRepository(ctx, Role.class)
                .findOne(roleId, relations)
                .filter(role -> activeUserCanReadRole(ctx, role))
                .orElse(null);
    }

    public List<Channel> getChannelsForRole(RequestContext ctx, ID roleId) {
        return findOne(ctx, roleId, List.of("channels"))
                .map(Role::getChannels)
                .orElse(List.of());
    }

    public Role getSuperAdminRole(RequestContext ctx) {
        return getRoleByCode(ctx, SUPER_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new InternalServerError("error.super-admin-role-not-found"));
    }

    public Role getCustomerRole(RequestContext ctx) {
        return getRoleByCode(ctx, CUSTOMER_ROLE_CODE)
                .orElseThrow(() -> new InternalServerError("error.customer-role-not-found"));
    }
    public List<String> getAllPermissions() {
        return Arrays.asList(Permission.values());
    }

    /**
     * @description
     * Returns true if the User has the specified permission on that Channel
     */
    public boolean userHasPermissionOnChannel(RequestContext ctx, ID channelId, String permission) {
        return userHasAnyPermissionsOnChannel(ctx, channelId, List.of(permission));
    }

    /**
     * @description
     * Returns true if the User has any of the specified permissions on that Channel
     */
    public boolean userHasAnyPermissionsOnChannel(RequestContext ctx, ID channelId, List<String> permissions) {
        List<String> permissionsOnChannel = getActiveUserPermissionsOnChannel(ctx, channelId);
        return permissionsOnChannel.containsAny(permissions);
    }
    public boolean activeUserCanReadRole(RequestContext ctx, Role role) {
        List<ChannelPermission> permissionsRequired = getChannelPermissions(List.of(role));
        for (ChannelPermission channelPermissions : permissionsRequired) {
            boolean activeUserHasRequiredPermissions = userHasAllPermissionsOnChannel(
                ctx, channelPermissions.getId(), channelPermissions.getPermissions());
            if (!activeUserHasRequiredPermissions) {
                return false;
            }
        }
        return true;
    }

    /**
     * @description
     * Returns true if the User has all the specified permissions on that Channel
     */
    public boolean userHasAllPermissionsOnChannel(RequestContext ctx, ID channelId, List<Permission> permissions) {
        List<Permission> permissionsOnChannel = getActiveUserPermissionsOnChannel(ctx, channelId);
        return permissionsOnChannel.containsAll(permissions);
    }

    private List<Permission> getActiveUserPermissionsOnChannel(RequestContext ctx, ID channelId) {
        if (ctx.getActiveUserId() == null) {
            return List.of();
        }
        User user = connection.getEntityOrThrow(ctx, User.class, ctx.getActiveUserId(), List.of("roles", "roles.channels"));
        List<ChannelPermission> userChannels = getUserChannelsPermissions(user);
        ChannelPermission channel = userChannels.stream()
            .filter(c -> idsAreEqual(c.getId(), channelId))
            .findFirst()
            .orElse(null);
        if (channel == null) {
            return List.of();
        }
        return channel.getPermissions();
    }
    public Role create(RequestContext ctx, CreateRoleInput input) {
        checkPermissionsAreValid(input.getPermissions());

        List<Channel> targetChannels = new ArrayList<>();
        if (input.getChannelIds() != null && !input.getChannelIds().isEmpty()) {
            targetChannels = getPermittedChannels(ctx, input.getChannelIds());
        } else {
            targetChannels.add(ctx.getChannel());
        }
        checkActiveUserHasSufficientPermissions(ctx, targetChannels, input.getPermissions());
        Role role = createRoleForChannels(ctx, input, targetChannels);
        eventBus.publish(new RoleEvent(ctx, role, "created", input));
        return role;
    }

    public Role update(RequestContext ctx, UpdateRoleInput input) {
        checkPermissionsAreValid(input.getPermissions());
        Role role = findOne(ctx, input.getId());
        if (role == null) {
            throw new EntityNotFoundError("Role", input.getId());
        }
        if (role.getCode().equals(SUPER_ADMIN_ROLE_CODE) || role.getCode().equals(CUSTOMER_ROLE_CODE)) {
            throw new InternalServerError("error.cannot-modify-role", Map.of("roleCode", role.getCode()));
        }
        List<Channel> targetChannels = null;
        if (input.getChannelIds() != null && !input.getChannelIds().isEmpty()) {
            targetChannels = getPermittedChannels(ctx, input.getChannelIds());
        }
        if (input.getPermissions() != null) {
            checkActiveUserHasSufficientPermissions(ctx, targetChannels != null ? targetChannels : role.getChannels(), input.getPermissions());
        }
        Role updatedRole = patchEntity(role, Map.of(
            "code", input.getCode(),
            "description", input.getDescription(),
            "permissions", input.getPermissions() != null ? unique(Permission.Authenticated, input.getPermissions()) : null
        ));
        if (targetChannels != null) {
            updatedRole.setChannels(targetChannels);
        }
        connection.getRepository(ctx, Role.class).save(updatedRole, false);
        eventBus.publish(new RoleEvent(ctx, role, "updated", input));
        return assertFound(findOne(ctx, role.getId()));
    }
    public DeletionResponse delete(RequestContext ctx, ID id) {
        Role role = findOne(ctx, id);
        if (role == null) {
            throw new EntityNotFoundError("Role", id);
        }
        if (role.getCode().equals(SUPER_ADMIN_ROLE_CODE) || role.getCode().equals(CUSTOMER_ROLE_CODE)) {
            throw new InternalServerError("error.cannot-delete-role", Map.of("roleCode", role.getCode()));
        }
        Role deletedRole = new Role(role);
        connection.getRepository(ctx, Role.class).remove(role);
        eventBus.publish(new RoleEvent(ctx, deletedRole, "deleted", id));
        return new DeletionResponse(DeletionResult.DELETED);
    }

    public void assignRoleToChannel(RequestContext ctx, ID roleId, ID channelId) {
        channelService.assignToChannels(ctx, Role.class, roleId, List.of(channelId));
    }

    private List<Channel> getPermittedChannels(RequestContext ctx, List<ID> channelIds) {
        List<Channel> permittedChannels = new ArrayList<>();
        for (ID channelId : channelIds) {
            Channel channel = connection.getEntityOrThrow(ctx, Channel.class, channelId);
            boolean hasPermission = userHasPermissionOnChannel(ctx, channelId, Permission.CreateAdministrator);
            if (!hasPermission) {
                throw new Forbidden();
            }
            permittedChannels.add(channel);
        }
        return permittedChannels;
    }

    private void checkPermissionsAreValid(List<Permission> permissions) {
        if (permissions == null) {
            return;
        }
        List<Permission> allAssignablePermissions = getAllAssignablePermissions();
        for (Permission permission : permissions) {
            if (!allAssignablePermissions.contains(permission) || permission == Permission.SuperAdmin) {
                throw new UserInputError("error.permission-invalid", Map.of("permission", permission));
            }
        }
    }
    private void checkActiveUserHasSufficientPermissions(
            RequestContext ctx,
            List<Channel> targetChannels,
            List<Permission> permissions) {
        List<Permission> permissionsRequired = getChannelPermissions(List.of(
                new Role(unique(List.of(Permission.Authenticated, permissions)), targetChannels)
        ));
        for (ChannelPermissions channelPermissions : permissionsRequired) {
            boolean activeUserHasRequiredPermissions = userHasAllPermissionsOnChannel(
                    ctx,
                    channelPermissions.getId(),
                    channelPermissions.getPermissions()
            );
            if (!activeUserHasRequiredPermissions) {
                throw new UserInputError("error.active-user-does-not-have-sufficient-permissions");
            }
        }
    }

    private Role getRoleByCode(RequestContext ctx, String code) {
        EntityManager entityManager = ctx != null ? connection.getEntityManager() : connection.getRawEntityManager();
        return entityManager.createQuery(
                "SELECT r FROM Role r WHERE r.code = :code", Role.class)
                .setParameter("code", code)
                .getSingleResult();
    }

    private void ensureSuperAdminRoleExists() {
        List<Permission> assignablePermissions = getAllAssignablePermissions();
        try {
            Role superAdminRole = getSuperAdminRole();
            superAdminRole.setPermissions(assignablePermissions);
            connection.getRawEntityManager().merge(superAdminRole);
        } catch (EntityNotFoundException ex) {
            Channel defaultChannel = channelService.getDefaultChannel();
            createRoleForChannels(RequestContext.empty(),
                    new CreateRoleInput(SUPER_ADMIN_ROLE_CODE, SUPER_ADMIN_ROLE_DESCRIPTION, assignablePermissions),
                    List.of(defaultChannel));
        }
    }
    
    private void ensureCustomerRoleExists() {
        try {
            getCustomerRole();
        } catch (EntityNotFoundException ex) {
            Channel defaultChannel = channelService.getDefaultChannel();
            createRoleForChannels(RequestContext.empty(),
                    new CreateRoleInput(CUSTOMER_ROLE_CODE, CUSTOMER_ROLE_DESCRIPTION, List.of(Permission.Authenticated)),
                    List.of(defaultChannel));
        }
    }

    private void ensureRolesHaveValidPermissions() {
        List<Role> roles = connection.getRawEntityManager().createQuery(
                "SELECT r FROM Role r", Role.class)
                .getResultList();
        List<Permission> assignablePermissions = getAllAssignablePermissions();
        for (Role role : roles) {
            List<Permission> invalidPermissions = role.getPermissions().stream()
                    .filter(p -> !assignablePermissions.contains(p))
                    .collect(Collectors.toList());
            if (!invalidPermissions.isEmpty()) {
                role.setPermissions(role.getPermissions().stream()
                        .filter(assignablePermissions::contains)
                        .collect(Collectors.toList()));
                connection.getRawEntityManager().merge(role);
            }
        }
    }

    private Role createRoleForChannels(RequestContext ctx, CreateRoleInput input, List<Channel> channels) {
        Role role = new Role(input.getCode(), input.getDescription(), unique(List.of(Permission.Authenticated, input.getPermissions())));
        role.setChannels(channels);
        return connection.getEntityManager().merge(role);
    }

    private List<Permission> getAllAssignablePermissions() {
        return getAllPermissionsMetadata(configService.getAuthOptions().getCustomPermissions()).stream()
                .filter(p -> p.isAssignable())
                .map(p -> Permission.valueOf(p.getName()))
                .collect(Collectors.toList());
    }
}
