package io.aygh.vendor.service.query;

import io.aygh.shared.response.PagedResponse;
import io.aygh.vendor.dto.response.VendorBalanceResponse;
import io.aygh.vendor.dto.response.VendorBalanceSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface VendorBalanceQueryService {

    /** The vendor's ledger, newest first by default. */
    PagedResponse<VendorBalanceResponse> findTransactions(Long vendorId, Pageable pageable);

    /** Where the account stands, and the totals behind it. */
    VendorBalanceSummaryResponse findSummary(Long vendorId);
}
