package io.aygh.sales.service.query;

import io.aygh.sales.dto.response.SalesReturnResponse;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SalesReturnQueryService {

    SalesReturnResponse findById(Long id);

    /** Every credit note raised against one bill, oldest first. */
    List<SalesReturnResponse> findBySale(Long saleId);

    PagedResponse<SalesReturnResponse> findAll(String search, DateRange dateRange, Pageable pageable);
}
