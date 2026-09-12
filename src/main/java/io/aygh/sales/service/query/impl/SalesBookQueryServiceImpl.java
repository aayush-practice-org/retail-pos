package io.aygh.sales.service.query.impl;

import io.aygh.billing.service.InvoicePdfService;
import io.aygh.billing.service.MartBranding;
import io.aygh.billing.service.MartBrandingService;
import io.aygh.sales.dto.response.SalesBookResponse;
import io.aygh.sales.dto.response.SalesBookRowResponse;
import io.aygh.sales.dto.response.SalesBookTotalResponse;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.repository.SaleRepository;
import io.aygh.sales.service.query.SalesBookQueryService;
import io.aygh.shared.entity.TaxScheme;
import io.aygh.shared.response.DateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesBookQueryServiceImpl implements SalesBookQueryService {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kathmandu");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(REPORT_ZONE);

    private final SaleRepository saleRepository;
    private final MartBrandingService martBrandingService;
    private final InvoicePdfService invoicePdfService;

    @Transactional(readOnly = true)
    @Override
    public SalesBookResponse getSalesBook(DateRange dateRange) {
        DateRange range = dateRange != null ? dateRange : DateRange.THIS_MONTH;
        Instant start = range.getStart();
        Instant end = range.getEnd();

        MartBranding branding = martBrandingService.resolve();
        List<Sale> sales = saleRepository.findSalesBookSales(start, end);

        List<SalesBookRowResponse> rows = sales.stream()
                .map(this::toRow)
                .toList();

        String duration = formatDuration(range);

        return new SalesBookResponse(
                branding.companyName(),
                branding.registrationNumber(),
                duration,
                rows,
                totalOf(rows)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] generateSalesBookPdf(DateRange dateRange) {
        return invoicePdfService.generateSalesBook(getSalesBook(dateRange));
    }

    private String formatDuration(DateRange dateRange) {
        if (dateRange == DateRange.ALL_TIME) {
            return "All Time";
        }
        LocalDate start = dateRange.getStartDate();
        LocalDate end = dateRange.getEndDate();
        if (start.equals(end)) {
            return DATE_FMT.format(start);
        }
        return DATE_FMT.format(start) + " to " + DATE_FMT.format(end);
    }

    private SalesBookRowResponse toRow(Sale sale) {
        BigDecimal tax = zeroIfNull(sale.getVatAmount());
        boolean vatCharged = sale.getTaxScheme() == TaxScheme.VAT && tax.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal taxable = vatCharged ? zeroIfNull(sale.getTaxableAmount()) : BigDecimal.ZERO;
        BigDecimal nonTaxable = vatCharged ? BigDecimal.ZERO : zeroIfNull(sale.getNetTotal());

        return new SalesBookRowResponse(
                billDate(sale),
                sale.getInvoiceNumber(),
                sale.getCustomerName(),
                sale.getCustomerPan(),
                zeroIfNull(sale.getNetTotal()),
                nonTaxable,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                zeroIfNull(sale.getDiscountAmount()),
                taxable,
                tax
        );
    }

    private String billDate(Sale sale) {
        String nepaliDate = sale.getNepaliDate();
        if (nepaliDate != null && !nepaliDate.isBlank()) {
            return nepaliDate;
        }
        Instant at = sale.getSoldAt();
        return at == null ? null : DATE_FMT.format(at);
    }

    private SalesBookTotalResponse totalOf(List<SalesBookRowResponse> rows) {
        return new SalesBookTotalResponse(
                sum(rows, SalesBookRowResponse::totalSales),
                sum(rows, SalesBookRowResponse::nonTaxableSales),
                sum(rows, SalesBookRowResponse::exportSales),
                sum(rows, SalesBookRowResponse::discount),
                sum(rows, SalesBookRowResponse::taxableAmount),
                sum(rows, SalesBookRowResponse::tax)
        );
    }

    private BigDecimal sum(List<SalesBookRowResponse> rows,
                           java.util.function.Function<SalesBookRowResponse, BigDecimal> column) {
        return rows.stream()
                .map(column)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
