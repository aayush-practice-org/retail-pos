package io.aygh.catalog.mapper;

import io.aygh.catalog.dto.response.ProductVariantResponse;
import io.aygh.catalog.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductVariantMapper {

    /**
     * The base unit symbol is passed in rather than walked to through
     * {@code variant.getProduct().getBaseUnit()}, so mapping a list of variants
     * never triggers a lazy load per row.
     */
    public ProductVariantResponse toResponse(ProductVariant variant, String baseUnitSymbol) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .name(variant.getName())
                .sku(variant.getSku())
                .barcode(variant.getBarcode())
                .packSize(variant.getPackSize())
                .baseUnitSymbol(baseUnitSymbol)
                .originalQuantity(variant.getOriginalQuantity())
                .originalUnitId(variant.getOriginalUnitId())
                .originalUnitSource(variant.getOriginalUnitSource())
                .mrp(variant.getMrp())
                .sellingPrice(variant.getSellingPrice())
                .isDefault(variant.isDefault())
                .isActive(variant.isActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    public List<ProductVariantResponse> toResponses(List<ProductVariant> variants, String baseUnitSymbol) {
        return variants.stream()
                .map(variant -> toResponse(variant, baseUnitSymbol))
                .toList();
    }
}
