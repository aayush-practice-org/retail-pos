package io.aygh.identity.helper;

import io.aygh.exception.BusinessException;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.tenant.TenantSlug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The rules a mart registration has to satisfy before its schema is built.
 */
@Component
@RequiredArgsConstructor
public class AdminValidation {

    private final AdminRepository adminRepository;

    public void requireCompanyNameAvailable(String companyName, UUID excludingId) {
        boolean taken = excludingId == null
                ? adminRepository.existsByCompanyNameIgnoreCase(companyName)
                : adminRepository.existsByCompanyNameIgnoreCaseAndIdNot(companyName, excludingId);

        if (taken) {
            throw new BusinessException("A mart is already registered under the name '" + companyName + "'");
        }
    }

    public void requireRegistrationNumberAvailable(String registrationNumber, UUID excludingId) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? adminRepository.existsByRegistrationNumberIgnoreCase(registrationNumber)
                : adminRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(registrationNumber, excludingId);

        if (taken) {
            throw new BusinessException("Registration number already in use: " + registrationNumber);
        }
    }


    public String resolveSlug(String requestedSlug, String companyName) {
        String slug = requestedSlug == null || requestedSlug.isBlank()
                ? TenantSlug.from(companyName)
                : TenantSlug.requireValid(requestedSlug.trim().toLowerCase());

        if (adminRepository.slugTaken(slug)) {
            throw new BusinessException(
                    "The schema name '" + slug + "' is already in use — choose a different company name or slug");
        }
        return slug;
    }
}
