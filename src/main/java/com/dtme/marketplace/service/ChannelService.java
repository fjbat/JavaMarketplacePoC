package com.dtme.marketplace.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChannelService {

    private final TransactionalConnection connection;
    private final ConfigService configService;
    private final GlobalSettingsService globalSettingsService;
    private final CustomFieldRelationService customFieldRelationService;
    private final EventBus eventBus;
    private final ListQueryBuilder listQueryBuilder;

    private SelfRefreshingCache<List<Channel>, RequestContext> allChannels;

    @Autowired
    public ChannelService(TransactionalConnection connection,
                          ConfigService configService,
                          GlobalSettingsService globalSettingsService,
                          CustomFieldRelationService customFieldRelationService,
                          EventBus eventBus,
                          ListQueryBuilder listQueryBuilder) {
        this.connection = connection;
        this.configService = configService;
        this.globalSettingsService = globalSettingsService;
        this.customFieldRelationService = customFieldRelationService;
        this.eventBus = eventBus;
        this.listQueryBuilder = listQueryBuilder;
    }

    public void initChannels() {
        ensureDefaultChannelExists();
        ensureCacheExists();
    }

    private void ensureDefaultChannelExists() {
        // Implement the logic to ensure the default channel exists
    }

    private void ensureCacheExists() {
        allChannels = createCache();
    }

    private SelfRefreshingCache<List<Channel>, RequestContext> createCache() {
        return SelfRefreshingCache.<List<Channel>, RequestContext>builder()
                .name("ChannelService.allChannels")
                .ttl(configService.getEntityOptions().getChannelCacheTtl())
                .refresh(ctx -> {
                    List<Channel> items = listQueryBuilder
                            .build(Channel.class, new FindOptionsWhere(), new ListQueryOptions())
                            .getMany();
                    return new SelfRefreshingCache.RefreshResult<>(items);
                })
                .build();
    }
}
