package io.aygh.vendor.service.query.impl;

import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import io.aygh.vendor.dto.response.VendorHistoryResponse;
import io.aygh.vendor.helper.VendorResolver;
import io.aygh.vendor.mapper.VendorHistoryMapper;
import io.aygh.vendor.repository.VendorHistoryRepository;
import io.aygh.vendor.service.query.VendorHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorHistoryQueryServiceImpl implements VendorHistoryQueryService {

    private final VendorHistoryRepository vendorHistoryRepository;
    private final VendorResolver resolver;
    private final VendorHistoryMapper vendorHistoryMapper;

    @Override
    public VendorHistoryResponse findById(Long id) {
        return vendorHistoryMapper.toResponse(resolver.history(id));
    }

    @Override
    public PagedResponse<VendorHistoryResponse> findAll(Pageable pageable) {
        Page<VendorHistoryResponse> page = vendorHistoryRepository.findAllBy(pageable)
                .map(vendorHistoryMapper::toResponse);

        return PaginationUtils.toPagedResponse(page);
    }

    @Override
    public PagedResponse<VendorHistoryResponse> findByVendorId(Long vendorId, Pageable pageable) {
        resolver.requireVendor(vendorId);

        Page<VendorHistoryResponse> page = vendorHistoryRepository.findByVendorId(vendorId, pageable)
                .map(vendorHistoryMapper::toResponse);

        return PaginationUtils.toPagedResponse(page);
    }
}
