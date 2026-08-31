package io.aygh.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The super admin account seeded on first start, so a fresh mart has someone who
 * can sign in and hire everyone else. Bound from {@code app.default-admin}.
 * <p>
 * The defaults here are deliberately obvious ones: they are meant to be
 * overridden by environment variables anywhere that is not a developer's laptop,
 * and {@code SuperadminDataInitializer} logs a warning when they are still in
 * use.
 */
@ConfigurationProperties(prefix = "app.default-admin")
public record DefaultAdminProperties(
        Boolean enabled,
        String username,
        String password,
        String email,
        String fullName
) {
    public static final String DEFAULT_PASSWORD = "admin";

    public DefaultAdminProperties {
        enabled = enabled == null || enabled;
        username = isBlank(username) ? "admin" : username;
        password = isBlank(password) ? DEFAULT_PASSWORD : password;
        email = isBlank(email) ? "admin@mart.local" : email;
        fullName = isBlank(fullName) ? "Mart Owner" : fullName;
    }

    /** Whether the seeded password is still the built-in one — worth shouting about. */
    public boolean usesDefaultPassword() {
        return DEFAULT_PASSWORD.equals(password);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
