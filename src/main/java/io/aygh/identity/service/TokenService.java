package io.aygh.identity.service;

import io.aygh.config.properties.TokenProperties;
import io.aygh.exception.BusinessException;
import io.aygh.security.token.AppToken;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class TokenService {

    private final TokenProperties tokenProperties;
    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;

    public TokenService(TokenProperties tokenProperties, ObjectMapper objectMapper) {
        this.tokenProperties = tokenProperties;
        this.objectMapper = objectMapper;
        this.secretKey = new SecretKey(tokenProperties.secret().getBytes(StandardCharsets.UTF_8), Version.V4);
    }

    public Optional<String> encrypt(AppToken appToken) {
        try {
            String payload = objectMapper.writeValueAsString(appToken);
            return Optional.of(Paseto.encrypt(secretKey, payload, ""));
        } catch (Exception e) {
            throw new BusinessException("Failed to generate authentication token");
        }
    }

    public Optional<AppToken> decrypt(String token) {
        try {
            String payload = Paseto.decrypt(secretKey, token, "");
            AppToken appToken = objectMapper.readValue(payload, AppToken.class);
            if (System.currentTimeMillis() > appToken.getExpiresDate().toEpochMilli()) {
                return Optional.empty();
            }
            return Optional.of(appToken);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public long expirationHours() {
        return tokenProperties.expirationHours();
    }
}
