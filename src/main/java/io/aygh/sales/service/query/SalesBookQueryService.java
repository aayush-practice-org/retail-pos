package io.aygh.sales.service.query;

import io.aygh.sales.dto.response.SalesBookResponse;

import java.time.Instant;

public interface SalesBookQueryService {

    /**
     * Builds the sales book for every sale in the range.
     *
     * @param month BS month the period covers, for the book's header
     * @param year  BS year, likewise
     */
    SalesBookResponse getSalesBook(Instant start, Instant end, String month, String year);

    /** The same book rendered on the IRD form, laid out landscape so all ten columns fit. */
    byte[] generateSalesBookPdf(Instant start, Instant end, String month, String year);
}
