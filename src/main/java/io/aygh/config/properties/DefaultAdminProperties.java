package io.aygh.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The owner account seeded on first start, so a fresh mart has someone who can log in.
 */
@ConfigurationProperties(prefix = "app.default-admin")
public record DefaultAdminProperties(
        Boolean enabled,
        String username,
        String password,
        String email
) {

    public DefaultAdminProperties {
        enabled = enabled == null || enabled;
        username = username == null || username.isBlank() ? "admin" : username;
        password = password == null || password.isBlank() ? "admin" : password;
        email = email == null || email.isBlank() ? "admin@mart.local" : email;
    }
}
