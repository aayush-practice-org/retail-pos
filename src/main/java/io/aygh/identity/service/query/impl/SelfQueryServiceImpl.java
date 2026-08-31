package io.aygh.identity.service.query.impl;

import io.aygh.identity.dto.response.SelfResponse;
import io.aygh.identity.entity.Admin;
import io.aygh.identity.entity.User;
import io.aygh.identity.helper.UserResolver;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.identity.service.query.SelfQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelfQueryServiceImpl implements SelfQueryService {

    private final UserResolver userResolver;
    private final AdminRepository adminRepository;

    @Override
    public SelfResponse currentUser() {
        User user = userResolver.current();

        return SelfResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .tenantId(user.getTenantId())
                .tenantSlug(user.getTenantSlug())
                .companyName(user.getTenantId() == null
                        ? null
                        : adminRepository.findById(user.getTenantId()).map(Admin::getCompanyName).orElse(null))
                .expiresAt(user.getExpiresAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
