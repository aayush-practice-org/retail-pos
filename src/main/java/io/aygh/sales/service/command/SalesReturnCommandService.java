package io.aygh.sales.service.command;

import io.aygh.sales.dto.request.SalesReturnRequest;
import io.aygh.sales.dto.response.SalesReturnResponse;

public interface SalesReturnCommandService {

    /**
     * Takes goods back against a bill: raises the credit note, puts the stock
     * back on the shelf, credits the bill and files the credit note with CBMS.
     * All in one transaction — a rejection from the IRD unwinds the lot.
     */
    SalesReturnResponse create(SalesReturnRequest request);
}
