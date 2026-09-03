package io.aygh.vendor.helper;

import io.aygh.exception.BusinessException;
import io.aygh.vendor.entity.BalanceType;
import io.aygh.vendor.repository.VendorBalanceRepository;
import io.aygh.vendor.repository.VendorHistoryRepository;
import io.aygh.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The rules the entities cannot state for themselves.
 * <p>
 * Uniqueness is checked here <em>and</em> backed by a partial unique index in
 * the migration. The check is what produces a readable message; the index is
 * what actually holds under two concurrent requests.
 */
@Component
@RequiredArgsConstructor
public class VendorValidation {

    private final VendorRepository vendorRepository;
    private final VendorBalanceRepository vendorBalanceRepository;
    private final VendorHistoryRepository vendorHistoryRepository;

    public void requireNameAvailable(String name, Long excludingId) {
        boolean taken = excludingId == null
                ? vendorRepository.existsByNameIgnoreCase(name)
                : vendorRepository.existsByNameIgnoreCaseAndIdNot(name, excludingId);
        if (taken) {
            throw new BusinessException("A vendor named '" + name + "' already exists");
        }
    }

    /**
     * A PAN is optional, but two vendors sharing one means the mart's purchase
     * returns cannot be attributed — so it is unique wherever it is given.
     */
    public void requirePanAvailable(String panNumber, Long excludingId) {
        if (panNumber == null || panNumber.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? vendorRepository.existsByPanNumberIgnoreCase(panNumber)
                : vendorRepository.existsByPanNumberIgnoreCaseAndIdNot(panNumber, excludingId);
        if (taken) {
            throw new BusinessException("PAN '" + panNumber + "' is already registered to another vendor");
        }
    }

    /**
     * A vendor with money or purchases against it is part of the mart's records,
     * not a mistyped row someone is tidying up. Removing it would orphan a ledger
     * whose balance nothing could then explain.
     */
    public void requireRemovable(Long vendorId) {
        if (vendorBalanceRepository.existsByVendorId(vendorId)) {
            throw new BusinessException(
                    "This vendor has ledger entries against it and cannot be removed");
        }
        long purchases = vendorHistoryRepository.countByVendorId(vendorId);
        if (purchases > 0) {
            throw new BusinessException(
                    "This vendor has " + purchases + " purchase(s) recorded against it and cannot be removed");
        }
    }

    /**
     * Settlements are money leaving the till and are posted through their own
     * endpoint, which is guarded more narrowly. Letting one in here would route
     * a payment around that guard.
     */
    public void rejectSettlementEntry(BalanceType balanceType) {
        if (balanceType == BalanceType.SETTLEMENT) {
            throw new BusinessException(
                    "Settlements are recorded through the settlement endpoint, not as a ledger entry");
        }
    }
}
