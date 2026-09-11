
package io.aygh.purchase.service.command.impl;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductPurchaseUnit;
import io.aygh.purchase.dto.request.PurchaseItemRequest;
import io.aygh.purchase.dto.request.PurchaseRequest;
import io.aygh.purchase.dto.response.PurchaseDetailResponse;
import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.entity.PurchaseItem;
import io.aygh.purchase.helper.PurchaseCalculator;
import io.aygh.purchase.helper.PurchaseResolver;
import io.aygh.purchase.mapper.PurchaseMapper;
import io.aygh.purchase.repository.PurchaseRepository;
import io.aygh.purchase.service.command.PurchaseCommandService;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.service.command.StockLedgerService;
import io.aygh.vendor.dto.request.VendorLedgerEntryRequest;
import io.aygh.vendor.entity.BalanceType;
import io.aygh.vendor.entity.Vendor;
import io.aygh.vendor.helper.VendorResolver;
import io.aygh.vendor.service.command.VendorBalanceCommandService;
import io.aygh.vendor.service.command.VendorHistoryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Records a purchase and its related effects:
 * <ul>
 *     <li>Purchase document</li>
 *     <li>Stock movement</li>
 *     <li>Vendor history</li>
 *     <li>Vendor payable balance</li>
 * </ul>
 * <p>
 * All operations happen inside one transaction so the purchase and its
 * side-effects either succeed together or roll back together.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseCommandServiceImpl implements PurchaseCommandService {

    private final PurchaseRepository purchaseRepository;

    private final PurchaseResolver purchaseResolver;
    private final PurchaseCalculator purchaseCalculator;
    private final PurchaseMapper purchaseMapper;

    private final VendorResolver vendorResolver;
    private final VendorBalanceCommandService vendorBalanceCommandService;
    private final VendorHistoryCommandService vendorHistoryCommandService;

    private final StockLedgerService stockLedger;


    // -------------------------------------------------------------------------
    // Create Purchase
    // -------------------------------------------------------------------------

    @Override
    public PurchaseDetailResponse create(PurchaseRequest request) {

        Vendor vendor = resolveVendor(request);
        validateBillNumber(request, vendor);

        Purchase purchase = buildPurchase(request, vendor);

        addItems(purchase, request.items());
        calculateTotals(purchase);
        validatePurchase(purchase);

        Purchase savedPurchase = savePurchase(purchase);

        postStockMovements(savedPurchase);
        recordVendorHistory(savedPurchase);
        updateVendorBalance(savedPurchase);

        logPurchase(savedPurchase, vendor);

        return purchaseMapper.toDetail(savedPurchase);
    }


    // -------------------------------------------------------------------------
    // Vendor
    // -------------------------------------------------------------------------

    private Vendor resolveVendor(PurchaseRequest request) {
        return vendorResolver.vendor(request.vendorId());
    }

    private void validateBillNumber(PurchaseRequest request, Vendor vendor) {

        if (purchaseRepository.existsByVendorIdAndBillNumberIgnoreCase(
                vendor.getId(),
                request.billNumber())) {

            throw new BusinessException(
                    "Bill '" + request.billNumber()
                            + "' is already recorded against this vendor"
            );
        }
    }


    // -------------------------------------------------------------------------
    // Purchase
    // -------------------------------------------------------------------------

    private Purchase buildPurchase(
            PurchaseRequest request,
            Vendor vendor) {

        return Purchase.builder()
                .vendor(vendor)
                .billNumber(request.billNumber())
                .purchaseDate(resolvePurchaseDate(request))
                .paymentMethod(request.paymentMethod())
                .taxScheme(request.taxScheme())
                .discountAmount(resolveDiscount(request))
                .remark(request.remark())
                .build();
    }

    private LocalDate resolvePurchaseDate(PurchaseRequest request) {
        return request.purchaseDate() != null
                ? request.purchaseDate()
                : LocalDate.now();
    }

    private BigDecimal resolveDiscount(PurchaseRequest request) {
        return request.discountAmount() != null
                ? request.discountAmount()
                : BigDecimal.ZERO;
    }

    private void addItems(
            Purchase purchase,
            List<PurchaseItemRequest> itemRequests) {

        for (PurchaseItemRequest request : itemRequests) {
            PurchaseItem item = buildItem(request);
            purchase.addItem(item);
        }
    }

    private void calculateTotals(Purchase purchase) {

        purchaseCalculator.applyTotals(
                purchase,
                purchase.getItems()
        );
    }

    private void validatePurchase(Purchase purchase) {

        if (purchase.getDiscountAmount()
                .compareTo(purchase.getSubTotal()) > 0) {

            throw new BusinessException(
                    "The discount is more than the goods came to"
            );
        }
    }

    private Purchase savePurchase(Purchase purchase) {
        return purchaseRepository.save(purchase);
    }


    // -------------------------------------------------------------------------
    // Purchase Item
    // -------------------------------------------------------------------------

    private PurchaseItem buildItem(PurchaseItemRequest request) {

        Product product = purchaseResolver.product(
                request.productId()
        );

        ProductPurchaseUnit purchaseUnit =
                purchaseResolver.purchaseUnit(
                        product.getId(),
                        request.purchaseUnitId()
                );

        BigDecimal rate = resolveRate(request, purchaseUnit, product);
        BigDecimal packQuantity = resolvePackQuantity(
                purchaseUnit,
                product
        );

        BigDecimal quantity = request.quantity();

        return PurchaseItem.builder()
                .product(product)
                .purchaseUnit(purchaseUnit)
                .quantity(quantity)
                .packQuantity(packQuantity)
                .quantityInBaseUnits(
                        quantity.multiply(packQuantity)
                )
                .rate(rate)
                .lineTotal(
                        quantity
                                .multiply(rate)
                                .setScale(2, RoundingMode.HALF_UP)
                )
                .build();
    }

    private BigDecimal resolveRate(
            PurchaseItemRequest request,
            ProductPurchaseUnit purchaseUnit,
            Product product) {

        BigDecimal rate = request.rate() != null
                ? request.rate()
                : purchaseUnit.getPurchasePrice();

        if (rate == null) {
            throw new BusinessException(
                    "No rate given for '" + product.getName()
                            + "', and its purchase unit has no price set"
            );
        }

        return rate;
    }

    private BigDecimal resolvePackQuantity(
            ProductPurchaseUnit purchaseUnit,
            Product product) {

        BigDecimal packQuantity = purchaseUnit.getPackQuantity();

        if (packQuantity == null || packQuantity.signum() <= 0) {
            throw new BusinessException(
                    "'" + purchaseUnit.getUnit().getName()
                            + "' on '" + product.getName()
                            + "' has no pack quantity, so the stock it adds "
                            + "cannot be worked out"
            );
        }

        return packQuantity;
    }


    // -------------------------------------------------------------------------
    // Stock
    // -------------------------------------------------------------------------

    private void postStockMovements(Purchase purchase) {

        for (PurchaseItem item : purchase.getItems()) {

            stockLedger.post(
                    item.getProduct(),
                    StockMovementType.PURCHASE_IN,
                    item.getQuantityInBaseUnits(),
                    item.getQuantity(),
                    item.getPurchaseUnit().getUnit(),
                    purchase.getId(),
                    StockReferenceType.PURCHASE,
                    "Purchase bill " + purchase.getBillNumber()
            );
        }
    }


    // -------------------------------------------------------------------------
    // Vendor
    // -------------------------------------------------------------------------

    private void recordVendorHistory(Purchase purchase) {
        vendorHistoryCommandService.record(
                purchase.getVendor().getId(),
                purchase.getId()
        );
    }

    private void updateVendorBalance(Purchase purchase) {

        if (!purchase.isOnCredit()) {
            return;
        }

        vendorBalanceCommandService.post(
                purchase.getVendor().getId(),
                new VendorLedgerEntryRequest(
                        purchase.getNetTotal(),
                        BalanceType.PAYABLE
                )
        );
    }


    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    private void logPurchase(
            Purchase purchase,
            Vendor vendor) {

        log.info(
                "Recorded purchase {} from '{}' — {} line(s), net {}",
                purchase.getBillNumber(),
                vendor.getName(),
                purchase.getItems().size(),
                purchase.getNetTotal()
        );
    }
}
