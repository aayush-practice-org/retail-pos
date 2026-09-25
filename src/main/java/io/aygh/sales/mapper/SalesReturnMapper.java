package io.aygh.sales.mapper;

import io.aygh.sales.dto.response.SalesReturnItemResponse;
import io.aygh.sales.dto.response.SalesReturnResponse;
import io.aygh.sales.entity.SalesReturn;
import io.aygh.sales.entity.SalesReturnItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SalesReturnMapper {

    /** Listings never open the lines. */
    public SalesReturnResponse toSummary(SalesReturn salesReturn) {
        return toResponse(salesReturn, List.of());
    }

    public SalesReturnResponse toDetail(SalesReturn salesReturn) {
        return toResponse(salesReturn, salesReturn.getItems().stream().map(this::toItem).toList());
    }

    private SalesReturnResponse toResponse(SalesReturn r, List<SalesReturnItemResponse> items) {
        return new SalesReturnResponse(
                r.getId(),
                r.getCreditNoteNumber(),
                r.getSale().getId(),
                r.getSale().getInvoiceNumber(),
                r.getReturnedAt(),
                r.getReason(),
                r.getTaxScheme(),
                r.getCustomerName(),
                r.getCustomerPan(),
                r.getNepaliDate(),
                r.getFiscalYear(),
                r.getSubTotal(),
                r.getDiscountAmount(),
                r.getTaxableAmount(),
                r.getVatAmount(),
                r.getNetTotal(),
                r.getRefundAmount(),
                r.getSyncWithIrd(),
                r.getRemark(),
                items);
    }

    private SalesReturnItemResponse toItem(SalesReturnItem item) {
        return new SalesReturnItemResponse(
                item.getId(),
                item.getSaleItem().getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getUnitSymbol(),
                item.getQuantity(),
                item.getQuantityInBaseUnits(),
                item.getRate(),
                item.getLineTotal());
    }
}
