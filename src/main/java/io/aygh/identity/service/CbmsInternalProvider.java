package io.aygh.identity.service;

import io.aygh.identity.entity.Admin;
import io.aygh.identity.entity.CbmsInternalEntity;
import io.aygh.identity.entity.TaxRegistration;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.identity.repository.CbmsInternalRepository;
import io.aygh.shared.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The tenant's CBMS setup, created on first use.
 * <p>
 * A mart that has never been through the CBMS screen is PAN registered, charges
 * no VAT and bills under the registration number it signed up with.
 */
@Component
@RequiredArgsConstructor
public class CbmsInternalProvider {

    private final CbmsInternalRepository repository;
    private final AdminRepository adminRepository;

    /**
     * In a transaction of its own so the default row survives a sale that rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CbmsInternalEntity getOrCreate() {
        return repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> repository.save(defaultSetup()));
    }

    private CbmsInternalEntity defaultSetup() {
        UUID tenantId = UserHolder.getTenantId();
        String pan = tenantId == null ? null : adminRepository.findById(tenantId)
                .map(Admin::getRegistrationNumber)
                .orElse(null);

        return CbmsInternalEntity.builder()
                .tenantId(tenantId)
                .tenantSlug(UserHolder.getTenantSlug())
                .taxRegistration(TaxRegistration.PAN_REGISTERED)
                .pan(pan)
                .build();
    }
}
