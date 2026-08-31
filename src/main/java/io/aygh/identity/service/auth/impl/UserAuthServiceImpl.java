package io.aygh.identity.service.auth.impl;

import io.aygh.exception.BusinessException;
import io.aygh.identity.dto.request.ChangePasswordRequest;
import io.aygh.identity.dto.request.LoginRequest;
import io.aygh.identity.dto.response.LoginResponse;
import io.aygh.identity.entity.Admin;
import io.aygh.identity.entity.User;
import io.aygh.identity.helper.UserResolver;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.auth.UserAuthService;
import io.aygh.shared.service.AppToken;
import io.aygh.shared.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthServiceImpl implements UserAuthService {

    private static final String REJECTED = "Invalid username or password";


    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final UserResolver userResolver;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = findCandidate(request.username());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Sign-in attempt with a wrong password for '{}'", user.getUsername());
            throw new BusinessException(REJECTED);
        }
        validateUserAccess(user);
        AppToken claims = tokenService.issue(user);
        String token = tokenService.encrypt(claims);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("'{}' ({}) signed in to tenant '{}'", user.getUsername(), user.getRole(), user.getTenantSlug());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .tenantSlug(user.getTenantSlug())
                .companyName(companyNameOf(user))
                .token(token)
                .tokenExpiresAt(claims.expiresAt())
                .accountExpiresAt(user.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        User user = userResolver.current();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("The new password must differ from the current one");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("'{}' changed their own password", user.getUsername());
    }


    private User findCandidate(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(username).orElseThrow(
                        () -> new BusinessException(REJECTED)
                ));
    }

    private void validateUserAccess(User user) {
        if (!user.isAccountNonExpired()) {
            log.warn("Sign-in refused: account '{}' expired at {}", user.getUsername(), user.getExpiresAt());
            throw new BusinessException("This account has expired — contact your administrator");
        }
        if (!user.isEnabled()) {
            log.warn("Sign-in refused: account '{}' is {}", user.getUsername(), user.getStatus());
            throw new BusinessException(REJECTED);
        }
    }


    private String companyNameOf(User user) {
        return user.getTenantId() == null
                ? null
                : adminRepository.findById(user.getTenantId()).map(Admin::getCompanyName).orElse(null);
    }
}
