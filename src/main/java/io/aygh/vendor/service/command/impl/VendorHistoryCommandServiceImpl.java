package io.aygh.vendor.service.command.impl;

import io.aygh.vendor.dto.response.VendorHistoryResponse;
import io.aygh.vendor.entity.Vendor;
import io.aygh.vendor.entity.VendorHistory;
import io.aygh.vendor.helper.VendorResolver;
import io.aygh.vendor.mapper.VendorHistoryMapper;
import io.aygh.vendor.repository.VendorHistoryRepository;
import io.aygh.vendor.service.command.VendorHistoryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorHistoryCommandServiceImpl implements VendorHistoryCommandService {

    private final VendorHistoryRepository vendorHistoryRepository;
    private final VendorResolver resolver;
    private final VendorHistoryMapper vendorHistoryMapper;

    @Override
    public VendorHistoryResponse record(Long vendorId, Long purchaseId) {
        Vendor vendor = resolver.vendor(vendorId);

        VendorHistory saved = vendorHistoryRepository.save(VendorHistory.builder()
                .vendor(vendor)
                .purchaseId(purchaseId)
                .build());

        log.info("Recorded purchase {} against vendor '{}'", purchaseId, vendor.getName());
        return vendorHistoryMapper.toResponse(saved);
    }
}
