package io.aygh.sales.service.query;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleSummaryResponse;
import io.aygh.sales.dto.response.SalesTotalsResponse;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SaleQueryService {

    SaleDetailResponse findById(Long id);

    SaleDetailResponse findByInvoiceNumber(String invoiceNumber);

    PagedResponse<SaleSummaryResponse> findAll(String search, PaymentStatus status,
                                               Instant from, Instant to, Pageable pageable);

    SalesTotalsResponse totals(Instant from, Instant to);
}
