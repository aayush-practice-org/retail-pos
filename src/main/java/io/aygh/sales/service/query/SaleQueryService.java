package io.aygh.sales.service.query;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleSummaryResponse;
import io.aygh.sales.dto.response.SalesReportSummary;
import io.aygh.sales.dto.response.SalesTotalsResponse;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface SaleQueryService {

    SaleDetailResponse findById(Long id);

    SaleDetailResponse findByInvoiceNumber(String invoiceNumber);

    SalesReportSummary salesReport(DateRange dateRange);

    PagedResponse<SaleSummaryResponse> findAll(String search, PaymentStatus status,
                                               DateRange dateRange, Pageable pageable);

    SalesTotalsResponse totals(DateRange dateRange);

    SaleDetailResponse incrementPrintCount(Long id);
}
