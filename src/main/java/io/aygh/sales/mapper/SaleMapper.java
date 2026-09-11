package io.aygh.sales.mapper;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleItemResponse;
import io.aygh.sales.dto.response.SaleSummaryResponse;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.entity.SaleItem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hand-written: the summary must be built without touching {@code items}, which
 * is lazy — the count comes from a counting query instead — and the line
 * responses read the names copied onto the row rather than following the
 * associations back to the catalogue.
 */
@Component
public class SaleMapper {

    public SaleSummaryResponse toSummary(Sale sale, int itemCount) {
        Long customerId = sale.getCustomer() != null ? sale.getCustomer().getId() : null;
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getInvoiceNumber(),
                sale.getSoldAt(),
                sale.getChannel(),
                sale.getTaxScheme(),
                customerId,
                sale.getCustomerName(),
                sale.getSubTotal(),
                sale.getDiscountAmount(),
                sale.getVatAmount(),
                sale.getNetTotal(),
                sale.getPaidAmount(),
                sale.dueAmount(),
                sale.getPaymentMethod(),
                sale.getPaymentStatus(),
                itemCount);
    }

    public SaleDetailResponse toDetail(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(this::toItem)
                .toList();

        Long customerId = sale.getCustomer() != null ? sale.getCustomer().getId() : null;
        return new SaleDetailResponse(
                sale.getId(),
                sale.getInvoiceNumber(),
                sale.getSoldAt(),
                sale.getChannel(),
                sale.getTaxScheme(),
                customerId,
                sale.getCustomerName(),
                sale.getCustomerPhone(),
                sale.getCustomerPan(),
                sale.getSubTotal(),
                sale.getDiscountAmount(),
                sale.getTaxableAmount(),
                sale.getVatAmount(),
                sale.getNetTotal(),
                sale.getPaymentMethod(),
                sale.getPaymentStatus(),
                sale.getPaidAmount(),
                sale.getChangeAmount(),
                sale.dueAmount(),
                sale.getRemark(),
                items);
    }

    public SaleItemResponse toItem(SaleItem item) {
        return new SaleItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProduct().getProductCode(),
                item.getSellingUnit().getId(),
                item.getUnitSymbol(),
                item.getQuantity(),
                item.getQuantityInBaseUnits(),
                item.getRate(),
                item.getMrp(),
                item.getDiscountAmount(),
                item.getLineTotal());
    }
}
