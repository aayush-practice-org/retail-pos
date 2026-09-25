package io.aygh.purchase.service.command.impl;

import io.aygh.exception.BusinessException;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.purchase.dto.request.PurchaseReturnItemRequest;
import io.aygh.purchase.dto.request.PurchaseReturnRequest;
import io.aygh.purchase.dto.response.PurchaseReturnResponse;
import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.entity.PurchaseItem;
import io.aygh.purchase.entity.PurchaseReturn;
import io.aygh.purchase.entity.PurchaseReturnItem;
import io.aygh.purchase.helper.DebitNoteNumberGenerator;
import io.aygh.purchase.helper.PurchaseResolver;
import io.aygh.purchase.helper.PurchaseReturnCalculator;
import io.aygh.purchase.mapper.PurchaseReturnMapper;
import io.aygh.purchase.repository.PurchaseRepository;
import io.aygh.purchase.repository.PurchaseReturnRepository;
import io.aygh.purchase.service.command.PurchaseReturnCommandService;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.service.command.StockLedgerService;
import io.aygh.vendor.dto.request.VendorLedgerEntryRequest;
import io.aygh.vendor.entity.BalanceType;
import io.aygh.vendor.service.command.VendorBalanceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseReturnCommandServiceImpl implements PurchaseReturnCommandService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseResolver purchaseResolver;
    private final PurchaseReturnCalculator calculator;
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final DebitNoteNumberGenerator debitNoteNumbers;
    private final StockLedgerService stockLedger;
    private final VendorBalanceCommandService vendorBalanceCommandService;

    @Override
    public PurchaseReturnResponse create(PurchaseReturnRequest request) {
        // Locked before anything is read off it, so what is left to return
        // cannot change underneath this return.
        purchaseRepository.findForUpdate(request.purchaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", request.purchaseId()));
        Purchase purchase = purchaseResolver.purchase(request.purchaseId());

        List<PurchaseReturn> previous = purchaseReturnRepository.findByPurchaseIdOrderByCreatedAtAsc(purchase.getId());
        Map<Long, BigDecimal> returnedQuantity = new HashMap<>();
        Map<Long, BigDecimal> returnedLineTotal = new HashMap<>();
        for (PurchaseReturn earlier : previous) {
            for (PurchaseReturnItem item : earlier.getItems()) {
                returnedQuantity.merge(item.getPurchaseItem().getId(), item.getQuantity(), BigDecimal::add);
                returnedLineTotal.merge(item.getPurchaseItem().getId(), item.getLineTotal(), BigDecimal::add);
            }
        }

        // The same bill line named twice is one return of the combined quantity
        Map<Long, BigDecimal> requested = new LinkedHashMap<>();
        for (PurchaseReturnItemRequest line : request.items()) {
            requested.merge(line.purchaseItemId(), line.quantity(), BigDecimal::add);
        }

        Map<Long, PurchaseItem> purchaseItems = new HashMap<>();
        purchase.getItems().forEach(item -> purchaseItems.put(item.getId(), item));

        PurchaseReturn purchaseReturn = PurchaseReturn.builder()
                .debitNoteNumber(debitNoteNumbers.next())
                .purchase(purchase)
                .vendor(purchase.getVendor())
                .returnDate(request.returnDate() != null
                        ? request.returnDate()
                        : LocalDate.now(ZoneId.of("Asia/Kathmandu")))
                .reason(request.reason().trim())
                .taxScheme(purchase.getTaxScheme())
                .remark(request.remark())
                .build();

        if (purchaseReturn.getReturnDate().isBefore(purchase.getPurchaseDate())) {
            throw new BusinessException("Goods cannot go back before bill " + purchase.getBillNumber()
                    + " was received on " + purchase.getPurchaseDate());
        }

        for (Map.Entry<Long, BigDecimal> line : requested.entrySet()) {
            PurchaseItem purchaseItem = purchaseItems.get(line.getKey());
            if (purchaseItem == null) {
                throw new BusinessException("Line " + line.getKey() + " is not on bill " + purchase.getBillNumber());
            }
            purchaseReturn.addItem(buildItem(purchaseItem, line.getValue(),
                    returnedQuantity.getOrDefault(purchaseItem.getId(), BigDecimal.ZERO),
                    returnedLineTotal.getOrDefault(purchaseItem.getId(), BigDecimal.ZERO)));
        }

        boolean completesPurchase = purchase.getItems().stream().allMatch(item ->
                returnedQuantity.getOrDefault(item.getId(), BigDecimal.ZERO)
                        .add(requested.getOrDefault(item.getId(), BigDecimal.ZERO))
                        .compareTo(item.getQuantity()) >= 0);

        calculator.applyTotals(purchaseReturn, purchase, previous, completesPurchase);
        purchase.setReturnedAmount(purchase.getReturnedAmount().add(purchaseReturn.getNetTotal()));

        PurchaseReturn saved = purchaseReturnRepository.save(purchaseReturn);

        // Off the shelf, at the pack quantity the goods came in at. The ledger
        // refuses a line that is no longer in stock, which fails the whole return.
        for (PurchaseReturnItem item : saved.getItems()) {
            stockLedger.post(
                    item.getProduct(),
                    StockMovementType.PURCHASE_RETURN_OUT,
                    item.getQuantityInBaseUnits(),
                    item.getQuantity(),
                    item.getPurchaseItem().getPurchaseUnit().getUnit(),
                    saved.getId(),
                    StockReferenceType.PURCHASE_RETURN,
                    "Debit note " + saved.getDebitNoteNumber() + " against bill " + purchase.getBillNumber());
        }

        // The vendor owes the mart for goods it took back: on a credit bill that
        // comes off what the mart owes, on a paid one it is money owed back.
        if (saved.getNetTotal().signum() > 0) {
            vendorBalanceCommandService.post(purchase.getVendor().getId(),
                    new VendorLedgerEntryRequest(saved.getNetTotal(), BalanceType.RECEIVABLE));
        }

        log.info("Debit note {} against bill {} from '{}' — {} line(s), net {}",
                saved.getDebitNoteNumber(), purchase.getBillNumber(), purchase.getVendor().getName(),
                saved.getItems().size(), saved.getNetTotal());

        return purchaseReturnMapper.toDetail(saved);
    }

    /**
     * A bill line's share of what it cost. Sending back the last of a line debits
     * whatever of its total is left, so the returns on a line add up to it exactly.
     */
    private PurchaseReturnItem buildItem(PurchaseItem purchaseItem, BigDecimal quantity,
                                         BigDecimal alreadyReturned, BigDecimal alreadyDebited) {
        BigDecimal left = purchaseItem.getQuantity().subtract(alreadyReturned);
        if (quantity.compareTo(left) > 0) {
            throw new BusinessException(String.format(
                    "Only %s %s of '%s' is left to return on this bill",
                    left.stripTrailingZeros().toPlainString(),
                    purchaseItem.getPurchaseUnit().getUnit().getSymbol(),
                    purchaseItem.getProduct().getName()));
        }

        BigDecimal lineTotal = quantity.compareTo(left) == 0
                ? purchaseItem.getLineTotal().subtract(alreadyDebited).max(BigDecimal.ZERO)
                : purchaseItem.getLineTotal().multiply(quantity)
                .divide(purchaseItem.getQuantity(), MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP);

        return PurchaseReturnItem.builder()
                .purchaseItem(purchaseItem)
                .product(purchaseItem.getProduct())
                .quantity(quantity)
                .quantityInBaseUnits(quantity.multiply(purchaseItem.getPackQuantity()))
                .rate(purchaseItem.getRate())
                .lineTotal(lineTotal)
                .build();
    }
}
