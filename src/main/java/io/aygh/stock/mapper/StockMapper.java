package io.aygh.stock.mapper;

import io.aygh.inventory.entity.Product;
import io.aygh.stock.dto.response.ProductStockResponse;
import io.aygh.stock.dto.response.StockMovementResponse;
import io.aygh.stock.entity.ProductStock;
import io.aygh.stock.entity.StockMovement;
import io.aygh.stock.entity.StockStatus;
import org.springframework.stereotype.Component;

/**
 * Hand-written rather than MapStruct: both responses flatten several levels of
 * association and derive a field the entity does not hold ({@link StockStatus}),
 * which in a generated mapper would be more annotations than code.
 */
@Component
public class StockMapper {

    public ProductStockResponse toResponse(ProductStock stock) {
        Product product = stock.getProduct();

        return new ProductStockResponse(
                product.getId(),
                product.getName(),
                product.getProductCode(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                stock.getQuantity(),
                stock.getReorderLevel(),
                statusOf(stock),
                product.getBaseUnit().getName(),
                product.getBaseUnit().getSymbol(),
                stock.getUpdatedAt());
    }

    public StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getMovementType(),
                movement.getMovementType().direction(),
                movement.getQuantity(),
                movement.getBalanceAfter(),
                movement.getEnteredQuantity(),
                movement.getEnteredUnit() == null ? null : movement.getEnteredUnit().getSymbol(),
                movement.getReferenceId(),
                movement.getReferenceType(),
                movement.getRemark(),
                movement.getCreatedAt());
    }

    public StockStatus statusOf(ProductStock stock) {
        if (stock.isOutOfStock()) {
            return StockStatus.OUT_OF_STOCK;
        }
        return stock.isLow() ? StockStatus.LOW : StockStatus.IN_STOCK;
    }
}
