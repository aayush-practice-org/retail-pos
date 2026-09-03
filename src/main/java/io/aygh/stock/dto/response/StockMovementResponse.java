package io.aygh.stock.dto.response;

import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One line of a product's stock ledger. {@code quantity} is in the base unit;
 * {@code enteredQuantity} and {@code enteredUnitSymbol} are what was actually
 * typed, kept so a movement reads the way the person who made it remembers it.
 */
public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        StockMovementType movementType,
        int direction,
        BigDecimal quantity,
        BigDecimal balanceAfter,
        BigDecimal enteredQuantity,
        String enteredUnitSymbol,
        Long referenceId,
        StockReferenceType referenceType,
        String remark,
        Instant createdAt
) {
}
