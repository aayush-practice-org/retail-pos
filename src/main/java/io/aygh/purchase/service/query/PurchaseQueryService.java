package io.aygh.purchase.service.query;

import io.aygh.purchase.dto.response.PurchaseDetailResponse;
import io.aygh.purchase.dto.response.PurchaseReportSummary;
import io.aygh.purchase.dto.response.PurchaseSummaryResponse;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface PurchaseQueryService {

    PurchaseDetailResponse findById(Long id);

    PurchaseReportSummary purchaseReport(DateRange dateRange);

    PagedResponse<PurchaseSummaryResponse> findAll(String search, Long vendorId,
                                                   DateRange dateRange, Pageable pageable);
}
