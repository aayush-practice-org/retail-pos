package io.aygh.stock.controller;

import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.aygh.stock.dto.request.ReorderLevelRequest;
import io.aygh.stock.dto.request.StockAdjustmentRequest;
import io.aygh.stock.dto.response.ProductStockResponse;
import io.aygh.stock.dto.response.StockMovementResponse;
import io.aygh.stock.dto.response.StockOverviewResponse;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.service.command.StockCommandService;
import io.aygh.stock.service.query.StockQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Stock levels and the ledger behind them.
 * <p>
 * Backs the Inventory group of the sidebar: {@code STOCK_LEVELS} on the listing,
 * {@code STOCK_ADJUSTMENTS} on the adjustment endpoint, and
 * {@code STOCK_WRITE_OFFS} — which is narrower, so writing off is guarded
 * without the store keeper.
 */
@Tag(name = "Stock", description = "What is on hand, and every movement that got it there.")
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'STORE_KEEPER')")
public class StockController {

    private final StockCommandService stockCommandService;
    private final StockQueryService stockQueryService;

    @Operation(summary = "Stock levels — set lowOnly to see only what needs attention")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductStockResponse>>> levels(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean lowOnly,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                stockQueryService.findLevels(search, categoryId, lowOnly, pageable.toPageable())));
    }

    @Operation(summary = "Headline counts for the stock dashboard")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StockOverviewResponse>> overview() {
        return ResponseEntity.ok(ApiResponse.ok(stockQueryService.overview()));
    }

    @Operation(summary = "One product's stock level")
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductStockResponse>> byProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(stockQueryService.findByProductId(productId)));
    }

    @Operation(summary = "The movement ledger, newest first")
    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<PagedResponse<StockMovementResponse>>> movements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) StockReferenceType referenceType,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                stockQueryService.findMovements(productId, referenceType, pageable.toPageable())));
    }

    @Operation(summary = "Correct a stock figure after a count")
    @PostMapping("/adjustments")
    public ResponseEntity<ApiResponse<StockMovementResponse>> adjust(
            @Valid @RequestBody StockAdjustmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(stockCommandService.adjust(request)));
    }

    /**
     * Separate from the adjustment endpoint only so it can be guarded more
     * narrowly — writing stock off is a loss, and the store keeper who counts the
     * shelf is not who decides to take one.
     */
    @Operation(summary = "Write stock off as expired, damaged or lost")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    @PostMapping("/write-offs")
    public ResponseEntity<ApiResponse<StockMovementResponse>> writeOff(
            @Valid @RequestBody StockAdjustmentRequest request) {

        StockAdjustmentRequest forced = new StockAdjustmentRequest(
                request.productId(), request.quantity(), request.unitId(),
                StockMovementType.WRITE_OFF, request.remark());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(stockCommandService.adjust(forced)));
    }

    @Operation(summary = "Set the level at which a product reads as low")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    @PutMapping("/products/{productId}/reorder-level")
    public ResponseEntity<ApiResponse<ProductStockResponse>> setReorderLevel(
            @PathVariable Long productId, @Valid @RequestBody ReorderLevelRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Reorder level updated",
                stockCommandService.setReorderLevel(productId, request)));
    }
}
