package io.aygh.config.properties;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class DefaultAdminProperties {
    private final Boolean enabled;
    private final String username;
    private final String password;
    private final String email;
    private final String fullName;

    public DefaultAdminProperties() {
        enabled = true;
        username = "superadmin";
        password = "superadmin";
        email = "superadmin@mart.local";
        fullName = "System Owner";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
