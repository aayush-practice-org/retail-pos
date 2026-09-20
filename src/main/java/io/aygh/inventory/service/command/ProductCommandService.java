package io.aygh.inventory.service.command;

import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductUpdateRequest;
import io.aygh.inventory.dto.request.ProductVatRequest;
import io.aygh.inventory.dto.request.QuickAddRequest;
import io.aygh.inventory.dto.response.ProductDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitDetailResponse;
import io.aygh.inventory.dto.response.ProductSellingUnitResponse;
import io.aygh.inventory.dto.response.ProductSummaryResponse;


public interface ProductCommandService {

    /**
     * Creates the product and its trading configuration together, and
     * answers with the detail view: the caller has just created purchase and
     * selling units it has no ids for, and a summary would force a second
     * round trip to learn what it just made. {@code ProductDetailResponse}
     * extends the summary, so an existing caller reading the old fields is
     * unaffected.
     */
    ProductDetailResponse create(ProductCreateRequest request);

    /**
     * Creates a product from the little that is known at the till and answers
     * with its sellable unit, in the shape a barcode lookup would have
     * returned. Everything else is defaulted.
     */
    ProductSellingUnitResponse quickAdd(QuickAddRequest request);

    ProductSummaryResponse update(Long id, ProductUpdateRequest request);

    // ── How the product is bought ─────────────────────────────────────────

    ProductPurchaseUnitDetailResponse addPurchaseUnit(Long productId, ProductPurchaseUnitCreateRequest request);

    ProductPurchaseUnitDetailResponse updatePurchaseUnit(
            Long productId, Long purchaseUnitId, ProductPurchaseUnitUpdateRequest request);

    void removePurchaseUnit(Long productId, Long purchaseUnitId);

    /**
     * Opens a new VAT rate, closing the one currently in force the day before.
     * Rates are never edited in place — see {@code ProductPurchaseVat}.
     */
    ProductPurchaseUnitDetailResponse addVatRate(Long productId, Long purchaseUnitId, ProductVatRequest request);

    // ── How the product is sold ───────────────────────────────────────────

    ProductSellingUnitResponse addSellingUnit(Long productId, ProductSellingUnitCreateRequest request);

    ProductSellingUnitResponse updateSellingUnit(
            Long productId, Long sellingUnitId, ProductSellingUnitUpdateRequest request);

    void removeSellingUnit(Long productId, Long sellingUnitId);

    /**
     * Returns the product as the detail view renders it, after a configuration change.
     */
    ProductDetailResponse reload(Long productId);
}
