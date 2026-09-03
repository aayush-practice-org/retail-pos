package io.aygh.stock.service.command;

import io.aygh.stock.dto.request.ReorderLevelRequest;
import io.aygh.stock.dto.request.StockAdjustmentRequest;
import io.aygh.stock.dto.response.ProductStockResponse;
import io.aygh.stock.dto.response.StockMovementResponse;

public interface StockCommandService {

    /** A hand-entered correction or write-off. */
    StockMovementResponse adjust(StockAdjustmentRequest request);

    ProductStockResponse setReorderLevel(Long productId, ReorderLevelRequest request);
}
