package io.aygh.identity.bootstrap;

import io.aygh.config.properties.DefaultAdminProperties;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import io.aygh.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultAdminProperties defaultAdmin;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.debug("Super admin already present — nothing to seed");
            return;
        }

        // A leftover account under the seeded name would collide with the unique
        // index on username, so say why rather than letting the insert fail.
        if (userRepository.existsByUsernameIgnoreCase(defaultAdmin.getUsername())) {
            log.error("Cannot seed the super admin: username '{}' is already taken by another account. "
                    + "Set app.default-admin.username to something else.", defaultAdmin.getUsername());
            return;
        }

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .username(defaultAdmin.getUsername())
                .email(defaultAdmin.getEmail())
                .password(passwordEncoder.encode(defaultAdmin.getPassword()))
                .fullName(defaultAdmin.getFullName())
                .role(UserRole.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

    }
}
