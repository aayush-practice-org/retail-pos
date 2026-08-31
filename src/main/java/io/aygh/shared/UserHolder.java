package io.aygh.shared;

import io.aygh.exception.BusinessException;
import io.aygh.identity.entity.UserRole;

import java.util.UUID;

/**
 * Who is making the current request, and which tenant schema it belongs to.
 * <p>
 * Populated by {@code PasetoAuthenticationFilter} from the bearer token before
 * anything touches the database, because the tenant slug held here is what
 * {@link io.aygh.tenant.TenantIdentifier} hands Hibernate to pick the schema. A
 * request that reaches a repository with this empty is served from
 * {@code public} — correct for the shared identity tables, and empty for
 * everything else.
 * <p>
 * The values live in thread locals, so the filter clears them in a
 * {@code finally} block: pooled request threads are reused, and a slug left
 * behind would route the next caller's queries into the previous caller's
 * schema.
 */
public final class UserHolder {

    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<UserRole> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_SLUG = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void setUserId(UUID userId) {
        USER_ID.set(userId);
    }

    public static UUID getUserId() {
        return USER_ID.get();
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void setRole(UserRole role) {
        ROLE.set(role);
    }

    public static UserRole getRole() {
        return ROLE.get();
    }

    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static UUID getTenantId() {
        return TENANT_ID.get();
    }

    public static void setTenantSlug(String slug) {
        TENANT_SLUG.set(slug);
    }

    public static String getTenantSlug() {
        return TENANT_SLUG.get();
    }

    public static boolean isSuperAdmin() {
        return ROLE.get() == UserRole.SUPER_ADMIN;
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
        TENANT_ID.remove();
        TENANT_SLUG.remove();
    }
}
