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

/**
 * Seeds the super admin on first start, so a fresh installation has someone who
 * can register the first mart.
 * <p>
 * It runs on every boot and does nothing on all but the first: the account is
 * created only when the installation has no super admin at all, and an existing
 * one is never touched — not its password, not its status. That matters, because
 * a seeder that reset it on each restart would quietly undo whatever the owner
 * had changed. The database backs the same rule up with a partial unique index,
 * so two of these can never exist even if two instances start at once.
 * <p>
 * The account belongs to no tenant: its {@code tenantId} and {@code tenantSlug}
 * stay null, so its requests are served from {@code public}, which is where the
 * shared identity tables and the mart register live.
 * <p>
 * Credentials come from {@code app.default-admin}. Leaving them at their
 * defaults is fine on a laptop and is logged loudly anywhere else.
 */
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
        if (!defaultAdmin.enabled()) {
            log.info("Super admin seeding is disabled (app.default-admin.enabled=false)");
            return;
        }

        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.debug("Super admin already present — nothing to seed");
            return;
        }

        // A leftover account under the seeded name would collide with the unique
        // index on username, so say why rather than letting the insert fail.
        if (userRepository.existsByUsernameIgnoreCase(defaultAdmin.username())) {
            log.error("Cannot seed the super admin: username '{}' is already taken by another account. "
                    + "Set app.default-admin.username to something else.", defaultAdmin.username());
            return;
        }

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .username(defaultAdmin.username())
                .email(defaultAdmin.email())
                .password(passwordEncoder.encode(defaultAdmin.password()))
                .fullName(defaultAdmin.fullName())
                .role(UserRole.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        if (defaultAdmin.usesDefaultPassword()) {
            log.warn("Seeded super admin '{}' with the built-in default password — "
                            + "change it before this install goes anywhere near production",
                    defaultAdmin.username());
        } else {
            log.info("Seeded super admin '{}'", defaultAdmin.username());
        }
    }
}
