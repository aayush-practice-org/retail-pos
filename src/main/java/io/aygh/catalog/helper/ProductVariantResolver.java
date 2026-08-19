package io.aygh.catalog.helper;

import io.aygh.catalog.entity.ProductVariant;
import io.aygh.catalog.repository.ProductVariantRepository;
import io.aygh.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductVariantResolver {

    private final ProductVariantRepository productVariantRepository;

    /**
     * Variants are always addressed through their owning product, so a variant id
     * belonging to another product reads as "not found" rather than leaking across.
     */
    public ProductVariant resolve(Long productId, Long variantId) {
        if (variantId == null) {
            throw new IllegalArgumentException("Variant id cannot be null");
        }

        return productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Variant %d not found on product %d".formatted(variantId, productId)));
    }
}
