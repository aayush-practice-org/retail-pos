package io.aygh.purchase.service.query;

import io.aygh.purchase.dto.response.PurchaseDetailResponse;
import io.aygh.purchase.dto.response.PurchaseSummaryResponse;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PurchaseQueryService {

    PurchaseDetailResponse findById(Long id);

    PagedResponse<PurchaseSummaryResponse> findAll(String search, Long vendorId,
                                                   LocalDate from, LocalDate to, Pageable pageable);
}
