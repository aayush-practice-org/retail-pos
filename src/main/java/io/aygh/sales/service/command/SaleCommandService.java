package io.aygh.sales.service.command;

import io.aygh.sales.dto.request.SaleRequest;
import io.aygh.sales.dto.response.SaleDetailResponse;

public interface SaleCommandService {

    /**
     * Rings up a basket: raises the bill, takes the stock off the shelf, and
     * settles whatever was tendered. All in one transaction — a line that cannot
     * be covered fails the whole sale rather than leaving half a basket sold.
     */
    SaleDetailResponse create(SaleRequest request);
}
