package io.aygh.identity.service.command.impl;

import io.aygh.identity.dto.request.AdminCreateRequest;
import io.aygh.identity.dto.request.AdminUpdateRequest;
import io.aygh.identity.dto.request.ResetPasswordRequest;
import io.aygh.identity.dto.response.AdminResponse;
import io.aygh.identity.entity.Admin;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import io.aygh.identity.helper.AdminResolver;
import io.aygh.identity.helper.AdminValidation;
import io.aygh.identity.helper.UserValidation;
import io.aygh.identity.mapper.AdminMapper;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.command.AdminCommandService;
import io.aygh.tenant.TenantSchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCommandServiceImpl implements AdminCommandService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final AdminResolver adminResolver;
    private final AdminValidation adminValidation;
    private final UserValidation userValidation;
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final TenantSchemaService tenantSchemaService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public AdminResponse createAdmin(AdminCreateRequest request) {
        UUID adminId = register(request);
        return provisionAdmin(adminId);
    }

    @Override
    public AdminResponse provisionAdmin(UUID id) {
        String slug = transactionTemplate.execute(status -> adminResolver.byId(id).getSlug());

        try {
            tenantSchemaService.provision(slug);
            return transactionTemplate.execute(status -> {
                Admin admin = adminResolver.byId(id);
                admin.markProvisioned();
                return adminMapper.toResponse(adminRepository.save(admin));
            });
        } catch (RuntimeException e) {
            log.error("Could not provision schema '{}' for admin {}", slug, id, e);
            return transactionTemplate.execute(status -> {
                Admin admin = adminResolver.byId(id);
                admin.markProvisioningFailed(rootMessage(e));
                return adminMapper.toResponse(adminRepository.save(admin));
            });
        }
    }

    @Override
    @Transactional
    public AdminResponse updateAdmin(UUID id, AdminUpdateRequest request) {
        Admin admin = adminResolver.byId(id);
        User user = admin.getUser();

        userValidation.requireUsernameAvailable(request.username(), user.getId());
        userValidation.requireEmailAvailable(request.email(), user.getId());
        adminValidation.requireCompanyNameAvailable(request.companyName(), id);
        adminValidation.requireRegistrationNumberAvailable(request.registrationNumber(), id);

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setMobileNumber(request.mobileNumber());
        // The account lapses with the subscription: one date, so an extension
        // cannot be applied to the mart and forgotten on the login.
        user.setExpiresAt(request.subscriptionExpiresAt());

        admin.setCompanyName(request.companyName());
        admin.setCompanyAddress(request.companyAddress());
        admin.setCompanyPhone(request.companyPhone());
        admin.setRegistrationNumber(request.registrationNumber());
        admin.setSubscriptionExpiresAt(request.subscriptionExpiresAt());

        log.info("Updated mart '{}' ({})", admin.getCompanyName(), admin.getSlug());
        return adminMapper.toResponse(adminRepository.save(admin));
    }

    @Override
    @Transactional
    public void resetAdminPassword(UUID id, ResetPasswordRequest request) {
        Admin admin = adminResolver.byId(id);
        admin.getUser().setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(admin.getUser());
        log.info("Super admin reset the password for mart admin '{}'", admin.getUser().getUsername());
    }

    @Override
    @Transactional
    public void deleteAdmin(UUID id) {
        Admin admin = adminResolver.byId(id);
        String companyName = admin.getCompanyName();
        String slug = admin.getSlug();

        adminRepository.delete(admin);

        // Every account in the mart carries its admin's id as a tenant id, so this
        // takes the staff and the admin's own account together. They would
        // otherwise keep signing in to a mart that no longer has an owner.
        int accounts = userRepository.softDeleteByTenantId(id);

        log.warn("Retired mart '{}' ({}) along with {} account(s). "
                        + "Schema '{}' was left in place and still holds its data.",
                companyName, id, accounts, slug);
    }

    @Override
    public List<String> migrateAllTenants() {
        List<String> slugs = transactionTemplate.execute(status -> adminRepository.findAllSlugs());
        log.info("Running tenant migrations across {} mart(s)", slugs.size());
        return tenantSchemaService.migrateAll(slugs);
    }


    private UUID register(AdminCreateRequest request) {
        return transactionTemplate.execute(status -> {
            userValidation.requireUsernameAvailable(request.username(), null);
            userValidation.requireEmailAvailable(request.email(), null);
            adminValidation.requireCompanyNameAvailable(request.companyName(), null);
            adminValidation.requireRegistrationNumberAvailable(request.registrationNumber(), null);

            String slug = adminValidation.resolveSlug(request.slug(), request.companyName());

            // The admin's own id is the tenant id its staff will carry, so it is
            // assigned here rather than by the database — the row cannot be
            // written until the value is known.
            UUID adminId = UUID.randomUUID();

            User user = userRepository.save(User.builder()
                    .id(adminId)
                    .username(request.username())
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .fullName(request.fullName())
                    .mobileNumber(request.mobileNumber())
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .expiresAt(request.subscriptionExpiresAt())
                    .tenantId(adminId)
                    .tenantSlug(slug)
                    .build());

            adminRepository.save(Admin.builder()
                    .user(user)
                    .companyName(request.companyName())
                    .companyAddress(request.companyAddress())
                    .companyPhone(request.companyPhone())
                    .registrationNumber(request.registrationNumber())
                    .slug(slug)
                    .subscriptionExpiresAt(request.subscriptionExpiresAt())
                    .build());

            log.info("Registered mart '{}' as schema '{}' with admin '{}'",
                    request.companyName(), slug, request.username());
            return adminId;
        });
    }

    /**
     * The innermost message, which is the one that actually says what went wrong.
     */
    private static String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }
}
