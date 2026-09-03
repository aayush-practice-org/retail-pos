package io.aygh.stock.dto.response;

/** The headline counts a stock dashboard opens with. */
public record StockOverviewResponse(
        long trackedProducts,
        long needingAttention
) {
}
