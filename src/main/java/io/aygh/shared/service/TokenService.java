package io.aygh.shared.service;

import io.aygh.config.properties.TokenProperties;
import io.aygh.exception.BusinessException;
import io.aygh.identity.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Issues and reads the bearer tokens the API authenticates with.
 * <p>
 * PASETO v4.local: the payload is encrypted with a server-side key, so unlike a
 * JWT it is opaque to the holder, and there is no algorithm field for an
 * attacker to talk the server out of. The key comes from {@code app.token.secret}
 * and must be exactly 32 bytes — enforced at startup by
 * {@link TokenProperties}, because a service that boots with a short key and
 * fails on first login is far worse than one that refuses to boot.
 * <p>
 * {@link #decrypt} answers only "is this a token this server issued, and is it
 * still inside its window". Whether the account still exists, is still enabled
 * and still belongs to the tenant it claims is decided against the database on
 * every request by the authentication filter.
 */
@Service
@Slf4j
public class TokenService {

    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;
    private final Duration tokenLifetime;

    public TokenService(TokenProperties tokenProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.secretKey = new SecretKey(tokenProperties.secret().getBytes(StandardCharsets.UTF_8), Version.V4);
        this.tokenLifetime = Duration.ofHours(tokenProperties.expirationHours());
    }

    /**
     * The claims for {@code user}, valid from now for the configured lifetime.
     */
    public AppToken issue(User user) {
        Instant now = Instant.now();
        return AppToken.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .tenantId(user.getTenantId())
                .tenantSlug(user.getTenantSlug())
                .issuedAtEpochSeconds(now.getEpochSecond())
                .expiresAtEpochSeconds(now.plus(tokenLifetime).getEpochSecond())
                .accountExpiresAtEpochSeconds(
                        user.getExpiresAt() == null ? 0L : user.getExpiresAt().getEpochSecond())
                .build();
    }

    public String encrypt(AppToken appToken) {
        try {
            return Paseto.encrypt(secretKey, objectMapper.writeValueAsString(appToken), "");
        } catch (Exception e) {
            // The cause can carry key material in its message, so it stays in the
            // log and never reaches the client.
            log.error("Failed to encrypt an authentication token", e);
            throw new BusinessException("Failed to generate authentication token");
        }
    }

    /**
     * The claims inside {@code token}, or empty if it was not issued by this
     * server, has been tampered with, or has expired. All three are the same
     * answer to a caller — telling them apart only helps someone probing.
     */
    public Optional<AppToken> decrypt(String token) {
        try {
            AppToken appToken = objectMapper.readValue(Paseto.decrypt(secretKey, token, ""), AppToken.class);
            return appToken.isExpired(Instant.now()) ? Optional.empty() : Optional.of(appToken);
        } catch (Exception e) {
            log.debug("Rejected an unreadable or expired token", e);
            return Optional.empty();
        }
    }

    public Duration tokenLifetime() {
        return tokenLifetime;
    }
}
