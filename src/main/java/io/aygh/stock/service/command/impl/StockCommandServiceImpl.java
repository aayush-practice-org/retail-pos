package io.aygh.stock.service.command.impl;

import io.aygh.exception.BusinessException;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.repository.UnitRepository;
import io.aygh.stock.dto.request.ReorderLevelRequest;
import io.aygh.stock.dto.request.StockAdjustmentRequest;
import io.aygh.stock.dto.response.ProductStockResponse;
import io.aygh.stock.dto.response.StockMovementResponse;
import io.aygh.stock.entity.ProductStock;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.helper.StockConversion;
import io.aygh.stock.helper.StockResolver;
import io.aygh.stock.mapper.StockMapper;
import io.aygh.stock.repository.ProductStockRepository;
import io.aygh.stock.service.command.StockCommandService;
import io.aygh.stock.service.command.StockLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockCommandServiceImpl implements StockCommandService {

    /**
     * A movement raised by hand is a correction, not a trade. Purchases and sales
     * post their own types through {@link StockLedgerService}, and letting one be
     * typed in here would put stock on the shelf with no purchase behind it.
     */
    private static final Set<StockMovementType> ADJUSTABLE = Set.of(
            StockMovementType.ADJUSTMENT_IN,
            StockMovementType.ADJUSTMENT_OUT,
            StockMovementType.WRITE_OFF);

    private final ProductStockRepository productStockRepository;
    private final UnitRepository unitRepository;
    private final StockResolver resolver;
    private final StockConversion conversion;
    private final StockLedgerService ledger;
    private final StockMapper stockMapper;

    @Override
    public StockMovementResponse adjust(StockAdjustmentRequest request) {
        if (!ADJUSTABLE.contains(request.movementType())) {
            throw new BusinessException(request.movementType()
                    + " is raised by the document that causes it, not as an adjustment");
        }

        Product product = resolver.product(request.productId());
        Unit enteredUnit = enteredUnit(request.unitId());
        BigDecimal inBaseUnits = conversion.toBaseUnits(product, request.quantity(), enteredUnit);

        return stockMapper.toResponse(ledger.post(
                product,
                request.movementType(),
                inBaseUnits,
                request.quantity(),
                enteredUnit,
                null,
                StockReferenceType.ADJUSTMENT,
                request.remark()));
    }

    @Override
    public ProductStockResponse setReorderLevel(Long productId, ReorderLevelRequest request) {
        Product product = resolver.product(productId);

        ProductStock stock = productStockRepository.findByProductId(productId)
                .orElseGet(() -> ProductStock.builder().product(product).build());
        stock.setReorderLevel(request.reorderLevel());

        ProductStock saved = productStockRepository.save(stock);
        log.info("Reorder level for '{}' set to {}", product.getName(), request.reorderLevel());
        return stockMapper.toResponse(saved);
    }

    /** Null means the quantity was entered in the product's own base unit. */
    private Unit enteredUnit(Long unitId) {
        if (unitId == null) {
            return null;
        }
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", unitId));
    }
}
