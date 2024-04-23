package com.dtme.marketplace.helper;
import com.vendure.common.types.LanguageCode;
import com.vendure.config.ApiOptions;
import com.vendure.config.AssetOptions;
import com.vendure.config.AuthOptions;
import com.vendure.config.CatalogOptions;
import com.vendure.config.EntityOptions;
import com.vendure.config.ImportExportOptions;
import com.vendure.config.JobQueueOptions;
import com.vendure.config.OrderOptions;
import com.vendure.config.PaymentOptions;
import com.vendure.config.PromotionOptions;
import com.vendure.config.RuntimeVendureConfig;
import com.vendure.config.ShippingOptions;
import com.vendure.config.SystemOptions;
import com.vendure.config.TaxOptions;
import com.vendure.config.VendureConfig;
import com.vendure.custom.CustomFields;
import com.vendure.entity.EntityIdStrategy;
import com.vendure.logger.Logger;
import com.vendure.logger.VendureLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.typeorm.DataSourceOptions;

import java.util.List;

@Service
public class ConfigService implements VendureConfig {
    private RuntimeVendureConfig activeConfig;

    @Autowired
    public ConfigService() {
        this.activeConfig = getConfig();
        if (this.activeConfig.getAuthOptions().isDisableAuth()) {
            Logger.warn("Auth has been disabled. This should never be the case for a production system!");
        }
    }

    @Override
    public ApiOptions getApiOptions() {
        return this.activeConfig.getApiOptions();
    }

    @Override
    public AuthOptions getAuthOptions() {
        return this.activeConfig.getAuthOptions();
    }

    @Override
    public CatalogOptions getCatalogOptions() {
        return this.activeConfig.getCatalogOptions();
    }

    @Override
    public String getDefaultChannelToken() {
        return this.activeConfig.getDefaultChannelToken();
    }

    @Override
    public LanguageCode getDefaultLanguageCode() {
        return this.activeConfig.getDefaultLanguageCode();
    }

    @Override
    public EntityOptions getEntityOptions() {
        return this.activeConfig.getEntityOptions();
    }

    @Override
    public EntityIdStrategy<Object> getEntityIdStrategy() {
        return this.activeConfig.getEntityIdStrategy();
    }

    @Override
    public AssetOptions getAssetOptions() {
        return this.activeConfig.getAssetOptions();
    }

    @Override
    public DataSourceOptions getDbConnectionOptions() {
        return this.activeConfig.getDbConnectionOptions();
    }

    @Override
    public PromotionOptions getPromotionOptions() {
        return this.activeConfig.getPromotionOptions();
    }

    @Override
    public ShippingOptions getShippingOptions() {
        return this.activeConfig.getShippingOptions();
    }

    @Override
    public OrderOptions getOrderOptions() {
        return this.activeConfig.getOrderOptions();
    }

    @Override
    public PaymentOptions getPaymentOptions() {
        return this.activeConfig.getPaymentOptions();
    }

    @Override
    public TaxOptions getTaxOptions() {
        return this.activeConfig.getTaxOptions();
    }

    @Override
    public ImportExportOptions getImportExportOptions() {
        return this.activeConfig.getImportExportOptions();
    }

    @Override
    public CustomFields getCustomFields() {
        return this.activeConfig.getCustomFields();
    }

    @Override
    public List<Object> getPlugins() {
        return this.activeConfig.getPlugins();
    }

    @Override
    public VendureLogger getLogger() {
        return this.activeConfig.getLogger();
    }

    @Override
    public JobQueueOptions getJobQueueOptions() {
        return this.activeConfig.getJobQueueOptions();
    }

    @Override
    public SystemOptions getSystemOptions() {
        return this.activeConfig.getSystemOptions();
    }

    private static RuntimeVendureConfig getConfig() {
        // Implementation of getConfig() method
    }
}

