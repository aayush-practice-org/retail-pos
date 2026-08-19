package io.aygh.config;

import io.aygh.config.properties.DefaultAdminProperties;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 *  Seeds the owner account on first start, so a fresh mart has someone who can
 *  log in and hire everyone else. It runs only when the mart has no ADMIN at all —
 *  once there is one, this never touches the database again.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultAdminProperties defaultAdmin;

    @Bean
    public CommandLineRunner seedDefaultAdmin() {
        return args -> {
            if (!defaultAdmin.enabled()) {
                return;
            }

            if (userRepository.countByRole(UserRole.ADMIN) > 0) {
                return;
            }

            User admin = User.builder()
                    .username(defaultAdmin.username())
                    .fullName("Mart Owner")
                    .email(defaultAdmin.email())
                    .password(passwordEncoder.encode(defaultAdmin.password()))
                    .role(UserRole.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(admin);

            log.warn("Seeded default ADMIN '{}' — change its password before going live",
                    defaultAdmin.username());
        };
    }
}
