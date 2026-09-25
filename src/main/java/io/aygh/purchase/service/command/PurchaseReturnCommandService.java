package io.aygh.purchase.service.command;

import io.aygh.purchase.dto.request.PurchaseReturnRequest;
import io.aygh.purchase.dto.response.PurchaseReturnResponse;

public interface PurchaseReturnCommandService {

    /**
     * Sends goods back against a vendor bill: raises the debit note, takes the
     * stock off the shelf and posts what the vendor now owes back. All in one
     * transaction — a line that is no longer on the shelf fails the whole return.
     */
    PurchaseReturnResponse create(PurchaseReturnRequest request);
}
