package io.aygh.catalog.helper;

import io.aygh.catalog.repository.ProductRepository;
import io.aygh.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductValidation {

    private final ProductRepository productRepository;

    public void validateUniqueName(String name, Long excludeId) {
        if (productRepository.existsByName(name, excludeId)) {
            throw new BusinessException("Product name already exists: " + name);
        }
    }
}
