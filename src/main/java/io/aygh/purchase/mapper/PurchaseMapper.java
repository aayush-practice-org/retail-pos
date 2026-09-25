package io.aygh.purchase.mapper;

import io.aygh.purchase.dto.response.PurchaseDetailResponse;
import io.aygh.purchase.dto.response.PurchaseItemResponse;
import io.aygh.purchase.dto.response.PurchaseSummaryResponse;
import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.entity.PurchaseItem;
import io.aygh.vendor.entity.Vendor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hand-written: both shapes flatten several associations, and the summary must
 * be built without touching {@code items} — that collection is lazy, and reading
 * it per row is what turns a listing into a query storm. The item count comes
 * from a counting query instead.
 */
@Component
public class PurchaseMapper {

    public PurchaseSummaryResponse toSummary(Purchase purchase, int itemCount) {
        Vendor vendor = purchase.getVendor();

        return new PurchaseSummaryResponse(
                purchase.getId(),
                purchase.getBillNumber(),
                purchase.getPurchaseDate(),
                vendor.getId(),
                vendor.getName(),
                purchase.getPaymentMethod(),
                purchase.getTaxScheme(),
                purchase.getSubTotal(),
                purchase.getDiscountAmount(),
                purchase.getTaxableAmount(),
                purchase.getVatAmount(),
                purchase.getNetTotal(),
                itemCount,
                purchase.getCreatedAt());
    }

    public PurchaseDetailResponse toDetail(Purchase purchase) {
        Vendor vendor = purchase.getVendor();
        List<PurchaseItemResponse> items = purchase.getItems().stream()
                .map(this::toItem)
                .toList();

        return new PurchaseDetailResponse(
                purchase.getId(),
                purchase.getBillNumber(),
                purchase.getPurchaseDate(),
                vendor.getId(),
                vendor.getName(),
                vendor.getPanNumber(),
                vendor.getAddress(),
                purchase.getPaymentMethod(),
                purchase.getTaxScheme(),
                purchase.getSubTotal(),
                purchase.getDiscountAmount(),
                purchase.getTaxableAmount(),
                purchase.getVatAmount(),
                purchase.getNetTotal(),
                purchase.getReturnedAmount(),
                purchase.getRemark(),
                items,
                purchase.getCreatedAt());
    }

    public PurchaseItemResponse toItem(PurchaseItem item) {
        return new PurchaseItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getProductCode(),
                item.getPurchaseUnit().getId(),
                item.getPurchaseUnit().getUnit().getName(),
                item.getPurchaseUnit().getUnit().getSymbol(),
                item.getQuantity(),
                item.getPackQuantity(),
                item.getQuantityInBaseUnits(),
                item.getProduct().getBaseUnit().getSymbol(),
                item.getRate(),
                item.getLineTotal());
    }
}
