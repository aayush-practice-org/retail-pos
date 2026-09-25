package io.aygh.sales.service.command.impl;

import io.aygh.exception.BusinessException;
import io.aygh.exception.CbmsSyncFailedException;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.identity.entity.CbmsInternalEntity;
import io.aygh.identity.service.CbmsInternalProvider;
import io.aygh.sales.dto.request.SalesReturnItemRequest;
import io.aygh.sales.dto.request.SalesReturnRequest;
import io.aygh.sales.dto.response.SalesReturnResponse;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.entity.SaleItem;
import io.aygh.sales.entity.SalesReturn;
import io.aygh.sales.entity.SalesReturnItem;
import io.aygh.sales.helper.CreditNoteNumberGenerator;
import io.aygh.sales.helper.SaleResolver;
import io.aygh.sales.helper.SalesReturnCalculator;
import io.aygh.sales.mapper.SalesReturnMapper;
import io.aygh.sales.repository.SaleRepository;
import io.aygh.sales.repository.SalesReturnRepository;
import io.aygh.sales.service.command.SalesReturnCommandService;
import io.aygh.shared.cbms.CbmsClient;
import io.aygh.shared.cbms.CbmsSalesReturnRequest;
import io.aygh.shared.service.NepaliDateUtils;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.service.command.StockLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SalesReturnCommandServiceImpl implements SalesReturnCommandService {

    private final SaleRepository saleRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final SaleResolver saleResolver;
    private final SalesReturnCalculator calculator;
    private final SalesReturnMapper salesReturnMapper;
    private final CreditNoteNumberGenerator creditNoteNumbers;
    private final StockLedgerService stockLedger;
    private final CbmsClient cbmsClient;
    private final CbmsInternalProvider cbmsInternalProvider;

    @Override
    public SalesReturnResponse create(SalesReturnRequest request) {
        // Locked before anything is read off it, so what is left to return
        // cannot change underneath this return.
        saleRepository.findForUpdate(request.saleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", request.saleId()));
        Sale sale = saleResolver.sale(request.saleId());

        List<SalesReturn> previous = salesReturnRepository.findBySaleIdOrderByReturnedAtAsc(sale.getId());
        Map<Long, BigDecimal> returnedQuantity = new HashMap<>();
        Map<Long, BigDecimal> returnedLineTotal = new HashMap<>();
        for (SalesReturn earlier : previous) {
            for (SalesReturnItem item : earlier.getItems()) {
                returnedQuantity.merge(item.getSaleItem().getId(), item.getQuantity(), BigDecimal::add);
                returnedLineTotal.merge(item.getSaleItem().getId(), item.getLineTotal(), BigDecimal::add);
            }
        }

        // The same bill line named twice is one return of the combined quantity
        Map<Long, BigDecimal> requested = new LinkedHashMap<>();
        for (SalesReturnItemRequest line : request.items()) {
            requested.merge(line.saleItemId(), line.quantity(), BigDecimal::add);
        }

        Map<Long, SaleItem> saleItems = new HashMap<>();
        sale.getItems().forEach(item -> saleItems.put(item.getId(), item));

        Instant now = Instant.now();
        SalesReturn salesReturn = SalesReturn.builder()
                .creditNoteNumber(creditNoteNumbers.next())
                .sale(sale)
                .returnedAt(now)
                .reason(request.reason().trim())
                .taxScheme(sale.getTaxScheme())
                .customerName(sale.getCustomerName())
                .customerPan(sale.getCustomerPan())
                .nepaliDate(request.nepaliDate() != null
                        ? request.nepaliDate()
                        : NepaliDateUtils.getIrdCompliantBsDate(now))
                .fiscalYear(NepaliDateUtils.getIrdFiscalYear(now))
                .remark(request.remark())
                .build();

        for (Map.Entry<Long, BigDecimal> line : requested.entrySet()) {
            SaleItem saleItem = saleItems.get(line.getKey());
            if (saleItem == null) {
                throw new BusinessException("Line " + line.getKey() + " is not on invoice " + sale.getInvoiceNumber());
            }
            salesReturn.addItem(buildItem(saleItem, line.getValue(),
                    returnedQuantity.getOrDefault(saleItem.getId(), BigDecimal.ZERO),
                    returnedLineTotal.getOrDefault(saleItem.getId(), BigDecimal.ZERO)));
        }

        boolean completesSale = sale.getItems().stream().allMatch(item ->
                returnedQuantity.getOrDefault(item.getId(), BigDecimal.ZERO)
                        .add(requested.getOrDefault(item.getId(), BigDecimal.ZERO))
                        .compareTo(item.getQuantity()) >= 0);

        calculator.applyTotals(salesReturn, sale, previous, completesSale);
        salesReturn.setRefundAmount(sale.applyReturn(salesReturn.getNetTotal()));

        SalesReturn saved = salesReturnRepository.save(salesReturn);

        // Back on the shelf, at the pack quantity the goods went out at
        for (SalesReturnItem item : saved.getItems()) {
            SaleItem saleItem = item.getSaleItem();
            stockLedger.post(
                    item.getProduct(),
                    StockMovementType.SALE_RETURN_IN,
                    item.getQuantityInBaseUnits(),
                    item.getQuantity(),
                    saleItem.getSellingUnit().getUnit(),
                    saved.getId(),
                    StockReferenceType.SALE_RETURN,
                    "Credit note " + saved.getCreditNoteNumber() + " against " + sale.getInvoiceNumber());
        }

        fileWithIrd(saved, sale);

        log.info("Credit note {} against {} — {} line(s), net {}, refund {}",
                saved.getCreditNoteNumber(), sale.getInvoiceNumber(), saved.getItems().size(),
                saved.getNetTotal(), saved.getRefundAmount());

        return salesReturnMapper.toDetail(saved);
    }

    /**
     * A bill line's share of what was charged for it. Handing back the last of a
     * line credits whatever of its total is left, so the returns on a line add up
     * to the line exactly however the paisa fell on the earlier ones.
     */
    private SalesReturnItem buildItem(SaleItem saleItem, BigDecimal quantity,
                                      BigDecimal alreadyReturned, BigDecimal alreadyCredited) {
        BigDecimal left = saleItem.getQuantity().subtract(alreadyReturned);
        if (quantity.compareTo(left) > 0) {
            throw new BusinessException(String.format(
                    "Only %s %s of '%s' is left to return on this bill",
                    left.stripTrailingZeros().toPlainString(), saleItem.getUnitSymbol(), saleItem.getProductName()));
        }

        BigDecimal lineTotal = quantity.compareTo(left) == 0
                ? saleItem.getLineTotal().subtract(alreadyCredited).max(BigDecimal.ZERO)
                : saleItem.getLineTotal().multiply(quantity)
                .divide(saleItem.getQuantity(), MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP);

        return SalesReturnItem.builder()
                .saleItem(saleItem)
                .product(saleItem.getProduct())
                .productName(saleItem.getProductName())
                .unitSymbol(saleItem.getUnitSymbol())
                .quantity(quantity)
                .quantityInBaseUnits(quantity.multiply(saleItem.getPackQuantity()))
                .rate(saleItem.getRate())
                .lineTotal(lineTotal)
                .build();
    }

    /**
     * Last, once the credit note is final: a rejection rolls the whole return
     * back, stock included. A bill raised before CBMS syncing existed was never
     * filed, so the IRD has nothing to credit it against — its return is kept
     * on the books only.
     */
    private void fileWithIrd(SalesReturn salesReturn, Sale sale) {
        if (!Boolean.TRUE.equals(sale.getSyncWithIrd())) {
            log.warn("Invoice {} was never filed with the IRD; credit note {} is not sent to CBMS",
                    sale.getInvoiceNumber(), salesReturn.getCreditNoteNumber());
            return;
        }

        if (!cbmsClient.sendCbmsSalesReturnRequest(buildCbmsRequest(salesReturn, sale))) {
            throw new CbmsSyncFailedException("CBMS rejected credit note " + salesReturn.getCreditNoteNumber()
                    + ". Please try again.");
        }
        salesReturn.setSyncWithIrd(true);
    }

    private CbmsSalesReturnRequest buildCbmsRequest(SalesReturn salesReturn, Sale sale) {
        CbmsInternalEntity cbms = cbmsInternalProvider.getOrCreate();
        return CbmsSalesReturnRequest.builder()
                .username(cbms.getCbmsUsername())
                .password(cbms.getCbmsPassword())
                .sellerPan(cbms.getPan())
                // The IRD's API wants empty strings, not nulls
                .buyerName(salesReturn.getCustomerName() != null ? salesReturn.getCustomerName() : "")
                .buyerPan(salesReturn.getCustomerPan() != null ? salesReturn.getCustomerPan() : "")
                // The fiscal year of the bill being credited, as the IRD holds it
                .fiscalYear(sale.getFiscalYear())
                .refInvoiceNumber(sale.getInvoiceNumber())
                .creditNoteNumber(salesReturn.getCreditNoteNumber())
                .creditNoteDate(salesReturn.getNepaliDate())
                .reasonForReturn(salesReturn.getReason())
                .totalSales(salesReturn.getNetTotal().doubleValue())
                .taxableSalesVat(salesReturn.getTaxableAmount().doubleValue())
                .vat(salesReturn.getVatAmount().doubleValue())
                // Every nullable tax column is sent as zero, or the API fails to map it
                .excisableAmount(0.0)
                .excise(0.0)
                .taxableSalesHst(0.0)
                .hst(0.0)
                .amountForEsf(0.0)
                .esf(0.0)
                .exportSales(0.0)
                .taxExemptedSales(0.0)
                .isRealTime(true)
                .dateTimeClient(LocalDateTime.now(ZoneId.of("Asia/Kathmandu")))
                .build();
    }
}
