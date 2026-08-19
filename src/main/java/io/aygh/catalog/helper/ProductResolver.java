package io.aygh.catalog.helper;

import io.aygh.catalog.entity.Category;
import io.aygh.catalog.entity.Product;
import io.aygh.catalog.repository.ProductRepository;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.unit.entity.SystemUnit;
import io.aygh.unit.resolver.UnitResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductResolver {

    private final ProductRepository productRepository;
    private final CategoryResolver categoryResolver;
    private final UnitResolver unitResolver;

    public Product resolve(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Product id cannot be null");
        }

        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public Product resolveWithCategoryAndUnit(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Product id cannot be null");
        }

        return productRepository.findByIdWithCategoryAndUnit(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public Category resolveCategory(Long id) {
        return categoryResolver.resolve(id);
    }

    public SystemUnit resolveBaseUnit(UUID id) {
        return unitResolver.resolveSystemUnit(id);
    }
}
