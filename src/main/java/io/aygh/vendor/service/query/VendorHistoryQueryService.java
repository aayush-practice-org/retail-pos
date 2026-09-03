package io.aygh.vendor.service.query;

import io.aygh.shared.response.PagedResponse;
import io.aygh.vendor.dto.response.VendorHistoryResponse;
import org.springframework.data.domain.Pageable;

public interface VendorHistoryQueryService {

    VendorHistoryResponse findById(Long id);

    PagedResponse<VendorHistoryResponse> findAll(Pageable pageable);

    PagedResponse<VendorHistoryResponse> findByVendorId(Long vendorId, Pageable pageable);
}
