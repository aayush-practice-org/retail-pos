package io.aygh.vendor.service.command.impl;

import io.aygh.vendor.dto.request.VendorRequest;
import io.aygh.vendor.dto.response.VendorResponse;
import io.aygh.vendor.entity.Vendor;
import io.aygh.vendor.helper.VendorResolver;
import io.aygh.vendor.helper.VendorValidation;
import io.aygh.vendor.mapper.VendorMapper;
import io.aygh.vendor.repository.VendorRepository;
import io.aygh.vendor.service.command.VendorCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorCommandServiceImpl implements VendorCommandService {

    private final VendorRepository vendorRepository;
    private final VendorResolver resolver;
    private final VendorValidation validation;
    private final VendorMapper vendorMapper;

    @Override
    public VendorResponse create(VendorRequest request) {
        validation.requireNameAvailable(request.name(), null);
        validation.requirePanAvailable(request.panNumber(), null);

        Vendor saved = vendorRepository.save(vendorMapper.toEntity(request));
        log.info("Created vendor '{}'", saved.getName());
        return vendorMapper.toResponse(saved);
    }

    @Override
    public VendorResponse update(Long id, VendorRequest request) {
        Vendor vendor = resolver.vendor(id);

        validation.requireNameAvailable(request.name(), id);
        validation.requirePanAvailable(request.panNumber(), id);
        vendorMapper.applyUpdate(request, vendor);

        Vendor saved = vendorRepository.save(vendor);
        log.info("Updated vendor '{}'", saved.getName());
        return vendorMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Vendor vendor = resolver.vendor(id);

        validation.requireRemovable(id);

        vendorRepository.delete(vendor);
        log.info("Removed vendor '{}'", vendor.getName());
    }
}
