package io.aygh.catalog.helper;

import io.aygh.catalog.repository.ProductVariantRepository;
import io.aygh.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProductVariantValidation {

    private final ProductVariantRepository productVariantRepository;

    public void validateUniqueName(Long productId, String name, Long excludeId) {
        if (productVariantRepository.existsByProductIdAndName(productId, name, excludeId)) {
            throw new BusinessException("This product already has a variant named '%s'".formatted(name));
        }
    }

    public void validateUniqueSku(String sku, Long excludeId) {
        if (sku == null || sku.isBlank()) return;

        if (productVariantRepository.existsBySku(sku, excludeId)) {
            throw new BusinessException("SKU already in use: " + sku);
        }
    }

    public void validateUniqueBarcode(String barcode, Long excludeId) {
        if (barcode == null || barcode.isBlank()) return;

        if (productVariantRepository.existsByBarcode(barcode, excludeId)) {
            throw new BusinessException("Barcode already in use: " + barcode);
        }
    }

    public void validatePricing(BigDecimal mrp, BigDecimal sellingPrice) {
        if (mrp == null || sellingPrice == null) return;

        if (sellingPrice.compareTo(mrp) > 0) {
            throw new BusinessException(
                    "Selling price (%s) cannot be higher than the MRP (%s)".formatted(sellingPrice, mrp));
        }
    }
}
