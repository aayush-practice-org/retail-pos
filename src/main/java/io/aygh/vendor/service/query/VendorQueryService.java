package io.aygh.vendor.service.query;

import io.aygh.shared.response.PagedResponse;
import io.aygh.vendor.dto.response.VendorResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorQueryService {

    VendorResponse findById(Long id);

    PagedResponse<VendorResponse> findAll(String search, Pageable pageable);

    List<VendorResponse> findAllForSelection();
}
