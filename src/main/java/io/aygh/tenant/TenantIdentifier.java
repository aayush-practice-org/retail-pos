package io.aygh.tenant;

import io.aygh.shared.UserHolder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which schema the current unit of work belongs to.
 * <p>
 * Consulted on every session open, so it must never throw and never block: it
 * reads the slug the authentication filter already put on the thread and falls
 * back to {@code public}. That fallback is what lets the login itself work —
 * credentials are checked before any tenant is known — and what keeps startup,
 * scheduled jobs and the super admin's own screens pointed at the shared tables.
 */
@Component
public class TenantIdentifier implements CurrentTenantIdentifierResolver<String> {

    public static final String DEFAULT_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String slug = UserHolder.getTenantSlug();
        return slug != null && !slug.isBlank() ? slug : DEFAULT_SCHEMA;
    }

    /**
     * False, because a session may legitimately outlive the request that set the
     * slug — validating it would fail those instead of letting them finish
     * against the schema they opened on.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
