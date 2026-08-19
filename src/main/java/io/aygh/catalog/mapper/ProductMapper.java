package io.aygh.catalog.mapper;

import io.aygh.catalog.dto.response.ProductDetailResponse;
import io.aygh.catalog.dto.response.ProductResponse;
import io.aygh.catalog.dto.response.ProductVariantResponse;
import io.aygh.catalog.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product, long variantCount) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .baseUnitId(product.getBaseUnit().getId())
                .baseUnitName(product.getBaseUnit().getName())
                .baseUnitSymbol(product.getBaseUnit().getSymbol())
                .imageUrl(product.getImageUrl())
                .isActive(product.isActive())
                .variantCount(variantCount)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductDetailResponse toDetailResponse(Product product, List<ProductVariantResponse> variants) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .baseUnitId(product.getBaseUnit().getId())
                .baseUnitName(product.getBaseUnit().getName())
                .baseUnitSymbol(product.getBaseUnit().getSymbol())
                .imageUrl(product.getImageUrl())
                .isActive(product.isActive())
                .variants(variants)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
