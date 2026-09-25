package io.aygh.purchase.service.query;

import io.aygh.purchase.dto.response.PurchaseReturnResponse;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PurchaseReturnQueryService {

    PurchaseReturnResponse findById(Long id);

    /** Every debit note raised against one vendor bill, oldest first. */
    List<PurchaseReturnResponse> findByPurchase(Long purchaseId);

    PagedResponse<PurchaseReturnResponse> findAll(String search, Long vendorId, DateRange dateRange, Pageable pageable);
}
