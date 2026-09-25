package io.aygh.purchase.mapper;

import io.aygh.purchase.dto.response.PurchaseReturnItemResponse;
import io.aygh.purchase.dto.response.PurchaseReturnResponse;
import io.aygh.purchase.entity.PurchaseReturn;
import io.aygh.purchase.entity.PurchaseReturnItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseReturnMapper {

    /** Listings never open the lines. */
    public PurchaseReturnResponse toSummary(PurchaseReturn purchaseReturn) {
        return toResponse(purchaseReturn, List.of());
    }

    public PurchaseReturnResponse toDetail(PurchaseReturn purchaseReturn) {
        return toResponse(purchaseReturn, purchaseReturn.getItems().stream().map(this::toItem).toList());
    }

    private PurchaseReturnResponse toResponse(PurchaseReturn r, List<PurchaseReturnItemResponse> items) {
        return new PurchaseReturnResponse(
                r.getId(),
                r.getDebitNoteNumber(),
                r.getPurchase().getId(),
                r.getPurchase().getBillNumber(),
                r.getVendor().getId(),
                r.getVendor().getName(),
                r.getReturnDate(),
                r.getReason(),
                r.getTaxScheme(),
                r.getSubTotal(),
                r.getDiscountAmount(),
                r.getTaxableAmount(),
                r.getVatAmount(),
                r.getNetTotal(),
                r.getRemark(),
                items,
                r.getCreatedAt());
    }

    private PurchaseReturnItemResponse toItem(PurchaseReturnItem item) {
        return new PurchaseReturnItemResponse(
                item.getId(),
                item.getPurchaseItem().getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getPurchaseItem().getPurchaseUnit().getUnit().getSymbol(),
                item.getQuantity(),
                item.getQuantityInBaseUnits(),
                item.getRate(),
                item.getLineTotal());
    }
}
