package io.aygh.config;

import io.aygh.tenant.TenantConnectionProvider;
import io.aygh.tenant.TenantIdentifier;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class HibernateMultiTenantConfig {

    private final TenantConnectionProvider connectionProvider;
    private final TenantIdentifier tenantIdentifier;

    @Bean
    public HibernatePropertiesCustomizer multiTenancyCustomizer() {
        return properties -> {
            properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifier);
        };
    }
}
