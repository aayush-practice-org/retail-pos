package io.aygh.vendor.service.command;

import io.aygh.vendor.dto.response.VendorHistoryResponse;

/**
 * The seam purchasing writes through.
 * <p>
 * There is no controller behind this: a vendor's purchase trail is a
 * consequence of recording a purchase, never something typed in on its own. It
 * is an interface rather than a repository call from the purchase module so
 * that module depends on what vendors offer, not on how they store it.
 */
public interface VendorHistoryCommandService {

    VendorHistoryResponse record(Long vendorId, Long purchaseId);
}
