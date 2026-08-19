package io.aygh.identity.service.auth;

import io.aygh.exception.BusinessException;
import io.aygh.identity.dto.request.LoginRequest;
import io.aygh.identity.dto.response.LoginResponse;
import io.aygh.identity.entity.User;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.token.UserToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAuthService {

    private final UserRepository userRepository;
    private final UserToken userToken;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Attempting login: {}", request.username());

        User user = fetchUser(request.username());

        match(request.password(), user.getPassword());

        if (!user.isActive()) {
            log.warn("Deactivated account attempted login: {}", user.getUsername());
            throw new BusinessException("This account has been deactivated. Contact your administrator.");
        }

        UserToken.Issued issued = userToken.generateToken(user);

        log.info("User {} successfully logged in as {}", user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .id(user.getId())
                .token(issued.token())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresAt(issued.expiresAt())
                .build();
    }

    private User fetchUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new BusinessException("Invalid username/email or password"));
    }

    private void match(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("Password mismatch on login attempt");
            throw new BusinessException("Invalid username/email or password");
        }
    }
}
