package io.aygh.identity.service.command.impl;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.identity.dto.request.CbmsInternalRequest;
import io.aygh.identity.dto.request.CbmsInternalUpdateRequest;
import io.aygh.identity.dto.response.CbmsInternalResponse;
import io.aygh.identity.entity.Admin;
import io.aygh.identity.entity.CbmsInternalEntity;
import io.aygh.identity.mapper.CbmsInternalMapper;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.identity.repository.CbmsInternalRepository;
import io.aygh.identity.service.command.CbmsInternalCommandService;
import io.aygh.shared.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class CbmsInternalCommandServiceImpl implements CbmsInternalCommandService {

    private final CbmsInternalRepository cbmsInternalRepository;
    private final CbmsInternalMapper cbmsInternalMapper;
    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public CbmsInternalResponse createCbmsInternal(CbmsInternalRequest request) {
        UUID tenantId = UserHolder.getTenantId();
        String tenantSlug = UserHolder.getTenantSlug();

        Admin admin = adminRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "id", tenantId));

        CbmsInternalEntity entity = new CbmsInternalEntity();
        entity.setTenantId(tenantId);
        entity.setTenantSlug(tenantSlug);
        entity.setCbmsUsername(request.cbmsUsername());
        entity.setCbmsPassword(request.cbmsPassword());
        entity.setTaxRegistration(request.taxRegistration());
        entity.setTaxIncluded(request.taxIncluded());
        entity.setPan(admin.getRegistrationNumber());

        CbmsInternalEntity saved = cbmsInternalRepository.save(entity);
        log.info("Created CBMS internal configuration id={} for tenant {}", saved.getId(), tenantSlug);

        return cbmsInternalMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CbmsInternalResponse updateCbmsInternal(Long id, CbmsInternalUpdateRequest request) {
        UUID tenantId = UserHolder.getTenantId();
        Admin admin = adminRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "id", tenantId));

        CbmsInternalEntity entity = cbmsInternalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CbmsInternal", "id", id));

        entity.setCbmsUsername(request.cbmsUsername());
        entity.setCbmsPassword(request.cbmsPassword());
        entity.setTaxRegistration(request.taxRegistration());
        entity.setTaxIncluded(request.taxIncluded());
        entity.setPan(admin.getRegistrationNumber());

        CbmsInternalEntity updated = cbmsInternalRepository.save(entity);
        log.info("Updated CBMS internal configuration id={}", updated.getId());

        return cbmsInternalMapper.toResponse(updated);
    }
}
