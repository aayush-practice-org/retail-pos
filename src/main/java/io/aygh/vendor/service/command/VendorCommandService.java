package io.aygh.vendor.service.command;

import io.aygh.vendor.dto.request.VendorRequest;
import io.aygh.vendor.dto.response.VendorResponse;

public interface VendorCommandService {

    VendorResponse create(VendorRequest request);

    VendorResponse update(Long id, VendorRequest request);

    /** Soft-removes a vendor nothing is recorded against. */
    void delete(Long id);
}
