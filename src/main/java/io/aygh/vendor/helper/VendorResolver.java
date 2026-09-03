package io.aygh.vendor.helper;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.vendor.entity.Vendor;
import io.aygh.vendor.entity.VendorHistory;
import io.aygh.vendor.repository.VendorHistoryRepository;
import io.aygh.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loading vendor rows by id, with a 404 instead of an empty Optional.
 */
@Component
@RequiredArgsConstructor
public class VendorResolver {

    private final VendorRepository vendorRepository;
    private final VendorHistoryRepository vendorHistoryRepository;

    public Vendor vendor(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id));
    }

    /**
     * Fails on an unknown id without loading the row. Used by the ledger reads,
     * which need the vendor to exist but never look at it — answering an empty
     * page for a vendor that was never there would read as "no transactions yet".
     */
    public void requireVendor(Long id) {
        if (!vendorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Vendor", "id", id);
        }
    }

    public VendorHistory history(Long id) {
        return vendorHistoryRepository.findWithVendorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor history", "id", id));
    }
}
