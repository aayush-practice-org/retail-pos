package io.aygh.stock.dto.response;

import io.aygh.stock.entity.StockStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What is on hand for one product. The base unit's name and symbol travel with
 * the number because a bare "1250" means nothing on a stock screen.
 */
public record ProductStockResponse(
        Long productId,
        String productName,
        String productCode,
        String categoryName,
        BigDecimal quantity,
        BigDecimal reorderLevel,
        StockStatus status,
        String baseUnitName,
        String baseUnitSymbol,
        Instant updatedAt
) {
}
