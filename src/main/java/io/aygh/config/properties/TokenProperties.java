package io.aygh.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.token")
public record TokenProperties(
        @NotBlank(message = "app.token.secret must be configured")
        @Size(min = 32, max = 32, message = "app.token.secret must be exactly 32 bytes for PASETO v4 local tokens")
        String secret,

        @Positive(message = "app.token.expiration-hours must be greater than zero")
        long expirationHours
) {
}