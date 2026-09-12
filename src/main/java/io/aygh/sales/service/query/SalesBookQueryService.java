package io.aygh.sales.service.query;

import io.aygh.sales.dto.response.SalesBookResponse;
import io.aygh.shared.response.DateRange;

public interface SalesBookQueryService {

    /**
     * Builds the sales book for every sale in the date range.
     */
    SalesBookResponse getSalesBook(DateRange dateRange);

    /**
     * The same book rendered on the IRD form, laid out landscape so all ten columns fit.
     */
    byte[] generateSalesBookPdf(DateRange dateRange);
}
