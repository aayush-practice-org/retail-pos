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
import java.util.ArrayList;
import java.util.List;

/**
 * Recording a purchase is four things at once — a bill, stock on the shelf, a
 * vendor's balance and a vendor's trail — and they are only ever right together.
 * One transaction covers all of it: a purchase that fails on its last line
 * leaves no stock behind and no payable raised.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseCommandServiceImpl implements PurchaseCommandService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseResolver resolver;
    private final PurchaseCalculator calculator;
    private final PurchaseMapper purchaseMapper;

    private final VendorResolver vendorResolver;
    private final VendorBalanceCommandService vendorBalanceCommandService;
    private final VendorHistoryCommandService vendorHistoryCommandService;
    private final StockLedgerService stockLedger;

    @Override
    public PurchaseDetailResponse create(PurchaseRequest request) {
        Vendor vendor = vendorResolver.vendor(request.vendorId());
        LocalDate purchaseDate = request.purchaseDate() == null ? LocalDate.now() : request.purchaseDate();

        requireBillNumberUnused(request.billNumber(), vendor.getId());

        Purchase purchase = Purchase.builder()
                .vendor(vendor)
                .billNumber(request.billNumber())
                .purchaseDate(purchaseDate)
                .paymentMethod(request.paymentMethod())
                .taxScheme(request.taxScheme())
                .discountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount())
                .remark(request.remark())
                .build();

        List<PurchaseItem> items = new ArrayList<>();
        for (PurchaseItemRequest line : request.items()) {
            PurchaseItem item = buildItem(line);
            purchase.addItem(item);
            items.add(item);
        }

        calculator.applyTotals(purchase, items);

        if (purchase.getDiscountAmount().compareTo(purchase.getSubTotal()) > 0) {
            throw new BusinessException("The discount is more than the goods came to");
        }

        Purchase saved = purchaseRepository.save(purchase);

        // Stock only after the purchase has an id: every movement points back at
        // the document that caused it, and a null reference would orphan the trail.
        for (PurchaseItem item : saved.getItems()) {
            stockLedger.post(
                    item.getProduct(),
                    StockMovementType.PURCHASE_IN,
                    item.getQuantityInBaseUnits(),
                    item.getQuantity(),
                    item.getPurchaseUnit().getUnit(),
                    saved.getId(),
                    StockReferenceType.PURCHASE,
                    "Purchase bill " + saved.getBillNumber());
        }

        vendorHistoryCommandService.record(vendor.getId(), saved.getId());

        // Anything but credit was settled at the counter and leaves the vendor's
        // account where it was; a payable would have to be settled again to clear.
        if (saved.isOnCredit()) {
            vendorBalanceCommandService.post(vendor.getId(),
                    new VendorLedgerEntryRequest(saved.getNetTotal(), BalanceType.PAYABLE));
        }

        log.info("Recorded purchase {} from '{}' — {} line(s), net {}",
                saved.getBillNumber(), vendor.getName(), saved.getItems().size(), saved.getNetTotal());

        return purchaseMapper.toDetail(saved);
    }

    private PurchaseItem buildItem(PurchaseItemRequest line) {
        Product product = resolver.product(line.productId());
        ProductPurchaseUnit unit = resolver.purchaseUnit(product.getId(), line.purchaseUnitId());

        BigDecimal rate = line.rate() != null ? line.rate() : unit.getPurchasePrice();
        if (rate == null) {
            throw new BusinessException("No rate given for '" + product.getName()
                    + "', and its purchase unit has no price set");
        }

        // Copied, not referenced: editing the catalogue's pack quantity later must
        // not restate how much this bill actually put on the shelf.
        BigDecimal packQuantity = unit.getPackQuantity();
        if (packQuantity == null || packQuantity.signum() <= 0) {
            throw new BusinessException("'" + unit.getUnit().getName() + "' on '" + product.getName()
                    + "' has no pack quantity, so the stock it adds cannot be worked out");
        }

        return PurchaseItem.builder()
                .product(product)
                .purchaseUnit(unit)
                .quantity(line.quantity())
                .packQuantity(packQuantity)
                .quantityInBaseUnits(line.quantity().multiply(packQuantity))
                .rate(rate)
                .lineTotal(line.quantity().multiply(rate).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * Re-keying the same bill is the commonest data-entry mistake there is, and
     * it doubles both the stock and what the vendor is owed.
     */
    private void requireBillNumberUnused(String billNumber, Long vendorId) {
        if (purchaseRepository.existsByVendorIdAndBillNumberIgnoreCase(vendorId, billNumber)) {
            throw new BusinessException("Bill '" + billNumber + "' is already recorded against this vendor");
        }
    }
}
