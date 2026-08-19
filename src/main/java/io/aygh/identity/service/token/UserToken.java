package io.aygh.identity.service.token;

import io.aygh.exception.BusinessException;
import io.aygh.identity.entity.User;
import io.aygh.identity.service.TokenService;
import io.aygh.security.token.AppToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserToken {

    private final TokenService tokenService;

    public Issued generateToken(User user) {
        Instant expiresAt = Instant.now().plus(Duration.ofHours(tokenService.expirationHours()));

        AppToken appToken = new AppToken();
        appToken.setUserId(user.getId());
        appToken.setUserName(user.getUsername());
        appToken.setRole(user.getRole().name());
        appToken.setExpiresAt(expiresAt.toEpochMilli());

        String token = tokenService.encrypt(appToken)
                .orElseThrow(() -> new BusinessException("Failed to generate authentication token"));

        return new Issued(token, expiresAt);
    }

    public record Issued(String token, Instant expiresAt) {
    }
}
