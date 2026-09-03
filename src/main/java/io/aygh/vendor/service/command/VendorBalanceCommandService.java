package io.aygh.vendor.service.command;

import io.aygh.vendor.dto.request.VendorLedgerEntryRequest;
import io.aygh.vendor.dto.request.VendorSettlementRequest;
import io.aygh.vendor.dto.response.VendorBalanceResponse;

public interface VendorBalanceCommandService {

    /** Posts a payable or a receivable. Rejects a settlement — that has its own path. */
    VendorBalanceResponse post(Long vendorId, VendorLedgerEntryRequest request);

    /** Records money paid to the vendor. */
    VendorBalanceResponse settle(Long vendorId, VendorSettlementRequest request);
}
