package io.aygh.purchase.service.command;

import io.aygh.purchase.dto.request.PurchaseRequest;
import io.aygh.purchase.dto.response.PurchaseDetailResponse;

public interface PurchaseCommandService {

    /**
     * Records goods received from a vendor: the bill, the stock it put on the
     * shelf, and — when it was taken on credit — what the vendor is now owed.
     * All of it in one transaction, or none of it.
     */
    PurchaseDetailResponse create(PurchaseRequest request);
}
