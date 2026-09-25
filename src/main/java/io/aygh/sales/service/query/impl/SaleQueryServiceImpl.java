package io.aygh.sales.service.query.impl;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleSummaryResponse;
import io.aygh.sales.dto.response.SalesReportSummary;
import io.aygh.sales.dto.response.SalesTotalsResponse;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.helper.SaleResolver;
import io.aygh.sales.mapper.SaleMapper;
import io.aygh.sales.repository.SaleRepository;
import io.aygh.sales.repository.SalesReturnRepository;
import io.aygh.sales.service.query.SaleQueryService;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleQueryServiceImpl implements SaleQueryService {

    private final SaleRepository saleRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final SaleResolver resolver;
    private final SaleMapper saleMapper;

    @Override
    public SaleDetailResponse findById(Long id) {
        return saleMapper.toDetail(resolver.sale(id));
    }

    @Override
    public SaleDetailResponse findByInvoiceNumber(String invoiceNumber) {
        return saleMapper.toDetail(resolver.byInvoiceNumber(invoiceNumber));
    }

    @Override
    public SalesReportSummary salesReport(DateRange dateRange) {
        DateRange range = dateRange == null ? DateRange.THIS_MONTH : dateRange;
        SalesReportSummary salesReportSummary = saleRepository.salesReport(range.getStart(), range.getEnd());
        salesReportSummary.setPaymentTypeTotals(saleRepository.paymentByType(range.getStart(), range.getEnd()));
        salesReportSummary.applyReturns(salesReturnRepository.totalsBetween(range.getStart(), range.getEnd()));
        return salesReportSummary;
    }

    @Override
    public PagedResponse<SaleSummaryResponse> findAll(String search, PaymentStatus status,
                                                      DateRange dateRange, Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? null : "%" + search.trim().toLowerCase() + "%";

        Instant from = DateRange.getStart(dateRange);
        Instant to = DateRange.getEnd(dateRange);

        Page<Sale> page = saleRepository.search(pattern, status, from, to, pageable);

        List<SaleSummaryResponse> content = page.getContent().stream()
                .map(sale -> saleMapper.toSummary(sale, saleRepository.countItems(sale.getId())))
                .toList();

        return PaginationUtils.toPagedResponse(page, content);
    }

    @Override
    public SalesTotalsResponse totals(DateRange dateRange) {
        Instant from = DateRange.getStart(dateRange);
        Instant to = DateRange.getEnd(dateRange);
        return saleRepository.totalsBetween(from, to);
    }

    @Transactional
    @Override
    public SaleDetailResponse incrementPrintCount(Long id) {
        Sale sale = resolver.sale(id);
        int printCount = (sale.getPrintCount() != null ? sale.getPrintCount() : 0) + 1;
        sale.setPrintCount(printCount);
        sale.setIsBillPrinted(true);
        Sale saved = saleRepository.save(sale);
        return saleMapper.toDetail(saved);
    }
}
