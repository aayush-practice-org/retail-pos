package io.aygh.billing.service;

import io.aygh.identity.entity.Admin;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.shared.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The company details printed on this request's documents.
 * <p>
 * Read from the {@link Admin} row that owns the caller's tenant, which lives in
 * {@code public} — the mart's identity is registered once for the installation,
 * not copied into every tenant schema. The tenant id comes from
 * {@link UserHolder}, so it comes from the bearer token and never from anything
 * the caller can put on the wire: a client cannot ask for a bill printed under
 * another mart's name.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MartBrandingService {

    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public MartBranding resolve() {
        UUID tenantId = UserHolder.getTenantId();
        if (tenantId == null) {
            log.debug("No tenant on the request; printing with placeholder branding");
            return MartBranding.unknown();
        }

        return adminRepository.findWithUserById(tenantId)
                .map(this::toBranding)
                .orElseGet(() -> {
                    log.warn("No mart registered for tenantId={}; printing with placeholder branding", tenantId);
                    return MartBranding.unknown();
                });
    }

    private MartBranding toBranding(Admin admin) {
        return new MartBranding(
                admin.getCompanyName(),
                admin.getCompanyAddress(),
                admin.getCompanyPhone(),
                admin.getUser() == null ? null : admin.getUser().getEmail(),
                admin.getRegistrationNumber());
    }
}
